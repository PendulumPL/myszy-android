package pl.razem.myszy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import org.json.JSONObject
import java.util.UUID
import java.util.regex.Pattern

object AliorParser {
    private val amountPattern = Pattern.compile("(?<!\\d)(\\d{1,3}(?:[ .]\\d{3})*(?:[,.]\\d{2}))\\s*(?:z\u0142|PLN)", Pattern.CASE_INSENSITIVE)
    private val placePattern = Regex("(?i)Miejsce transakcji:\\s*([^|\\n]+)")
    fun parse(title: String, text: String): PendingPayment? {
        val combined=listOf(title,text).filter{it.isNotBlank()}.joinToString(" | ")
        val matcher=amountPattern.matcher(combined); if(!matcher.find()) return null
        val value=matcher.group(1)?.replace(" ","")?.replace(".","")?.replace(",",".")?.toDoubleOrNull()?:return null
        val merchant=placePattern.find(combined)?.groupValues?.getOrNull(1)?.trim()?.take(80)
            ?: Regex("(?i)(?:w |u |dla )([A-Z0-9ŁÓŚŻŹĆĘĄŃ* ._-]{3,60})").find(combined)?.groupValues?.getOrNull(1)?.trim()
            ?: "Płatność Alior"
        return PendingPayment(merchant,value)
    }
    fun isAliorGmail(title:String,text:String):Boolean { val all="$title $text"; return all.contains("Alior Bank",true) && (all.contains("autoryzacja transakcji",true)||all.contains("Kwota transakcji",true)) }
    fun isGoogleWalletPayment(title: String, text: String): Boolean {
        val all = "$title\n$text"
        return all.contains("kwota", ignoreCase = true) && amountPattern.matcher(all).find()
    }
    fun parseGoogleWallet(title: String, text: String): PendingPayment? {
        val all = "$title\n$text"
        val matcher = amountPattern.matcher(all)
        if (!matcher.find()) return null
        val value = matcher.group(1)?.replace(" ", "")?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: return null
        val merchant = text.lineSequence().map { it.trim() }.firstOrNull {
            it.length >= 3 && !it.startsWith("kwota", ignoreCase = true) &&
                !it.contains("karta", ignoreCase = true) && !it.contains("portfel google", ignoreCase = true)
        } ?: title.trim().takeIf { it.isNotBlank() && !it.contains("Portfel Google", ignoreCase = true) }
            ?: "P\u0142atno\u015b\u0107 Portfelem Google"
        return PendingPayment(merchant.take(80), value)
    }
}

