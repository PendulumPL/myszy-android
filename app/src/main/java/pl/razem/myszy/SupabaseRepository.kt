package pl.razem.myszy

import android.content.Context
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

val supabase = createSupabaseClient(
    BuildConfig.SUPABASE_URL,
    BuildConfig.SUPABASE_PUBLISHABLE_KEY
) {
    install(Auth) {
        scheme = BuildConfig.AUTH_SCHEME
        host = "auth-callback"
    }
    install(Postgrest)
    install(Storage)
    install(Functions)
}

private val authRedirectUrl = "${BuildConfig.AUTH_SCHEME}://auth-callback"

@Serializable data class MemberRow(
    @SerialName("household_id") val householdId: String,
    @SerialName("user_id") val userId: String,
    val nickname: String,
    @SerialName("avatar_id") val avatarId: Int = 0,
    @SerialName("profile_color") val profileColor: Int = 0
)

@Serializable private data class MemberProfilePatch(
    @SerialName("avatar_id") val avatarId: Int,
    @SerialName("profile_color") val profileColor: Int
)

@Serializable data class HouseholdRow(
    @SerialName("balance_correction") val balanceCorrection: Double = 0.0,
    @SerialName("created_by") val createdBy: String
)

@Serializable data class CloudExpense(
    val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("payer_id") val payerId: String,
    val merchant: String,
    val amount: Double,
    @SerialName("pawel_percent") val pawelPercent: Int,
    @SerialName("receipt_path") val receiptPath: String? = null,
    val source: String = "manual",
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("created_at") val createdAt: String,
    val category: String = "Inne",
    val comment: String = "",
    @SerialName("pawel_share") val pawelShare: Double? = null,
    @SerialName("ania_share") val aniaShare: Double? = null
)

@Serializable data class NewCloudExpense(
    val id: String,
@SerialName("household_id") val householdId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("payer_id") val payerId: String,
    val merchant: String,
    val amount: Double,
    @SerialName("pawel_percent") val pawelPercent: Int,
    @SerialName("receipt_path") val receiptPath: String? = null,
    val source: String = "manual",
    val category: String = "Inne",
    @SerialName("occurred_at") val occurredAt: String,
    val comment: String = "",
    @SerialName("pawel_share") val pawelShare: Double? = null,
    @SerialName("ania_share") val aniaShare: Double? = null
)

@Serializable data class ActivityRow(@SerialName("household_id") val householdId: String, @SerialName("expense_id") val expenseId: String? = null, @SerialName("user_id") val userId: String, val action: String, val merchant: String, val amount: Double, @SerialName("created_at") val createdAt: String)

@Serializable data class DeviceInstallation(
    @SerialName("user_id") val userId: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("token") val installationId: String
)

@Serializable data class CreateHomeResult(
    @SerialName("household_id") val householdId: String,
    @SerialName("invite_code") val inviteCode: String
)

class SupabaseRepository(private val context: Context) {
    private companion object {
        const val MAX_RECEIPT_BYTES = 10 * 1024 * 1024
    }

