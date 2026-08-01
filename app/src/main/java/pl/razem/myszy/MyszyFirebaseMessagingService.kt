package pl.razem.myszy

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyszyFirebaseMessagingService : FirebaseMessagingService() {
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val repository = SupabaseRepository(applicationContext)
                val membership = repository.membership() ?: return@runCatching
                repository.registerFcmToken(membership.householdId, token)
            }.onFailure {
                Log.e("MyszyFcm", "Nie udało się zapisać tokenu FCM", it)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i("MyszyFcm", "Odebrano prywatny sygnał odświeżenia FCM")
        // Fresh installs first establish a baseline; old queued pushes stay silent.
        if (!Store(applicationContext).expenseNotificationsReady()) {
            Log.i("MyszyFcm", "Pominięto sygnał sprzed ustanowienia bieżącej sesji")
            return
        }
        val id = message.data["expense_id"] ?: run {
            Log.w("MyszyFcm", "Pominięto sygnał bez identyfikatora wydatku")
            return
        }
        ExpenseNotifications.show(applicationContext, id)
        Log.i("MyszyFcm", "Wyświetlono prywatne powiadomienie o wspólnym wydatku")
    }
}