object AliorDecisionNotifier {
    fun show(context:Context,payment:PendingPayment){
        val prefs=context.getSharedPreferences("razem",Context.MODE_PRIVATE)
        prefs.edit().putString("pending_payment",JSONObject().put("merchant",payment.merchant).put("amount",payment.amount).toString()).apply()
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Propozycje wydatków Alior",NotificationManager.IMPORTANCE_HIGH))
        fun action(action:String,code:Int)=PendingIntent.getBroadcast(context,code,Intent(context,PaymentActionReceiver::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val launch=context.packageManager.getLaunchIntentForPackage(context.packageName)!!
        val open=PendingIntent.getActivity(context,3,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // "Tak" is handled by a receiver, so it works even while the app is not visible.
        val accept=action(ACCEPT,2)
        val n=Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Propozycja nowego wydatku").setContentText("Otwórz aplikację, aby sprawdzić szczegóły.").setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(open).addAction(Notification.Action.Builder(null,"Nie",action(REJECT,1)).build()).addAction(Notification.Action.Builder(null,"Tak",accept).build()).setAutoCancel(true).build()
        manager.notify(NOTIFICATION_ID,n)
    }
    const val EXTRA_ACCEPT="accept_pending_payment"; const val CHANNEL="alior_expenses"; const val ACCEPT="pl.razem.myszy.ACCEPT_PAYMENT"; const val REJECT="pl.razem.myszy.REJECT_PAYMENT"; const val NOTIFICATION_ID=9021
}

class AliorNotificationListener:NotificationListenerService(){
    override fun onNotificationPosted(sbn:StatusBarNotification){
        // Deliberately disabled for the current release. Re-enable only in a
        // future update after the manual expense flow and privacy review are
        // complete.
        if (!NOTIFICATION_READING_ENABLED) return
        val prefs=getSharedPreferences("razem",Context.MODE_PRIVATE)
        if(prefs.getBoolean("safe_mouse",true)) return
        val identity=prefs.getString("notification_identity","").orEmpty()
        if(!isPawel(identity)) return
        val fromAliorApp=sbn.packageName==ALIOR_PACKAGE
        // Shell notifications are accepted only in debug builds for emulator QA.
        val fromEmulatorTest=(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && sbn.packageName=="com.android.shell"
        val fromGmail=sbn.packageName==GMAIL_PACKAGE
        val fromGoogleWallet=sbn.packageName==GOOGLE_WALLET_PACKAGE
        if(!fromAliorApp&&!fromGmail&&!fromGoogleWallet&&!fromEmulatorTest) return
        val extras=sbn.notification.extras
        val title=extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text=listOf(Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT,Notification.EXTRA_SUB_TEXT).mapNotNull{extras.getCharSequence(it)?.toString()}.distinct().joinToString("\n")
        val fromAliorGmail=fromGmail&&AliorParser.isAliorGmail(title,text)
        val fromWalletPayment=fromGoogleWallet&&AliorParser.isGoogleWalletPayment(title,text)
        if(!fromAliorApp&&!fromAliorGmail&&!fromWalletPayment&&!fromEmulatorTest) return
        val parsed=when {
            fromWalletPayment -> AliorParser.parseGoogleWallet(title,text)
            else -> AliorParser.parse(title,text)
        } ?: if (fromEmulatorTest) PendingPayment("ZABKA Z6872 K.1 PL", 7.98) else null
        if(parsed==null) return
        AliorDecisionNotifier.show(this,parsed)
    }
    companion object{const val NOTIFICATION_READING_ENABLED=false;const val ALIOR_PACKAGE="pl.aliorbank.aib";const val GMAIL_PACKAGE="com.google.android.gm";const val GOOGLE_WALLET_PACKAGE="com.google.android.apps.walletnfcrel"}
}

class PaymentActionReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val store=Store(context);val payment=store.pending()?:return
        if(intent.action==AliorDecisionNotifier.REJECT){store.clear();context.getSystemService(NotificationManager::class.java).cancel(AliorDecisionNotifier.NOTIFICATION_ID);return}
        if(intent.action!=AliorDecisionNotifier.ACCEPT)return
        val result=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{
            var added=false
            try{
                // The receiver may run before Supabase restores the saved session.
                // Wait for it instead of silently losing an accepted payment.
                val repo=SupabaseRepository(context)
                if(repo.currentUser()==null){
                    val status=withTimeoutOrNull(8_000){ supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing } }
                    if(status !is SessionStatus.Authenticated) error("Sesja nie jest jeszcze gotowa")
                }
                val member=repo.membership() ?: error("Brak domu do zapisania wydatku")
                val expense=Expense(UUID.randomUUID().toString(),payment.merchant,payment.amount,60,member.nickname,null, payerId=member.userId)
                repo.addExpense(member.householdId,expense,"alior_notification")
                added=true
                ExpenseNotifications.show(context,expense)
            }catch(failure:Throwable){
                android.util.Log.e("MyszyAlior", "Nie udało się dodać płatności z powiadomienia")
                // Fallback: the dashboard owns a second, visible acceptance path.
                // It keeps the proposal and finishes the save after the activity is ready.
                context.startActivity(Intent(context, MainActivity::class.java)
                    .putExtra(AliorDecisionNotifier.EXTRA_ACCEPT, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }finally{
                if(added) store.clear()
                // A failure leaves the proposal visible on the dashboard for retry.
                context.getSystemService(NotificationManager::class.java).cancel(AliorDecisionNotifier.NOTIFICATION_ID)
                result.finish()
            }
        }
    }
}