    private var registeredFcmFingerprint: String? = null
    private var fcmRegistrationRequested = false

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) { this.email = email; this.password = password }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email, redirectUrl = authRedirectUrl) {
            this.email = email
            this.password = password
        }
        return currentUser() != null
    }

    suspend fun requestPasswordReset(email: String) {
        supabase.auth.resetPasswordForEmail(email, redirectUrl = authRedirectUrl)
    }

    suspend fun updatePassword(password: String) {
        supabase.auth.updateUser { this.password = password }
    }

    suspend fun signOut() {
        val userId = currentUser()?.id
        if (userId != null) {
            runCatching {
                val registrationToken = withTimeoutOrNull(5_000) {
                    @Suppress("DEPRECATION")
                    FirebaseMessaging.getInstance().token.await()
                } ?: return@runCatching
                supabase.from("razem_device_tokens").delete {
                    filter { eq("token", registrationToken); eq("user_id", userId) }
                }
            }
        }
        registeredFcmFingerprint = null
        fcmRegistrationRequested = false
        supabase.auth.signOut()
    }

    fun currentUser() = supabase.auth.currentUserOrNull()

    suspend fun membership(): MemberRow? {
        val uid = currentUser()?.id ?: return null
        return supabase.from("razem_members").select {
            filter { eq("user_id", uid) }
        }.decodeList<MemberRow>().firstOrNull()
    }

    suspend fun members(householdId: String): List<MemberRow> =
        supabase.from("razem_members").select {
            filter { eq("household_id", householdId) }
        }.decodeList()

    suspend fun updateMemberProfile(householdId: String, userId: String, avatarId: Int, profileColor: Int) {
        supabase.from("razem_members").update(MemberProfilePatch(avatarId.coerceIn(0, 19), profileColor)) {
            filter { eq("household_id", householdId); eq("user_id", userId) }
        }
    }

    suspend fun balanceCorrection(householdId: String): Double = runCatching {
        household(householdId)?.balanceCorrection ?: 0.0
    }.getOrDefault(0.0)

    suspend fun household(householdId: String): HouseholdRow? =
        supabase.from("razem_households").select {
            filter { eq("id", householdId) }
        }.decodeList<HouseholdRow>().firstOrNull()

    suspend fun createHome(nickname: String): CreateHomeResult {
        return supabase.postgrest.rpc(
            "create_razem_household",
            buildJsonObject { put("member_nickname", nickname) }
        ).decodeList<CreateHomeResult>().first()
    }

    suspend fun joinHome(code: String, nickname: String) {
        supabase.postgrest.rpc(
            "join_razem_household",
            buildJsonObject { put("code", code); put("member_nickname", nickname) }
        )
    }

    suspend fun registerFcmToken(householdId: String, refreshedToken: String? = null) {
        val user = currentUser() ?: return
        if (refreshedToken == null && fcmRegistrationRequested) return
        if (refreshedToken == null) fcmRegistrationRequested = true
        try {
            val registrationToken = refreshedToken ?: withTimeoutOrNull(20_000) {
                @Suppress("DEPRECATION")
                FirebaseMessaging.getInstance().token.await()
            }
            if (registrationToken == null) {
                fcmRegistrationRequested = false
                Log.w("MyszyFcm", "Nie pobrano tokenu FCM w ciągu 20 sekund")
                return
            }
            val fingerprint = "${user.id}|$householdId|$registrationToken"
            if (registeredFcmFingerprint == fingerprint) return
            supabase.from("razem_device_tokens").upsert(
                DeviceInstallation(user.id, householdId, registrationToken)
            ) { onConflict = "token" }
            registeredFcmFingerprint = fingerprint
            Log.i("MyszyFcm", "Zarejestrowano token FCM dla bieżącego urządzenia")
        } catch (failure: Throwable) {
            // A temporary Firebase/Supabase failure must not disable registration
            // for the rest of this app process. The next refresh may retry safely.
            if (refreshedToken == null) fcmRegistrationRequested = false
            throw failure
        }
    }

    suspend fun activities(householdId: String): List<ActivityRow> = supabase.from("razem_activity").select { filter { eq("household_id", householdId) }; order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING); limit(10) }.decodeList()

    suspend fun expenses(householdId: String): List<CloudExpense> {
        // Supabase caps a single response (usually at 1,000 rows). Fetch every
        // page so imported Settle Up history is never silently truncated.
        val all = mutableListOf<CloudExpense>()
        var from = 0L
        val pageSize = 1_000L
        while (true) {
            val page = supabase.from("razem_expenses").select {
                filter { eq("household_id", householdId) }
                range(from..(from + pageSize - 1))
            }.decodeList<CloudExpense>()
            all += page
            if (page.size < pageSize) return all
            from += pageSize
        }
    }


    suspend fun receiptUrl(path: String): String =
        supabase.storage.from("receipts").createSignedUrl(path, kotlin.time.Duration.parse("5m"))

    suspend fun removeReceipt(householdId: String, expenseId: String, path: String) {
        // Clear the database reference first. If deleting the object then fails,
        // the result is only an orphan file, never a broken receipt link in the UI.
        supabase.from("razem_expenses").update({ set("receipt_path", null as String?) }) {
            filter { eq("id", expenseId); eq("household_id", householdId) }
        }
        runCatching { supabase.storage.from("receipts").delete(path) }
    }
    suspend fun deleteExpense(householdId: String, expense: Expense) {
        var lastFailure: Throwable? = null
        repeat(3) { attempt ->
            try {
                supabase.from("razem_expenses").delete {
                    filter { eq("id", expense.id); eq("household_id", householdId) }
                }
                expense.receipt?.let { runCatching { supabase.storage.from("receipts").delete(it) } }
                return
            } catch (failure: Throwable) {
                lastFailure = failure
                if (attempt < 2) delay(700L * (attempt + 1))
            }
        }
        throw requireNotNull(lastFailure)
    }
    suspend fun updateExpense(householdId: String, expense: Expense, previousReceipt: String?) {
        val user = currentUser() ?: error("Brak sesji")
        val householdMembers = members(householdId)
        val payerId = expense.payerId ?: householdMembers.firstOrNull { member ->
            val payer = expense.payer.lowercase()
            val nickname = member.nickname.lowercase()
            payer == nickname ||
                (payer.contains("myszo") && nickname.contains("myszo")) ||
                (payer.contains("mysza") && nickname.contains("mysza")) ||
                (payer.contains("pawe") && nickname.contains("myszo")) ||
                (payer.contains("ania") && nickname.contains("mysza"))
        }?.userId ?: user.id
        val receiptPath = uploadReceipt(householdId, expense.receipt)
        try {
            supabase.from("razem_expenses").update({
            set("merchant", expense.merchant)
            set("amount", expense.amount)
            set("pawel_percent", expense.pawel)
            set("payer_id", payerId)
            set("receipt_path", receiptPath as String?)
            set("category", expense.category)
            set("occurred_at", expense.occurredAt)
            set("comment", expense.comment)
            set("pawel_share", expense.pawelShare as Double?)
            set("ania_share", expense.aniaShare as Double?)
            }) {
                filter {
                    eq("id", expense.id)
                    eq("household_id", householdId)
                }
            }
        } catch (failure: Throwable) {
            receiptPath?.takeIf { it != previousReceipt }?.let {
                runCatching { supabase.storage.from("receipts").delete(it) }
            }
            throw failure
        }
        if (previousReceipt != null && previousReceipt != receiptPath) runCatching { supabase.storage.from("receipts").delete(previousReceipt) }
        deleteOwnedLocalReceipt(expense.receipt)
        sendExpensePush(householdId, expense)
    }
    suspend fun sendExpensePush(householdId: String, expense: Expense) {
        // Powiadomienie jest tylko sygnałem odświeżenia: bez danych finansowych w FCM.
        runCatching {
            supabase.functions.invoke(
                function = "send-expense-push",
                body = buildJsonObject {
                    put("expense_id", expense.id)
                    put("household_id", householdId)
                },
                headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
            )
        }.onSuccess {
            Log.i("MyszyFcm", "Wywołanie powiadomienia zostało przyjęte przez zaplecze")
        }.onFailure { Log.e("MyszyFcm", "Wywołanie powiadomienia nie powiodło się", it) }
    }

    suspend fun addExpense(householdId: String, expense: Expense, source: String = "manual") {
        val user = currentUser() ?: error("Brak sesji")
        val householdMembers = members(householdId)
        val payerId = expense.payerId ?: householdMembers.firstOrNull { member ->
            val payer = expense.payer.lowercase()
            val nickname = member.nickname.lowercase()
            payer == nickname ||
                (payer.contains("myszo") && nickname.contains("myszo")) ||
                (payer.contains("mysza") && nickname.contains("mysza")) ||
                (payer.contains("pawe") && nickname.contains("myszo")) ||
                (payer.contains("ania") && nickname.contains("mysza"))
        }?.userId ?: user.id
        val receiptPath = uploadReceipt(householdId, expense.receipt)
        try {
            supabase.from("razem_expenses").insert(
                NewCloudExpense(expense.id, householdId, user.id, payerId, expense.merchant, expense.amount, expense.pawel, receiptPath, source, expense.category, expense.occurredAt, expense.comment, expense.pawelShare, expense.aniaShare)
            )
        } catch (failure: Throwable) {
            receiptPath?.let { runCatching { supabase.storage.from("receipts").delete(it) } }
            throw failure
        }
        deleteOwnedLocalReceipt(expense.receipt)
        sendExpensePush(householdId, expense)
    }

    private suspend fun uploadReceipt(householdId: String, value: String?): String? {
        if (value == null || value.startsWith("$householdId/")) return value
        if (value == DEMO_RECEIPT_PATH) return value
        val uri = Uri.parse(value)
        val bytes = context.contentResolver.openInputStream(uri)?.use {
            it.readAtMost(MAX_RECEIPT_BYTES, "Zdjęcie paragonu jest zbyt duże. Maksymalny rozmiar to 10 MB.")
        } ?: error("Nie udało się odczytać zdjęcia paragonu")
        val extension = imageExtension(bytes) ?: error("Paragon musi być zdjęciem JPG, PNG lub WebP.")
        val path = "$householdId/${UUID.randomUUID()}.$extension"
        supabase.storage.from("receipts").upload(path, bytes) { upsert = false }
        return path
    }

    private fun imageExtension(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "jpg"
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "png"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" && bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> "webp"
        else -> null
    }

    private fun deleteOwnedLocalReceipt(value: String?) {
        val uri = value?.let(Uri::parse) ?: return
        if (uri.authority != "${context.packageName}.files") return
        val name = uri.lastPathSegment ?: return
        if (!Regex("receipt-\\d+\\.jpg").matches(name)) return
        File(context.filesDir, "receipts/$name").delete()
    }
}
