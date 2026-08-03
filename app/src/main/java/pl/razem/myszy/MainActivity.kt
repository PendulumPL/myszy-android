package pl.razem.myszy

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.handleDeeplinks
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

val MousePink = Color(0xFFC66F54)
val MousePinkSoft = Color(0xFFF3D7C9)
val AniaPurple = Color(0xFFC66F54)
val AniaPurpleSoft = Color(0xFFF3D7C9)
val BearBlue = Color(0xFF708B76)
val BearBlueSoft = Color(0xFFE5EEE6)
val MoneyGreen = Color(0xFF147D64)
val WarmSurface = Color(0xFFFFFBFE)
val MouseCream = Color(0xFFFFF8F1)
val MouseSurface = Color(0xFFFFFCF8)
val MouseInk = Color(0xFF302B28)
val MouseMuted = Color(0xFF857A72)
val MouseTerracotta = Color(0xFFC66F54)
val MouseTerracottaSoft = Color(0xFFF3D7C9)
val MousePeach = MouseTerracottaSoft
val MouseSage = Color(0xFF708B76)
val MouseSageSoft = Color(0xFFE5EEE6)
val MouseLavender = Color(0xFF8175A7)
val MouseLavenderSoft = Color(0xFFEDE9F7)
val MouseLine = Color(0xFFE7DDD4)
// DEV-only marker used for the portfolio receipt preview. It is never uploaded
// to storage; the app renders the bundled fictional receipt instead.
const val DEMO_RECEIPT_PATH = "demo://myszy-receipt-v1"
val mouseMotifResources = listOf(
    R.drawable.mysza_clean_01, R.drawable.mysza_clean_02, R.drawable.mysza_clean_03,
    R.drawable.mysza_clean_04, R.drawable.mysza_clean_05, R.drawable.mysza_clean_06,
    R.drawable.mysza_clean_07, R.drawable.mysza_clean_08, R.drawable.mysza_clean_09,
    R.drawable.mysza_clean_10, R.drawable.mysza_avatar_01, R.drawable.mysza_avatar_02,
    R.drawable.mysza_avatar_03, R.drawable.mysza_avatar_04, R.drawable.mysza_avatar_05,
    R.drawable.mysza_avatar_06, R.drawable.mysza_avatar_07, R.drawable.mysza_avatar_08,
    R.drawable.mysza_avatar_09, R.drawable.mysza_avatar_10
)
val aniaAvatarResources = mouseMotifResources
val aniaPurplePalette = listOf(
    MouseTerracotta, MouseSage, Color(0xFFA97C50), Color(0xFFC97C7C), Color(0xFF8C7A6B),
    Color(0xFF6D7964), Color(0xFFD09A61), Color(0xFF9A6A5D), Color(0xFF6F7875), Color(0xFFB57987),
    Color(0xFFCF6F57), Color(0xFF7EA98A), Color(0xFFE08B8B), Color(0xFF9B8E7E),
    Color(0xFFE1A764), Color(0xFFB87868), Color(0xFF84918D), Color(0xFFC78A9A),
    Color(0xFF73A9D8), Color(0xFFC879A8), Color(0xFF8D77C4), Color(0xFF55AFA6)
)
data class Expense(val id: String, val merchant: String, val amount: Double, val pawel: Int, val payer: String, val receipt: String?, val occurredAt: String = java.time.Instant.now().toString(), val category: String = "Inne", val createdAt: String = java.time.Instant.now().toString(), val comment: String = "", val pawelShare: Double? = null, val aniaShare: Double? = null, val payerId: String? = null)
data class PendingPayment(val merchant: String, val amount: Double)

class Store(context: Context) {
    private val preferences = context.getSharedPreferences("razem", Context.MODE_PRIVATE)
    fun pending(): PendingPayment? = preferences.getString("pending_payment", null)?.let {
        runCatching { JSONObject(it).let { value -> PendingPayment(value.getString("merchant"), value.getDouble("amount")) } }.getOrNull()
    }
    fun clear() = preferences.edit().remove("pending_payment").apply()
    fun safeMode(): Boolean = preferences.getBoolean("safe_mouse", true)
    fun setSafeMode(safe: Boolean) = preferences.edit().putBoolean("safe_mouse", safe).apply()
    fun setNotificationIdentity(identity: String) = preferences.edit().putString("notification_identity", identity).apply()
    fun expenseNotificationsReady(): Boolean = preferences.getBoolean("expense_notifications_ready", false)
    fun setExpenseNotificationsReady(ready: Boolean) = preferences.edit().putBoolean("expense_notifications_ready", ready).apply()
    fun clearSensitiveSessionState() = preferences.edit()
        .remove("expenses")
        .remove("pending_payment")
        .remove("last_unparsed_alior")
        .remove("notification_identity")
        .remove("expense_notifications_ready")
        .putBoolean("safe_mouse", true)
        .apply()
}

class MainActivity : ComponentActivity() {
    private companion object {
        const val AUTH_PREFERENCES = "razem_auth"
        const val PASSWORD_RECOVERY_REQUESTED = "password_recovery_requested"
    }

    private var requestedExpenseId by mutableStateOf<String?>(null)
    private var acceptPendingRequest by mutableStateOf(false)
    private var authRefreshToken by mutableIntStateOf(0)
    private var passwordRecovery by mutableStateOf(false)
    private var authLinkError by mutableStateOf<String?>(null)
    private var passwordRecoveryRequested by mutableStateOf(false)
    private val authPreferences by lazy { getSharedPreferences(AUTH_PREFERENCES, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        requestedExpenseId = intent.getStringExtra(ExpenseNotifications.EXTRA_EXPENSE_ID)
        acceptPendingRequest = intent.getBooleanExtra(AliorDecisionNotifier.EXTRA_ACCEPT, false)
        passwordRecoveryRequested = authPreferences.getBoolean(PASSWORD_RECOVERY_REQUESTED, false)
        handleAuthIntent(intent)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = MouseTerracotta, secondary = MouseSage, tertiary = MoneyGreen, surface = MouseSurface)) {
                Surface(Modifier.fillMaxSize()) {
                    CloudRazemApp(
                        openExpenseId = requestedExpenseId,
                        consumedOpenExpense = { requestedExpenseId = null },
                        acceptPending = acceptPendingRequest,
                        consumedAcceptPending = { acceptPendingRequest = false },
                        authRefreshToken = authRefreshToken,
                        passwordRecovery = passwordRecovery,
                        consumedPasswordRecovery = ::finishPasswordRecovery,
                        onPasswordResetRequested = ::markPasswordRecoveryRequested,
                        externalAuthError = authLinkError,
                        consumedExternalAuthError = { authLinkError = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedExpenseId = intent.getStringExtra(ExpenseNotifications.EXTRA_EXPENSE_ID)
        acceptPendingRequest = intent.getBooleanExtra(AliorDecisionNotifier.EXTRA_ACCEPT, false)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        val uri = intent.data ?: return
        val isRecovery = passwordRecoveryRequested || uri.getQueryParameter("type") == "recovery" ||
            uri.fragment?.split('&')?.any { it == "type=recovery" } == true
        supabase.handleDeeplinks(
            intent,
            onSessionSuccess = {
                runOnUiThread {
                    passwordRecovery = isRecovery
                    authLinkError = null
                    authRefreshToken++
                }
            },
            onError = {
                runOnUiThread {
                    authLinkError = "Link wygasł albo został już użyty. Poproś o nową wiadomość."
                    authRefreshToken++
                }
            }
        )
    }

    private fun markPasswordRecoveryRequested() {
        passwordRecoveryRequested = true
        authPreferences.edit().putBoolean(PASSWORD_RECOVERY_REQUESTED, true).apply()
    }

    private fun finishPasswordRecovery() {
        passwordRecovery = false
        passwordRecoveryRequested = false
        authPreferences.edit().remove(PASSWORD_RECOVERY_REQUESTED).apply()
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeLegacy(user: String, isPawelUser: Boolean, pawelUserId: String?, xs: List<Expense>, balanceCorrection: Double = 0.0, lastActivity: Pair<String, Expense>? = null, pending: PendingPayment?, safeMode: Boolean, setSafeMode: (Boolean) -> Unit, add: () -> Unit, edit: (Expense) -> Unit, viewReceipt: (Expense) -> Unit, removeReceipt: (Expense) -> Unit, importBank: () -> Unit, resumeImport: () -> Unit, hasImport: Boolean, canReadNotifications: Boolean, settle: () -> Unit, settlementHistory: () -> Unit, accept: (PendingPayment) -> Unit, reject: () -> Unit, logout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAnia = !isPawelUser
    val prefs = remember { context.getSharedPreferences("razem_profile", Context.MODE_PRIVATE) }
    var avatarIndex by remember { mutableIntStateOf(prefs.getInt("ania_avatar", 0).coerceIn(0, aniaAvatarResources.lastIndex)) }
    var accentArgb by remember { mutableIntStateOf(prefs.getInt("ania_color", AniaPurple.toArgb())) }
    var avatarDialog by rememberSaveable { mutableStateOf(false) }
    var colorDialog by rememberSaveable { mutableStateOf(false) }
    // Each person owns their profile accent. Older builds forced Pawel to blue,
    // which made the color picker appear to do nothing on his device.
    val accent = Color(accentArgb)
    val headerAvatar = if (isAnia) aniaAvatarResources[avatarIndex] else R.drawable.misio_pawel
    val allSettlements = remember(xs) { xs.filter(::isSettlement).sortedByDescending { it.occurredAt } }
    var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
    val settlements = if (settlementsExpanded) allSettlements else allSettlements.take(3)
    val balance = calculatePawelBalance(xs, balanceCorrection, pawelUserId)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStore by rememberSaveable { mutableStateOf<String?>(null) }
    var datePreset by rememberSaveable { mutableStateOf("ALL") }
    var rangeStart by rememberSaveable { mutableStateOf("") }
    var rangeEnd by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var storePickerOpen by rememberSaveable { mutableStateOf(false) }
    val filteredExpenses = remember(xs, searchQuery, selectedCategory, selectedStore, datePreset, rangeStart, rangeEnd) {
        val q = searchQuery.trim()
        xs.filter { expense ->
            (q.isEmpty() || listOf(expense.merchant, expense.category, expense.comment, expense.payer).any { it.contains(q, ignoreCase = true) }) &&
                (selectedCategory == null || expense.category == selectedCategory) &&
                (selectedStore == null || expense.merchant.contains(selectedStore!!, ignoreCase = true) || (selectedStore == "Żabka" && expense.merchant.contains("zabka", ignoreCase = true))) &&
                matchesDateFilter(expense.occurredAt, datePreset, rangeStart, rangeEnd)
        }
    }
    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { java.time.YearMonth.from(parseExpenseDay(it.occurredAt)) }
            .toList().sortedByDescending { it.first }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Scaffold(
        containerColor = Color(0xFFF8F3FA),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = Color.White.copy(alpha = .96f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp,
                shadowElevation = 12.dp
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = if (hasImport) resumeImport else importBank,
                        modifier = Modifier.weight(1f).height(50.dp),
                         shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF218C4B)),
                        contentPadding = PaddingValues(horizontal = 5.dp)
                    ) { Icon(Icons.Default.TableChart, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (hasImport) "Import" else "PDF / Excel", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
                    Button(
                        onClick = settlementHistory,
                        modifier = Modifier.weight(.8f).height(50.dp),
                         shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F6F61)),
                        contentPadding = PaddingValues(horizontal = 5.dp)
                    ) { Text("↔", fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(4.dp)); Text("Spłaty", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
                    Button(
                        onClick = add,
                        modifier = Modifier.weight(.8f).height(50.dp),
                         shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 5.dp)
                    ) { Text("+", fontSize = 21.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(3.dp)); Text("Dodaj", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            item {
                Column(Modifier.clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)).background(Brush.linearGradient(listOf(accent, accent.copy(alpha = .74f)))).fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(headerAvatar), null, Modifier.size(62.dp).clip(CircleShape).background(Color.White).then(if (isAnia) Modifier.clickable { avatarDialog = true } else Modifier), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (isAnia) "Mysza Ania" else "Misio Paweł", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (isAnia) "Dotknij avataru, aby go zmienić" else if (safeMode) "Standard" else "Odczyt powiadomień włączony", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                        }
                        TextButton(logout) { Text("Wyloguj", color = Color.White) }
                    }
                    if (isAnia) {
                        Spacer(Modifier.height(10.dp))
                        Surface(color = Color.White.copy(alpha = .18f), shape = RoundedCornerShape(14.dp), modifier = Modifier.clickable { colorDialog = true }) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(16.dp).clip(CircleShape).background(accent)); Spacer(Modifier.width(8.dp)); Text("Fiolet panelu", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Spacer(Modifier.width(6.dp)); Text("Zmień", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                            }
                        }
                    }
                    if (canReadNotifications) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = if (safeMode) Color.White else Color.White.copy(alpha = .16f), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).clickable { setSafeMode(true) }) { Text("Standard", Modifier.padding(vertical = 10.dp), color = if (safeMode) accent else Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold) }
                            Surface(color = if (!safeMode) Color.White else Color.White.copy(alpha = .16f), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).clickable { setSafeMode(false); if (!notificationAccess(context)) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Odczyt powiadomień*", Modifier.padding(vertical = 10.dp), color = if (!safeMode) accent else Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold) }
                        }
                        Text("* Tylko zatwierdzone powiadomienia płatności — aplikacja proponuje wydatek, ale nigdy nie zapisuje go sama.", color = Color.White.copy(alpha = .82f), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
            item {
                ElevatedCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = accent.copy(alpha = .08f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("🐭  WSPÓLNY DOM", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Button(settle, colors = ButtonDefaults.buttonColors(containerColor = MoneyGreen, contentColor = Color.White), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp)) { Text("Spłać", fontWeight = FontWeight.ExtraBold) }
                    }
                    Text("Bieżący bilans", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(when { balance > .005 -> "Ania ma do oddania"; balance < -.005 -> "Paweł ma do oddania"; else -> "Jesteście rozliczeni" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (kotlin.math.abs(balance) > .005) money(kotlin.math.abs(balance)) else money(0.0), color = MoneyGreen, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color.White.copy(alpha = .7f), shape = RoundedCornerShape(12.dp)) { Text("${filteredExpenses.size} wydatków", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent) }
                        Surface(color = Color.White.copy(alpha = .7f), shape = RoundedCornerShape(12.dp)) { Text("${allSettlements.size} spłat", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MoneyGreen) }
                    }
                } }
            }
            if (!safeMode && !notificationAccess(context)) item { AssistChip({ context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, { Text("Dokończ dostęp do odczytu powiadomień") }, Modifier.padding(horizontal = 16.dp)) }
            pending?.let { payment -> item { ElevatedCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF3D8)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Wykryto płatność Alior", fontWeight = FontWeight.Bold); Text(payment.merchant, style = MaterialTheme.typography.titleLarge); Text(money(payment.amount), color = MoneyGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(reject) { Text("Odrzuć") }; Button({ accept(payment) }) { Text("Dodaj 60/40") } } } } } }
            item {
                ElevatedCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Filtry wydatków", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (searchQuery.isNotBlank() || selectedCategory != null || selectedStore != null || datePreset != "ALL") TextButton({ searchQuery = ""; selectedCategory = null; selectedStore = null; datePreset = "ALL"; rangeStart = ""; rangeEnd = "" }) { Text("Wyczyść") }
                        }
                        OutlinedButton({ filtersExpanded = !filtersExpanded }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Text(if (filtersExpanded) "Zwiń panel filtrów" else "Szukaj i filtruj wydatki", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f)); Text(if (filtersExpanded) "⌃" else "⌄", fontSize = 18.sp)
                        }
                        if (filtersExpanded) {
                            OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Szukaj") }, placeholder = { Text("sklep, opis lub osoba") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                            Text("OKRES", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                FilterChip(datePreset == "ALL", { datePreset = "ALL" }, { Text("Wszystkie") }); FilterChip(datePreset == "WEEK", { datePreset = "WEEK" }, { Text("7 dni") }); FilterChip(datePreset == "MONTH", { datePreset = "MONTH" }, { Text("Ten miesiąc") }); FilterChip(datePreset == "YEAR", { datePreset = "YEAR" }, { Text("Ten rok") }); FilterChip(datePreset == "CUSTOM", { datePreset = "CUSTOM" }, { Text("Własny zakres") })
                            }
                            if (datePreset == "CUSTOM") Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton({ val initial = parseFilterDate(rangeStart) ?: LocalDate.now(); DatePickerDialog(context, { _, y, m, d -> rangeStart = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show() }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Od: ${formatFilterDate(rangeStart)}", maxLines = 1) }
                                OutlinedButton({ val initial = parseFilterDate(rangeEnd) ?: LocalDate.now(); DatePickerDialog(context, { _, y, m, d -> rangeEnd = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show() }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Do: ${formatFilterDate(rangeEnd)}", maxLines = 1) }
                            }
                            Text("TYP WYDATKU", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                            val categoryRows = listOf(
                                listOf<String?>(null, "Jedzenie"),
                                listOf<String?>("Spożywcze", "Zakupy"),
                                listOf<String?>("Dom", "Paliwo"),
                                listOf<String?>("Zdrowie", "Transport"),
                                listOf<String?>("Rozrywka", "Rachunki"),
                                listOf<String?>("Inne", null)
                            )
                            categoryRows.forEachIndexed { rowIndex, row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { category ->
                                        when {
                                            category == null && rowIndex == 0 -> FilterChip(selectedCategory == null, { selectedCategory = null }, { Text("Wszystkie", maxLines = 1) }, modifier = Modifier.weight(1f))
                                            category == null -> Spacer(Modifier.weight(1f))
                                            category == "Spożywcze" -> FilterChip(selectedCategory == category, { selectedCategory = if (selectedCategory == category) null else category }, { Text(category, maxLines = 1) }, modifier = Modifier.weight(1f), leadingIcon = { Image(painterResource(R.drawable.spozywcze), null, Modifier.size(18.dp), contentScale = ContentScale.Fit) })
                                            else -> FilterChip(selectedCategory == category, { selectedCategory = if (selectedCategory == category) null else category }, { Text(categoryIcon(category) + " " + category, maxLines = 1) }, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Text("SKLEP", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                            OutlinedButton({ storePickerOpen = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { selectedStore?.let { name -> groceryStores.firstOrNull { it.name == name }?.let { Image(painterResource(it.logo), name, Modifier.size(22.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(8.dp)) } }; Text(selectedStore ?: "Wszystkie sklepy", modifier = Modifier.weight(1f)); Text("Wybierz") }
                        }
                    }
                }
            }
            item { Text("Historia wydatków (${filteredExpenses.size})", Modifier.padding(horizontal = 16.dp, vertical = 3.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (filteredExpenses.isEmpty()) item { Text(if (xs.isEmpty()) "Jeszcze nie ma wydatków." else "Brak wydatków dla wybranych filtrów.", Modifier.padding(horizontal = 16.dp), color = Color.Gray) }
            groupedExpenses.forEach { (month, expenses) ->
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatExpenseMonth(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${money(expenses.filterNot(::isSettlement).sumOf { it.amount })} • ${expenses.size}", color = MoneyGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                items(expenses, key = { it.id }, contentType = { "expense" }) { expense ->
                    ExpenseHistoryCard(expense = expense, accent = accent, aniaAvatar = aniaAvatarResources[avatarIndex], pawelUserId = pawelUserId, onEdit = edit)
                }
            }
            if (settlements.isNotEmpty()) item {
                ElevatedCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF2F7F4))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Historia spłat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton({ settlementsExpanded = !settlementsExpanded }) {
                                Text(if (settlementsExpanded) "Zwiń" else "Rozwiń", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text(if (settlementsExpanded) "⌃" else "⌄", fontSize = 18.sp)
                            }
                        }
                        settlements.forEach { payment ->
                            val byPawel = isPawel(payment.payer)
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { edit(payment) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(if (byPawel) R.drawable.misio_pawel else aniaAvatarResources[avatarIndex]), null, Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(payment.payer + " rozlicza", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(formatExpenseDate(payment.occurredAt), fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(money(payment.amount), color = MoneyGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!settlementsExpanded && allSettlements.size > 3) Text("Pokazano 3 z ${allSettlements.size} spłat", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
    if (storePickerOpen) AlertDialog(
        onDismissRequest = { storePickerOpen = false },
        title = { Text("Wybierz sklep") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { TextButton({ selectedStore = null; storePickerOpen = false }, Modifier.fillMaxWidth()) { Text("Wszystkie sklepy", modifier = Modifier.weight(1f)); if (selectedStore == null) Text("✓") } }
                items(groceryStores, key = { it.name }) { store ->
                    TextButton({ selectedStore = store.name; storePickerOpen = false }, Modifier.fillMaxWidth()) {
                        Image(painterResource(store.logo), store.name, Modifier.size(28.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(10.dp))
                        Text(store.name, modifier = Modifier.weight(1f))
                        if (selectedStore == store.name) Text("✓")
                    }
                }
            }
        },
        confirmButton = { TextButton({ storePickerOpen = false }) { Text("Zamknij") } }
    )
    if (avatarDialog) AlertDialog(onDismissRequest = { avatarDialog = false }, title = { Text("Wybierz avatar Myszy") }, text = { FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { aniaAvatarResources.forEachIndexed { index, resource -> Image(painterResource(resource), "Avatar ${index + 1}", Modifier.size(62.dp).clip(CircleShape).background(AniaPurpleSoft).clickable { avatarIndex = index; prefs.edit().putInt("ania_avatar", index).apply(); avatarDialog = false }, contentScale = ContentScale.Crop) } } }, confirmButton = { TextButton({ avatarDialog = false }) { Text("Gotowe") } })
    if (colorDialog) AlertDialog(onDismissRequest = { colorDialog = false }, title = { Text("Fiolet panelu Ani") }, text = { FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { aniaPurplePalette.forEach { color -> Surface(color = color, shape = CircleShape, modifier = Modifier.size(42.dp).clickable { accentArgb = color.toArgb(); prefs.edit().putInt("ania_color", color.toArgb()).apply(); colorDialog = false }) {} } } }, confirmButton = { TextButton({ colorDialog = false }) { Text("Zostaw") } })
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun Home(user: String, isPawelUser: Boolean, pawelUserId: String?, xs: List<Expense>, members: List<MemberRow> = emptyList(), currentUserId: String? = null, accountEmail: String = "", balanceCorrection: Double = 0.0, lastActivity: Pair<String, Expense>? = null, pending: PendingPayment?, safeMode: Boolean, setSafeMode: (Boolean) -> Unit, add: () -> Unit, edit: (Expense) -> Unit, viewReceipt: (Expense) -> Unit, removeReceipt: (Expense) -> Unit, importBank: () -> Unit, resumeImport: () -> Unit, hasImport: Boolean, canReadNotifications: Boolean, settle: () -> Unit, settlementHistory: () -> Unit, accept: (PendingPayment) -> Unit, reject: () -> Unit, logout: () -> Unit, saveProfile: (Int, Int) -> Unit = { _, _ -> }) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAnia = !isPawelUser
    val prefs = remember { context.getSharedPreferences("razem_profile", Context.MODE_PRIVATE) }
    val currentMember = members.firstOrNull { it.userId == currentUserId }
    val partnerMember = members.firstOrNull { it.userId != currentUserId }
    val hasPartner = partnerMember != null
    var avatarIndex by remember(currentMember?.avatarId) { mutableIntStateOf((currentMember?.avatarId ?: prefs.getInt("profile_avatar", prefs.getInt("ania_avatar", 0))).coerceIn(0, mouseMotifResources.lastIndex)) }
    var accentArgb by remember(currentMember?.profileColor) { mutableIntStateOf((currentMember?.profileColor?.takeIf { it != 0 } ?: prefs.getInt("ania_color", MouseTerracotta.toArgb()))) }
    var avatarDialog by rememberSaveable { mutableStateOf(false) }
    var colorDialog by rememberSaveable { mutableStateOf(false) }
    var profileDialog by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStore by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var datePreset by rememberSaveable { mutableStateOf("ALL") }
    var rangeStart by rememberSaveable { mutableStateOf("") }
    var rangeEnd by rememberSaveable { mutableStateOf("") }
    var storePickerOpen by rememberSaveable { mutableStateOf(false) }
    var selectedNav by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val accent = Color(accentArgb)
    val currentName = currentMember?.nickname?.takeIf { it.isNotBlank() } ?: user
    val partnerName = partnerMember?.nickname?.takeIf { it.isNotBlank() }.orEmpty()
    val currentAvatar = mouseMotifResources[avatarIndex]
    // The partner's motif is part of their synced member profile. Keep an
    // identity-based fallback only for older rows created before V26.
    val partnerAvatarIndex = partnerMember?.avatarId?.takeIf { it in mouseMotifResources.indices }
        ?: if (isPawelUser) 1 else 0
    val partnerAvatar = if (hasPartner) mouseMotifResources[partnerAvatarIndex] else null
    val allSettlements = remember(xs) { xs.filter(::isSettlement).sortedByDescending { it.occurredAt } }
    val balance = calculatePawelBalance(xs, balanceCorrection, pawelUserId)
    val personalBalance = if (isPawelUser) balance else -balance
    val filteredExpenses = remember(xs, searchQuery, selectedCategory, selectedStore, datePreset, rangeStart, rangeEnd) {
        val query = searchQuery.trim()
        xs.filter { expense ->
            (query.isEmpty() || listOf(expense.merchant, expense.category, expense.comment, expense.payer).any { it.contains(query, ignoreCase = true) }) &&
                (selectedCategory == null || expense.category == selectedCategory) &&
                (selectedStore == null || expense.merchant.contains(selectedStore!!, ignoreCase = true) || (selectedStore == "Żabka" && expense.merchant.contains("zabka", ignoreCase = true))) &&
                matchesDateFilter(expense.occurredAt, datePreset, rangeStart, rangeEnd)
        }
    }
    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses.groupBy { java.time.YearMonth.from(parseExpenseDay(it.occurredAt)) }.toList().sortedByDescending { it.first }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }

    Scaffold(
        containerColor = MouseCream,
        bottomBar = {
            NavigationBar(containerColor = MouseSurface, tonalElevation = 2.dp) {
                val navColors = NavigationBarItemDefaults.colors(selectedIconColor = accent, selectedTextColor = MouseInk, indicatorColor = accent.copy(alpha = .18f), unselectedIconColor = MouseMuted, unselectedTextColor = MouseMuted)
                NavigationBarItem(selected = selectedNav == 0, onClick = { selectedNav = 0; scope.launch { listState.animateScrollToItem(0) } }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Norka") }, colors = navColors)
                NavigationBarItem(selected = selectedNav == 1, onClick = { selectedNav = 1; scope.launch { listState.animateScrollToItem(5.coerceAtMost(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)) } }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Historia") }, colors = navColors)
                NavigationBarItem(selected = selectedNav == 2, onClick = { selectedNav = 0; add() }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Dodaj wydatek", fontSize = 10.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }, colors = navColors)
                NavigationBarItem(selected = selectedNav == 3, onClick = { selectedNav = 3; settlementHistory() }, icon = { Icon(Icons.Default.Payments, null) }, label = { Text("Spłaty") }, colors = navColors)
                NavigationBarItem(selected = selectedNav == 4, onClick = { selectedNav = 4; profileDialog = true }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Mój profil") }, colors = navColors)
            }
        }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 112.dp)) {
            item {
                Surface(color = MouseCream) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { avatarDialog = true }) {
                                    Text("Oto ja", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Image(painterResource(currentAvatar), "Moja mysz", Modifier.size(52.dp).clip(CircleShape).background(accent.copy(alpha = .18f)).border(BorderStroke(2.dp, accent), CircleShape), contentScale = ContentScale.Crop)
                                    Text(currentName, color = MouseInk, fontSize = 10.sp, maxLines = 1)
                                }
                                if (hasPartner && partnerAvatar != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(partnerName, color = MouseSage, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Image(painterResource(partnerAvatar), "Mysz drugiej osoby", Modifier.size(52.dp).clip(CircleShape).background(MouseSageSoft), contentScale = ContentScale.Crop)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Nasza Norka", color = MouseInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    if (hasPartner) "$currentName + $partnerName" else "$currentName · oczekuje na drugą osobę",
                                    color = MouseMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            TextButton(onClick = { profileDialog = true }) { Text("Mój profil", color = accent, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MouseSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = .45f)),
                            modifier = Modifier.fillMaxWidth().clickable { profileDialog = true }
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(currentAvatar), "Twój motyw myszy", Modifier.size(38.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Zalogowano jako", color = MouseMuted, fontSize = 11.sp)
                                    Text(currentName, color = MouseInk, fontWeight = FontWeight.Bold)
                                    Text(accountEmail.ifBlank { "konto wspólnej norki" }, color = MouseMuted, fontSize = 12.sp, maxLines = 1)
                                }
                                 Text("Profil", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            item {
                Surface(color = accent.copy(alpha = .10f), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, accent.copy(alpha = .28f)), modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bilans norki", color = MouseSage, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${xs.count { !isSettlement(it) }} wydatków", color = MouseMuted, fontSize = 12.sp)
                        }
                        val partnerLabel = partnerName.ifBlank { "Druga osoba" }
                        val balanceLabel = when {
                            personalBalance > .005 -> "Jesteś do przodu"
                            personalBalance < -.005 -> "$partnerLabel jest do przodu"
                            else -> "Myszy są kwita 🐭🐭"
                        }
                        Text(balanceLabel, color = MouseInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                         Text(if (kotlin.math.abs(personalBalance) > .005) money(kotlin.math.abs(personalBalance)) else money(0.0), color = accent, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        if (kotlin.math.abs(personalBalance) > .005) Text("Kwota do spokojnego wyrównania między Wami", color = MouseMuted, fontSize = 12.sp)
                         Button(onClick = settle, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Wyrównaj bilans", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            if (!safeMode && !notificationAccess(context)) item { AssistChip({ context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, { Text("Dokończ dostęp do odczytu płatności") }, Modifier.padding(horizontal = 16.dp)) }
            pending?.let { payment ->
                item {
                    Surface(color = Color(0xFFFFF0D8), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color(0xFFE6C98B)), modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Znaleziono propozycję wydatku", color = MouseInk, fontWeight = FontWeight.Bold)
                            Text(payment.merchant, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                             Text(money(payment.amount), color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(reject) { Text("Odrzuć") }; Button({ accept(payment) }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("Dodaj 60/40") } }
                        }
                    }
                }
            }
            item {
                Surface(color = MouseSurface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MouseLine), modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Historia wydatków", color = MouseInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Wszystkie okruszki, które myszy dopisały do norki", color = MouseMuted, fontSize = 12.sp)
                            }
                            TextButton({ filtersExpanded = !filtersExpanded }) { Text(if (filtersExpanded) "Zwiń" else "Filtry", color = accent, fontWeight = FontWeight.Bold) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = accent.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                                Text("${filteredExpenses.count { !isSettlement(it) }} wydatków", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(color = MouseSageSoft, shape = RoundedCornerShape(12.dp)) {
                                Text(money(filteredExpenses.filterNot(::isSettlement).sumOf { it.amount }), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = MouseSage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (filtersExpanded) {
                            OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Szukaj") }, placeholder = { Text("sklep, opis lub osoba") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                            Text("OKRES", fontWeight = FontWeight.Bold, color = MouseMuted, fontSize = 11.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                FilterChip(datePreset == "ALL", { datePreset = "ALL" }, { Text("Wszystkie") }); FilterChip(datePreset == "WEEK", { datePreset = "WEEK" }, { Text("7 dni") }); FilterChip(datePreset == "MONTH", { datePreset = "MONTH" }, { Text("Ten miesiąc") }); FilterChip(datePreset == "YEAR", { datePreset = "YEAR" }, { Text("Ten rok") }); FilterChip(datePreset == "CUSTOM", { datePreset = "CUSTOM" }, { Text("Własny zakres") })
                            }
                            Text("TYP WYDATKU", fontWeight = FontWeight.Bold, color = MouseMuted, fontSize = 11.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf("Jedzenie", "Spożywcze", "Dom", "Paliwo", "Zdrowie", "Transport", "Rozrywka", "Rachunki", "Inne").forEach { category -> FilterChip(selectedCategory == category, { selectedCategory = if (selectedCategory == category) null else category }, { Text(categoryIcon(category) + " " + category) }) }
                            }
                             OutlinedButton({ storePickerOpen = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(selectedStore ?: "Wszystkie sklepy", modifier = Modifier.weight(1f)); Text("Wybierz", color = accent) }
                        }
                    }
                }
            }
            if (filteredExpenses.isEmpty()) item { Text("W norce na razie cisza. 🐭", Modifier.padding(horizontal = 20.dp), color = MouseMuted) }
            groupedExpenses.forEach { (month, expenses) ->
                item { Row(Modifier.padding(horizontal = 20.dp, vertical = 2.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(formatExpenseMonth(month), color = MouseInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${money(expenses.filterNot(::isSettlement).sumOf { it.amount })} · ${expenses.count { !isSettlement(it) }}", color = MouseSage, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
                items(expenses, key = { it.id }, contentType = { "expense" }) { expense ->
                    ExpenseHistoryCard(
                        expense = expense,
                        accent = accent,
                        aniaAvatar = aniaAvatarResources[avatarIndex],
                        pawelUserId = pawelUserId,
                        members = members,
                        currentUserId = currentUserId,
                        onEdit = edit
                    )
                }
            }
        }
    }
    if (storePickerOpen) AlertDialog(onDismissRequest = { storePickerOpen = false }, title = { Text("Wybierz sklep") }, text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) { groceryStores.forEach { store -> TextButton(onClick = { selectedStore = store.name; storePickerOpen = false }, modifier = Modifier.fillMaxWidth()) { Image(painterResource(store.logo), null, Modifier.size(28.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(10.dp)); Text(store.name, modifier = Modifier.weight(1f)); if (selectedStore == store.name) Text("✓", color = MouseTerracotta) } } } }, confirmButton = { TextButton({ selectedStore = null; storePickerOpen = false }) { Text("Wszystkie") } })
    if (avatarDialog) AlertDialog(onDismissRequest = { avatarDialog = false }, title = { Text("Wybierz motyw swojej myszy") }, text = { Column(Modifier.heightIn(max = 430.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) { Text("Masz do wyboru 20 motywów. Wybór zapisuje się na Twoim profilu.", color = MouseMuted, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { mouseMotifResources.forEachIndexed { index, resource -> Surface(color = if (avatarIndex == index) MouseTerracottaSoft else MouseSurface, shape = CircleShape, border = if (avatarIndex == index) BorderStroke(2.dp, MouseTerracotta) else BorderStroke(1.dp, MouseLine), modifier = Modifier.size(66.dp).clickable { avatarIndex = index; prefs.edit().putInt("profile_avatar", index).putInt("ania_avatar", index).apply(); saveProfile(index, accentArgb); avatarDialog = false }) { Image(painterResource(resource), "Motyw myszy ${index + 1}", Modifier.padding(4.dp).clip(CircleShape), contentScale = ContentScale.Crop) } } } } }, confirmButton = { TextButton({ avatarDialog = false }) { Text("Gotowe") } })
    if (colorDialog) AlertDialog(onDismissRequest = { colorDialog = false }, title = { Text("Kolor mojego profilu") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Wybierz kolor swojej kępki. Zmiana od razu podświetli profil i przyciski.", color = MouseMuted, fontSize = 12.sp); FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { aniaPurplePalette.forEach { color -> Surface(color = color, shape = CircleShape, border = BorderStroke(3.dp, if (accentArgb == color.toArgb()) MouseInk else Color.Transparent), modifier = Modifier.size(50.dp).clickable { accentArgb = color.toArgb(); prefs.edit().putInt("ania_color", color.toArgb()).apply(); saveProfile(avatarIndex, color.toArgb()); Toast.makeText(context, "Kolor profilu zapisany", Toast.LENGTH_SHORT).show(); colorDialog = false }) {} } } } }, confirmButton = { TextButton({ colorDialog = false }) { Text("Zostaw") } })
    if (profileDialog) AlertDialog(
        onDismissRequest = { profileDialog = false },
        title = { Text("Mój profil") },
        text = {
            Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(currentName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(accountEmail.ifBlank { "konto wspólnej norki" }, color = MouseMuted, fontSize = 13.sp)
                Text("Wspólna norka działa dla dwóch osób.", color = MouseMuted)
                 TextButton({ profileDialog = false; avatarDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Zmień motyw myszy") }
                 TextButton({ profileDialog = false; colorDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Zmień kolor profilu") }
                // Notification reading is shown as a disabled beta placeholder.
                if (true) {
                    HorizontalDivider(color = MouseLine)
                    Text("Dodatkowe ustawienia / Beta", color = MouseTerracotta, fontWeight = FontWeight.Bold)
                    Text("Funkcje testowe są wyłączone domyślnie.", color = MouseMuted, fontSize = 12.sp)
                    Surface(
                        color = MouseSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MouseLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                             Text("Odczyt powiadomień — kolejny update", color = MouseMuted, fontWeight = FontWeight.SemiBold)
                            Text(
                                 "Ta funkcja jest chwilowo wyłączona. Najpierw dopracowujemy bezpieczne, ręczne dodawanie wydatków.",
                                color = MouseMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                              Text("Dostępne w kolejnym update", color = MouseMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ profileDialog = false }) { Text("Zamknij") } },
        dismissButton = { TextButton({ profileDialog = false; logout() }) { Text("Wyloguj", color = MouseTerracotta) } }
    )
}

@Composable
private fun ExpenseHistoryCardLegacy(expense: Expense, accent: Color, aniaAvatar: Int, pawelUserId: String?, onEdit: (Expense) -> Unit) {
    val byPawel = isPawelExpense(expense, pawelUserId)
    val settlement = isSettlement(expense)
    val settlementRed = Color(0xFFC23A4A)
    val personColor = if (byPawel) BearBlue else accent
    val cardColor = when {
        settlement -> Color(0xFFFFE8EA)
        byPawel -> BearBlueSoft
        else -> AniaPurpleSoft
    }
    val amountColor = if (settlement) settlementRed else MoneyGreen
    ElevatedCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable { onEdit(expense) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = cardColor)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(if (byPawel) R.drawable.misio_pawel else aniaAvatar), null, Modifier.size(42.dp).clip(CircleShape).background(if (settlement) Color(0xFFFFCDD2) else if (byPawel) BearBlueSoft else AniaPurpleSoft), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (settlement) "↔ Spłata rozliczenia" else expense.merchant, style = MaterialTheme.typography.titleSmall, color = if (settlement) settlementRed else Color.Unspecified, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                if (settlement) {
                    Text("${expense.payer} rozlicza wspólny bilans", color = settlementRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    groceryLogo(expense.category, expense.merchant)?.let { logo -> Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(logo), null, Modifier.size(20.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(4.dp)); Text("Sklep", color = personColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } } ?: Text(categoryIcon(expense.category) + " " + expense.category, color = personColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("${expense.payer}  •  data wpisania: ${formatExpenseDay(expense.createdAt)}", color = Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(expense.amount), color = amountColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Default.Edit, "Edytuj", tint = if (settlement) settlementRed else Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}
@Composable
private fun ExpenseHistoryCard(
    expense: Expense,
    accent: Color,
    aniaAvatar: Int,
    pawelUserId: String?,
    members: List<MemberRow> = emptyList(),
    currentUserId: String? = null,
    onEdit: (Expense) -> Unit
) {
    val byPawel = isPawelExpense(expense, pawelUserId)
    val settlement = isSettlement(expense)
    val payerMember = members.firstOrNull { it.userId == expense.payerId }
    val payerAvatar = payerMember?.avatarId
        ?.takeIf { it in mouseMotifResources.indices }
        ?.let(mouseMotifResources::get)
        ?: if (byPawel) R.drawable.mysza_clean_02 else aniaAvatar
    val isMine = currentUserId != null && expense.payerId == currentUserId
    val hasKnownPayer = expense.payerId != null || payerMember != null
    val ownerColor = when {
        settlement -> MouseLavender
        currentUserId == null -> if (byPawel) MouseSage else accent
        isMine -> MouseSage
        else -> MouseTerracotta
    }
    val cardColor = when {
        settlement -> MouseLavenderSoft.copy(alpha = .82f)
        currentUserId == null -> Color.Transparent
        isMine -> MouseSageSoft.copy(alpha = .42f)
        else -> MousePeach.copy(alpha = .34f)
    }
    val cardBorder = when {
        settlement -> MouseLavender.copy(alpha = .72f)
        currentUserId == null -> Color.Transparent
        isMine -> MouseSage.copy(alpha = .45f)
        else -> MouseTerracotta.copy(alpha = .38f)
    }
    val amountColor = if (settlement) MouseLavender else ownerColor
    Surface(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable { onEdit(expense) },
        color = cardColor,
        shape = RoundedCornerShape(18.dp),
        border = if (cardBorder == Color.Transparent) null else BorderStroke(1.dp, cardBorder)
    ) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp)) {
                Surface(
                    color = if (settlement) MouseLavenderSoft else if (isMine) MouseSageSoft else MousePeach,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp).align(Alignment.TopStart)
                ) {
                Box(contentAlignment = Alignment.Center) {
                    groceryLogo(expense.category, expense.merchant)?.let { logo -> Image(painterResource(logo), null, Modifier.size(28.dp), contentScale = ContentScale.Fit) }
                        ?: if (!settlement) Image(painterResource(if (byPawel) R.drawable.mysza_clean_02 else aniaAvatar), null, Modifier.size(38.dp), contentScale = ContentScale.Crop)
                        else Text("↔", color = MouseLavender, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
                if (!settlement && hasKnownPayer) {
                    Surface(
                        color = ownerColor,
                        shape = CircleShape,
                        border = BorderStroke(2.dp, MouseSurface),
                        modifier = Modifier.size(25.dp).align(Alignment.BottomEnd)
                    ) {
                        Image(
                            painterResource(payerAvatar),
                            "Wpisane przez ${expense.payer}",
                            Modifier.padding(2.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (settlement) "Wyrównanie bilansu" else expense.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        color = MouseInk,
                        fontWeight = if (settlement) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (settlement) {
                        Surface(color = MouseLavender, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "WYRÓWNANIE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (settlement) Text("${expense.payer} · myszy są bliżej zgody", color = MouseMuted, fontSize = 12.sp)
                else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Wpisane przez ${expense.payer}", color = ownerColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Surface(color = ownerColor.copy(alpha = .14f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                when {
                                    currentUserId == null -> "wspólna norka"
                                    isMine -> "moje"
                                    else -> "druga mysz"
                                },
                                color = ownerColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("${expense.category} · ${formatExpenseDate(expense.occurredAt)}", color = MouseMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(expense.amount), color = amountColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.Edit, "Edytuj", tint = MouseMuted, modifier = Modifier.size(15.dp))
            }
        }
    }
}

fun formatExpenseMonth(month: java.time.YearMonth): String = month.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL"))).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pl-PL")) else it.toString() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Add(initial: Expense? = null, members: List<MemberRow> = emptyList(), currentUserId: String? = null, pawelUserId: String? = null, cancel: () -> Unit, viewReceipt: ((Expense) -> Unit)? = null, previewReceipt: (() -> Unit)? = null, removeReceipt: ((Expense) -> Unit)? = null, deleteExpense: ((Expense) -> Unit)? = null, importPdf: (() -> Unit)? = null, resumeImport: (() -> Unit)? = null, hasImport: Boolean = false, save: (Expense) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val firstPersonName = members.firstOrNull { it.userId == pawelUserId }?.nickname ?: "Pierwsza osoba"
    val secondPersonName = members.firstOrNull { it.userId != pawelUserId }?.nickname ?: "Druga osoba"
    var merchant by remember(initial) { mutableStateOf(initial?.merchant.orEmpty()) }
    var amount by remember(initial) { mutableStateOf(initial?.amount?.toString()?.replace('.', ',').orEmpty()) }
    var payer by remember(initial) { mutableStateOf(if (initial == null || isPawel(initial.payer)) firstPersonName else secondPersonName) }
    var payerId by remember(initial, currentUserId) { mutableStateOf(initial?.payerId ?: currentUserId) }
    var split by remember(initial) { mutableIntStateOf(initial?.pawel ?: 60) }
    var category by remember(initial) { mutableStateOf(initial?.category ?: suggestedCategory("")) }
    var categoryPickedManually by remember(initial) { mutableStateOf(initial != null) }
    var comment by remember(initial) { mutableStateOf(initial?.comment ?: "") }
    var costDate by remember(initial) { mutableStateOf(initial?.occurredAt?.let(::parseExpenseDay) ?: LocalDate.now()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var receipt by remember(initial) { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { receipt = it }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) receipt = cameraUri }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) { cameraUri = newPhoto(context); camera.launch(cameraUri!!) } }
    fun openCamera() { if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { cameraUri = newPhoto(context); camera.launch(cameraUri!!) } else permission.launch(Manifest.permission.CAMERA) }
    Scaffold(containerColor = MouseCream, topBar = { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MouseCream, titleContentColor = MouseInk, navigationIconContentColor = MouseTerracotta), title = { Text(if (initial == null) "Nowy okruszek" else "Edytuj wydatek", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(cancel) { Text("Anuluj") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (initial == null && (importPdf != null || resumeImport != null)) item {
                Surface(color = MouseSageSoft, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MouseSage.copy(alpha = .28f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Masz wyciąg z banku?", color = MouseSage, fontWeight = FontWeight.Bold)
                        Text("Myszy mogą wyłapać płatności z PDF-a, a Ty zatwierdzisz je pojedynczo.", color = MouseMuted, fontSize = 12.sp)
                        OutlinedButton(onClick = { if (hasImport) resumeImport?.invoke() else importPdf?.invoke() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MouseSage.copy(alpha = .55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MouseSage)) {
                            Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(7.dp)); Text(if (hasImport) "Wznów import PDF" else "Importuj PDF z banku", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { OutlinedTextField(amount, { amount = it }, label = { Text("Kwota") }, suffix = { Text("z\u0142") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MoneyGreen)) }
            item { OutlinedTextField(merchant, { value -> merchant = value; if (!categoryPickedManually) category = suggestedCategory(value) }, label = { Text("Na co poszło?") }, placeholder = { Text("np. zakupy, paliwo, restauracja") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) }
            item { Text("Kategoria", fontWeight = FontWeight.Bold); FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { expenseCategories.forEach { value -> if (value == "Spo\u017cywcze") FilterChip(category == value, { category = value; categoryPickedManually = true }, { Text(value) }, leadingIcon = { Image(painterResource(R.drawable.spozywcze), null, Modifier.size(24.dp), contentScale = ContentScale.Fit) }) else FilterChip(category == value, { category = value; categoryPickedManually = true }, { Text(categoryIcon(value) + " " + value) }) } } }
            if (category == "Spo\u017cywcze") item {
                Text("Wybierz sklep", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    groceryStores.forEach { store ->
                        FilterChip(
                            selected = merchant.equals(store.name, ignoreCase = true),
                            onClick = { merchant = store.name },
                            label = { Text(store.name) },
                            leadingIcon = { Image(painterResource(store.logo), null, Modifier.size(28.dp), contentScale = ContentScale.Fit) }
                        )
                    }
                }
            }
            item {
                OutlinedButton({
                    DatePickerDialog(context, { _, year, month, day -> costDate = LocalDate.of(year, month + 1, day) }, costDate.year, costDate.monthValue - 1, costDate.dayOfMonth).show()
                }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.CalendarToday, null)
                    Spacer(Modifier.width(10.dp))
                    Column { Text("Data zap\u0142acenia", fontWeight = FontWeight.Bold); Text(costDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")), fontSize = 13.sp) }
                }
            }
            item {
                Text("Kto zap\u0142aci\u0142?", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (members.isNotEmpty() && pawelUserId != null) {
                        members.forEach { member ->
                            val label = member.nickname
                            FilterChip(payerId == member.userId, { payerId = member.userId; payer = label }, { Text(label) })
                        }
                    } else {
                        listOf(firstPersonName, secondPersonName).forEach { label -> FilterChip(payer == label, { payer = label }, { Text(label) }) }
                    }
                }
            }
            item { Text("Podział", fontWeight = FontWeight.Bold); Text("$firstPersonName $split%  |  $secondPersonName ${100 - split}%", color = BearBlue, style = MaterialTheme.typography.titleLarge); Slider(split.toFloat(), { split = it.toInt() }, valueRange = 0f..100f, steps = 19); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(0, 50, 60, 100).forEach { value -> FilterChip(split == value, { split = value }, { Text("$value%") }) } } }
            item {
                Text("Paragon", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ filePicker.launch("image/*") }, Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Text(" Z pliku") }
                    Button({ openCamera() }, Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Text(" Aparat") }
                }
                if (BuildConfig.IS_DEV && initial == null) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton({ receipt = Uri.parse(DEMO_RECEIPT_PATH) }, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MouseTerracotta.copy(alpha = .55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = MouseTerracotta)) {
                        Text("Dodaj fikcyjny paragon do demonstracji")
                    }
                }
                if (receipt != null) {
                    AssistChip({ receipt = null }, { Text(if (receipt.toString() == DEMO_RECEIPT_PATH) "Fikcyjny paragon dodany" else "Nowe zdjęcie dodane") })
                    if (receipt.toString() == DEMO_RECEIPT_PATH) TextButton({ previewReceipt?.invoke() }) { Text("Podejrzyj fikcyjny paragon", color = MouseTerracotta) }
                }
                if (initial?.receipt != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ viewReceipt?.invoke(initial) }, Modifier.weight(1f)) { Text("Podejrzyj paragon") }
                        OutlinedButton({ removeReceipt?.invoke(initial) }, Modifier.weight(1f)) { Text("Usuń paragon") }
                    }
                }
            }
            item { OutlinedTextField(comment, { comment = it }, label = { Text("Komentarz (opcjonalnie)") }, placeholder = { Text("np. dla domu lub na wyjazd") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(10.dp)) }
            item { val number = amount.replace(',', '.').toDoubleOrNull() ?: 0.0; Card(colors = CardDefaults.cardColors(containerColor = BearBlueSoft), shape = RoundedCornerShape(16.dp)) { Text("$firstPersonName: ${money(number * split / 100)}   •   $secondPersonName: ${money(number * (100 - split) / 100)}", Modifier.padding(16.dp), fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(10.dp)); Button({ val exactShares = exactSharesAfterEdit(initial, number, split); save(Expense(initial?.id ?: UUID.randomUUID().toString(), merchant.ifBlank { "Wydatek" }, number, split, payer, receipt?.toString() ?: initial?.receipt, costDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString(), category, initial?.createdAt ?: java.time.Instant.now().toString(), comment, exactShares.first, exactShares.second, payerId)) }, Modifier.fillMaxWidth().height(54.dp), enabled = number > 0, shape = RoundedCornerShape(16.dp)) { Text(if (initial == null) "Zapisz okruszek" else "Zapisz zmiany", fontWeight = FontWeight.Bold) } }
            if (initial != null && deleteExpense != null) item { OutlinedButton({ confirmDelete = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Usuń cały wydatek") } }
        }
    }
    if (confirmDelete && initial != null) AlertDialog(onDismissRequest={ confirmDelete=false }, title={ Text("Usunąć wydatek?") }, text={ Text("Rekord, paragon i jego wpływ na bilans zostaną usunięte.") }, confirmButton={ Button({ confirmDelete=false; deleteExpense?.invoke(initial) }) { Text("Usuń") } }, dismissButton={ TextButton({ confirmDelete=false }) { Text("Anuluj") } })
}

@Composable
fun Celebration(character: String, finished: () -> Unit) {
    LaunchedEffect(character) { delay(1800); finished() }
    val motion = rememberInfiniteTransition(label = "character")
    val scale by motion.animateFloat(.88f, 1.08f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "bounce")
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable { finished() }, contentAlignment = Alignment.Center) {
        ElevatedCard(shape = RoundedCornerShape(28.dp)) { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(if (isPawel(character)) R.drawable.misio_pawel else R.drawable.mysza_ania), null, Modifier.size(190.dp).graphicsLayer { scaleX = scale; scaleY = scale }, contentScale = ContentScale.Fit)
            Text(if (isPawel(character)) "Misio ogarnia rachunki!" else "Mysza ogarnia rachunki!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        } }
    }

}

fun newPhoto(context: Context): Uri { val directory = File(context.filesDir, "receipts").apply { mkdirs() }; return FileProvider.getUriForFile(context, "${context.packageName}.files", File(directory, "receipt-${System.currentTimeMillis()}.jpg")) }
fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pl-PL")).format(value)
fun isPawel(payer: String): Boolean { val normalized = payer.lowercase(Locale.forLanguageTag("pl-PL")); return normalized.contains("myszo") || normalized.contains("pawe") || normalized.contains("misio") }
fun isPawelExpense(expense: Expense, pawelUserId: String?): Boolean =
    expense.payerId?.let { it == pawelUserId } ?: isPawel(expense.payer)
fun exactSharesAfterEdit(initial: Expense?, amount: Double, pawelPercent: Int): Pair<Double?, Double?> =
    if (initial != null && amount == initial.amount && pawelPercent == initial.pawel) {
        initial.pawelShare to initial.aniaShare
    } else {
        null to null
    }
fun notificationAccess(context: Context): Boolean = if (android.os.Build.VERSION.SDK_INT >= 27) {
    context.getSystemService(android.app.NotificationManager::class.java).isNotificationListenerAccessGranted(android.content.ComponentName(context, AliorNotificationListener::class.java))
} else {
    (Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: "").contains(context.packageName)
}
fun parseExpenseDay(value: String): LocalDate = runCatching { java.time.OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrElse { LocalDate.now() }
fun formatExpenseDay(value: String): String = parseExpenseDay(value).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
fun parseFilterDate(value: String): LocalDate? = runCatching { if (value.isBlank()) null else LocalDate.parse(value) }.getOrNull()
fun formatFilterDate(value: String): String = parseFilterDate(value)?.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "wybierz"
fun matchesDateFilter(value: String, preset: String, from: String = "", to: String = ""): Boolean {
    val date = parseExpenseDay(value)
    val today = LocalDate.now()
    return when (preset) {
        "MONTH" -> date.year == today.year && date.month == today.month
        "YEAR" -> date.year == today.year
        "WEEK" -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today)
        "CUSTOM" -> {
            val start = parseFilterDate(from)
            val end = parseFilterDate(to)
            (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
        }
        else -> true
    }
}
fun formatExpenseDate(value: String): String = runCatching {
    java.time.OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm"))
}.getOrElse { value }

fun isSettlement(expense: Expense): Boolean {
    val label = expense.merchant.trim().lowercase(Locale.forLanguageTag("pl-PL"))
    return label.startsWith("spłata rozliczenia") ||
        label.startsWith("splata rozliczenia") ||
        label.startsWith("uregulowanie długu") ||
        label.startsWith("uregulowanie dlugu")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementHistoryScreen(
    settlements: List<Expense>,
    currentName: String,
    partnerName: String,
    close: () -> Unit,
    edit: (Expense) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var datePreset by rememberSaveable { mutableStateOf("ALL") }
    var rangeStart by rememberSaveable { mutableStateOf("") }
    var rangeEnd by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var storePickerOpen by rememberSaveable { mutableStateOf(false) }
    val sorted = remember(settlements, datePreset, rangeStart, rangeEnd) { settlements.filter { matchesDateFilter(it.occurredAt, datePreset, rangeStart, rangeEnd) }.sortedByDescending { it.occurredAt } }
    val grouped = remember(sorted) { sorted.groupBy { java.time.YearMonth.from(parseExpenseDay(it.occurredAt)) }.toList().sortedByDescending { it.first } }
    val total = sorted.sumOf { it.amount }
    Scaffold(
        containerColor = MouseCream,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MouseCream, titleContentColor = MouseInk),
                title = { Column { Text("Wyrównania", fontWeight = FontWeight.Bold); Text("Kiedy myszy podały sobie łapkę", color = MouseMuted, fontSize = 12.sp) } },
                navigationIcon = { TextButton(close) { Text("Wróć", color = MouseTerracotta) } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Surface(color = MouseSageSoft, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, MouseSage.copy(alpha = .25f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Spokojna norka", color = MouseSage, fontWeight = FontWeight.Bold); Text("${sorted.size} zapisów", color = MouseMuted, fontSize = 13.sp) }
                        Text(money(total), color = MouseSage, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            item {
                Surface(color = MouseSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MouseLine), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pokaż wyrównania", color = MouseInk, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                            FilterChip(datePreset == "ALL", { datePreset = "ALL" }, { Text("Wszystkie") })
                            FilterChip(datePreset == "MONTH", { datePreset = "MONTH" }, { Text("Ten miesiąc") })
                            FilterChip(datePreset == "YEAR", { datePreset = "YEAR" }, { Text("Ten rok") })
                            FilterChip(datePreset == "WEEK", { datePreset = "WEEK" }, { Text("7 dni") })
                            FilterChip(datePreset == "CUSTOM", { datePreset = "CUSTOM" }, { Text("Zakres") })
                        }
                        if (datePreset == "CUSTOM") Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedButton({ val initial = parseFilterDate(rangeStart) ?: LocalDate.now(); DatePickerDialog(context, { _, y, m, d -> rangeStart = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show() }, Modifier.weight(1f)) { Text("Od: ${formatFilterDate(rangeStart)}") }
                            OutlinedButton({ val initial = parseFilterDate(rangeEnd) ?: LocalDate.now(); DatePickerDialog(context, { _, y, m, d -> rangeEnd = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show() }, Modifier.weight(1f)) { Text("Do: ${formatFilterDate(rangeEnd)}") }
                        }
                    }
                }
            }
            if (sorted.isEmpty()) item { Text("W norce nie ma jeszcze zapisanych wyrównań.", color = MouseMuted, modifier = Modifier.padding(vertical = 12.dp)) }
            grouped.forEach { (month, entries) ->
                item { Text(formatExpenseMonth(month), color = MouseInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
                items(entries, key = { it.id }, contentType = { "settlement" }) { payment ->
                    val byPawel = isPawel(payment.payer)
                    Surface(Modifier.fillMaxWidth().clickable { edit(payment) }, color = MouseLavenderSoft.copy(alpha = .78f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.5.dp, MouseLavender.copy(alpha = .55f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MouseLavenderSoft, shape = CircleShape, modifier = Modifier.size(48.dp)) { Image(painterResource(if (byPawel) R.drawable.misio_pawel else R.drawable.mysza_ania), null, Modifier.padding(3.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                val payerName = payment.payer.ifBlank { currentName }
                                val receiverName = if (payerName.equals(currentName, ignoreCase = true)) partnerName else currentName
                                Text("$payerName spłaca $receiverName", color = MouseInk, fontWeight = FontWeight.Bold, maxLines = 1)
                                Surface(color = MouseLavender, shape = RoundedCornerShape(7.dp)) { Text("WYRÓWNANIE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) }
                                Text(formatExpenseDate(payment.occurredAt), color = MouseMuted, fontSize = 12.sp, maxLines = 1)
                                if (payment.comment.isNotBlank()) Text(payment.comment, color = MouseMuted, fontSize = 12.sp, maxLines = 1)
                            }
                            Column(horizontalAlignment = Alignment.End) { Text(money(payment.amount), color = MouseLavender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Text("Edytuj", color = MouseMuted, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(user: String, isPawelUser: Boolean, cancel: () -> Unit, save: (Expense) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val number = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    Scaffold(containerColor = MouseCream, topBar = { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MouseCream, titleContentColor = MouseInk), title = { Text("Podaj łapkę", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(cancel) { Text("Anuluj", color = MouseTerracotta) } }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = MouseSageSoft, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("Spokojne wyrównanie", color = MouseSage, fontWeight = FontWeight.Bold); Text("Wpisz kwotę, którą właśnie oddajesz drugiej myszy. Bilans norki zmieni się po zapisaniu.", color = MouseInk, fontSize = 15.sp) } }
            Text("Ile wraca do wspólnej norki?", color = MouseInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(amount, { amount = it }, label = { Text("Kwota wyrównania") }, suffix = { Text("zł") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MouseTerracotta))
            if (number > 0) Text("Myszy zapiszą: ${money(number)}", color = MouseSage, fontWeight = FontWeight.Bold)
            Button({ save(Expense(UUID.randomUUID().toString(), "Spłata rozliczenia", number, if (isPawelUser) 100 else 0, user, null)) }, enabled = number > 0, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = MouseTerracotta), shape = RoundedCornerShape(16.dp)) { Text("Zapisz spokojne wyrównanie", fontWeight = FontWeight.Bold) }
        }
    }
}

fun calculatePawelBalance(expenses: List<Expense>, correction: Double = 0.0, pawelUserId: String? = null): Double = correction + expenses.sumOf { expense ->
    if (isSettlement(expense)) {
        if (isPawelExpense(expense, pawelUserId)) expense.amount else -expense.amount
    } else {
        val pawelShare = expense.pawelShare ?: expense.amount * expense.pawel / 100.0
        val aniaShare = expense.aniaShare ?: expense.amount - pawelShare
        if (isPawelExpense(expense, pawelUserId)) aniaShare else -pawelShare
    }
}

data class GroceryStore(val name: String, val logo: Int)
val groceryStores = listOf(
    GroceryStore("Biedronka", R.drawable.sklep_biedronka), GroceryStore("Lidl", R.drawable.sklep_lidl), GroceryStore("Stokrotka", R.drawable.sklep_stokrotka),
    GroceryStore("Carrefour", R.drawable.sklep_carrefour), GroceryStore("Netto", R.drawable.sklep_netto), GroceryStore("Pepco", R.drawable.sklep_pepco),
    GroceryStore("Rossmann", R.drawable.sklep_rossmann), GroceryStore("Action", R.drawable.sklep_action), GroceryStore("TEDi", R.drawable.sklep_tedi),
    GroceryStore("Woolworth", R.drawable.sklep_woolworth), GroceryStore("Żabka", R.drawable.sklep_zabka), GroceryStore("Orlen", R.drawable.sklep_orlen),
    GroceryStore("Shell", R.drawable.sklep_shell), GroceryStore("Moya", R.drawable.sklep_moya), GroceryStore("Circle K", R.drawable.sklep_circlek),
    GroceryStore("McDonald's", R.drawable.sklep_mcdonalds), GroceryStore("Decathlon", R.drawable.sklep_decathlon), GroceryStore("Pizza Hut", R.drawable.sklep_pizzahut),
    GroceryStore("Allegro", R.drawable.sklep_allegro), GroceryStore("BM King Kebab", R.drawable.sklep_bmking)
)
fun groceryLogo(category: String, merchant: String): Int? = groceryStores.firstOrNull { merchant.contains(it.name, ignoreCase = true) || (it.name == "Żabka" && merchant.contains("zabka", ignoreCase = true)) }?.logo ?: if (category == "Spożywcze") R.drawable.spozywcze else null
val expenseCategories = listOf("Jedzenie", "Spo\u017cywcze", "Dom", "Paliwo", "Zdrowie", "Transport", "Rozrywka", "Rachunki", "Inne")
fun suggestedCategory(merchant: String): String {
    val text = merchant.lowercase(Locale.forLanguageTag("pl-PL"))
    return when {
        text.containsAny("biedronka", "lidl", "stokrotka", "żabka", "zabka", "carrefour", "netto", "kaufland", "auchan", "aldi", "selgros") -> "Spożywcze"
        text.containsAny("orlen", "shell", "moya", "circle k", "bp ", "paliwo", "stacja") -> "Paliwo"
        text.containsAny("mcdonald", "kebab", "pizza", "restaur", "bar ", "glovo", "uber eats", "smartlunch", "kfc") -> "Jedzenie"
        text.containsAny("apteka", "lekar", "dent", "psychiatr", "okul", "medic", "zdrow") -> "Zdrowie"
        text.containsAny("netia", "internet", "czynsz", "prąd", "prad", "gaz", "woda", "netflix", "spotify", "rachunek") -> "Rachunki"
        text.containsAny("uber", "bolt", "taxi", "pkp", "parking", "bilet", "transport") -> "Transport"
        text.containsAny("ikea", "jysk", "castorama", "leroy", "obi", "dom") -> "Dom"
        text.containsAny("action", "pepco", "rossmann", "tedi", "woolworth", "allegro", "decathlon", "zakupy") -> "Zakupy"
        else -> "Inne"
    }
}
private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)fun categoryIcon(category: String): String = when(category) {
    "Jedzenie" -> "🍽"; "Dom" -> "🏠"; "Paliwo" -> "⛽"; "Zdrowie" -> "❤"; "Transport" -> "🚋"; "Rozrywka" -> "🎬"; "Rachunki" -> "🧾"; else -> "●"
}
