package pl.razem.myszy

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun CloudRazemApp(
    openExpenseId: String? = null,
    consumedOpenExpense: () -> Unit = {},
    acceptPending: Boolean = false,
    consumedAcceptPending: () -> Unit = {},
    authRefreshToken: Int = 0,
    passwordRecovery: Boolean = false,
    consumedPasswordRecovery: () -> Unit = {},
    externalAuthError: String? = null,
    consumedExternalAuthError: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SupabaseRepository(context) }
    val localStore = remember { Store(context) }
    val bankStore = remember { BankImportStore(context) }
    val scope = rememberCoroutineScope()
    var authReady by remember { mutableStateOf(false) }
    var authAttempt by remember { mutableIntStateOf(0) }
    var signedIn by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(false) }
    var member by remember { mutableStateOf<MemberRow?>(null) }
    var membershipLoadFailed by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var balanceCorrection by remember { mutableDoubleStateOf(0.0) }
    var pending by remember { mutableStateOf(localStore.pending()) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var celebration by remember { mutableStateOf<String?>(null) }
    var safeMode by remember { mutableStateOf(localStore.safeMode()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var authNotice by remember { mutableStateOf<String?>(null) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var importQueue by remember { mutableStateOf(bankStore.load()) }
    var reviewingImport by remember { mutableStateOf(false) }
    var importDraft by remember { mutableStateOf<Expense?>(null) }
    var settling by remember { mutableStateOf(false) }
    var showingSettlementHistory by remember { mutableStateOf(false) }
    var seenExpenseIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var syncInitialized by remember { mutableStateOf(false) }
    var lastActivity by remember { mutableStateOf<Pair<String, Expense>?>(null) }
    var householdMembers by remember { mutableStateOf<List<MemberRow>>(emptyList()) }
    var pawelUserId by remember { mutableStateOf<String?>(null) }
    var notificationStart by remember { mutableStateOf(java.time.Instant.now()) }

    fun clearLocalSession() {
        localStore.clearSensitiveSessionState()
        bankStore.clearForLogout()
        pending = null
        importQueue = emptyList()
        safeMode = true
        seenExpenseIds = emptySet()
        syncInitialized = false
        notificationStart = java.time.Instant.now()
    }

    suspend fun logout() {
        try {
            repo.signOut()
        } finally {
            clearLocalSession()
            signedIn = false
            member = null
        }
    }

    suspend fun refresh(showLoading: Boolean = true) {
        if (showLoading) loading = true
        val loadedMember = try {
            repo.membership()
        } catch (failure: Throwable) {
            // A temporary API timeout must never look like an unpaired account.
            android.util.Log.e("MyszySync", "Nie udało się pobrać członkostwa Domu", failure)
            membershipLoadFailed = true
            error = "Nie udało się teraz połączyć z Domem. Spróbujemy ponownie automatycznie."
            if (showLoading) loading = false
            return
        }
        membershipLoadFailed = false
        member = loadedMember
        member?.let { m ->
            localStore.setNotificationIdentity(m.nickname)
            runCatching { repo.registerFcmToken(m.householdId) }
                .onFailure { android.util.Log.e("MyszyFcm", "Nie udało się zarejestrować tokenu FCM", it) }
            householdMembers = repo.members(m.householdId)
            pawelUserId = repo.household(m.householdId)?.createdBy
            if (m.userId != pawelUserId) { safeMode = true; localStore.setSafeMode(true) }
            val nicknames = householdMembers.associate { it.userId to it.nickname }
            val cloud = repo.expenses(m.householdId)
            balanceCorrection = repo.household(m.householdId)?.balanceCorrection ?: 0.0
            val activityRows = runCatching { repo.activities(m.householdId) }.getOrDefault(emptyList())
            val mapped = cloud.map { Expense(it.id, it.merchant, it.amount, it.pawelPercent, nicknames[it.payerId] ?: "Nieznana osoba", it.receiptPath, it.occurredAt, it.category, it.createdAt, it.comment, it.pawelShare, it.aniaShare, it.payerId) }
                .sortedByDescending { it.occurredAt }
            val myId = repo.currentUser()?.id
            if (syncInitialized && localStore.expenseNotificationsReady()) {
                cloud.filter { incoming ->
                    incoming.id !in seenExpenseIds && incoming.createdBy != myId &&
                        runCatching { java.time.Instant.parse(incoming.createdAt).isAfter(notificationStart) }.getOrDefault(false)
                }.forEach { incoming ->
                    mapped.firstOrNull { it.id == incoming.id }?.let { ExpenseNotifications.show(context, it) }
                }
            }
            // Nie wymuszaj przebudowy LazyColumn, gdy odpowiedź z chmury nie zmieniła danych.
            // Przy historii Settle Up to oszczędza setki kart przy każdym odświeżeniu.

            if (items != mapped) items = mapped
            activityRows.firstOrNull()?.let { row -> mapped.firstOrNull { it.id == row.expenseId }?.let { lastActivity = (row.action to it) } }
            seenExpenseIds = cloud.map { it.id }.toSet()
            if (!syncInitialized) localStore.setExpenseNotificationsReady(true)
            syncInitialized = true
        }
        if (showLoading) loading = false
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            loading = true; error = null
            runCatching { BankStatementParser.parse(context, uri).filterNot { bankStore.wasAdded(it.id) } }
                .onSuccess { importQueue = it; bankStore.start(it); reviewingImport = it.isNotEmpty(); if (it.isEmpty()) error = "Nie znaleziono nowych płatności kartą ani BLIK"; loading = false }
                .onFailure { error = "Nie udało się odczytać pliku: ${it.message}"; loading = false }
        }
    }

    LaunchedEffect(authAttempt, authRefreshToken) {
        authReady = false
        val status = withTimeoutOrNull(8_000) { supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing } }
        signedIn = status is SessionStatus.Authenticated
        if (status == null) error = "Sprawdzanie zapisanej sesji trwa zbyt długo. Spróbuj ponownie."
        authReady = true
    }
    LaunchedEffect(externalAuthError) {
        if (externalAuthError != null) {
            error = externalAuthError
            consumedExternalAuthError()
        }
    }
    LaunchedEffect(signedIn, authReady) { if (signedIn && authReady) runCatching { refresh() }.onFailure { error = it.message; loading = false } }
    LaunchedEffect(signedIn, authReady) {
        if (!signedIn || !authReady) return@LaunchedEffect
        while (isActive) {
            delay(8_000)
            runCatching { refresh(showLoading = false) }
        }
    }
    LaunchedEffect(items, openExpenseId) { if (openExpenseId != null && items.isNotEmpty()) { items.firstOrNull { it.id == openExpenseId }?.let { editing = it }; consumedOpenExpense() } }
    LaunchedEffect(acceptPending, member) {
        val payment = localStore.pending()
        if (acceptPending && member != null && payment != null) {
            loading = true
            val expense = Expense(java.util.UUID.randomUUID().toString(), payment.merchant, payment.amount, 60, member!!.nickname, null, category = suggestedCategory(payment.merchant), payerId = member!!.userId)
            runCatching { repo.addExpense(member!!.householdId, expense, "alior_notification"); localStore.clear(); pending = null; ExpenseNotifications.show(context, expense); refresh() }.onFailure { error = it.message; loading = false }.also { consumedAcceptPending() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            !authReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !signedIn -> CloudLogin(
                loading = loading,
                error = error,
                notice = authNotice,
                retrySession = { authAttempt++ },
                clearFeedback = { error = null; authNotice = null },
                signIn = { email, password ->
                    scope.launch {
                        loading = true
                        error = null
                        authNotice = null
                        runCatching { repo.signIn(email, password) }
                            .onSuccess {
                                loading = false
                                showWelcome = true
                                signedIn = true
                            }
                            .onFailure {
                                error = "Nie udało się zalogować. Sprawdź e-mail, hasło i połączenie z internetem."
                                loading = false
                            }
                    }
                },
                signUp = { email, password, repeatedPassword ->
                    validateRegistration(email, password, repeatedPassword)?.let {
                        error = it
                        return@CloudLogin
                    }
                    scope.launch {
                        loading = true
                        error = null
                        authNotice = null
                        runCatching { repo.signUp(email, password) }
                            .onSuccess { signedInImmediately ->
                                loading = false
                                if (signedInImmediately) {
                                    signedIn = true
                                    showWelcome = true
                                } else {
                                    authNotice = "Konto utworzone. Otwórz wiadomość wysłaną na e-mail i potwierdź rejestrację."
                                }
                            }
                            .onFailure {
                                error = "Nie udało się utworzyć konta. Sprawdź dane lub spróbuj ponownie później."
                                loading = false
                            }
                    }
                },
                resetPassword = { email ->
                    scope.launch {
                        loading = true
                        error = null
                        authNotice = null
                        runCatching { repo.requestPasswordReset(email) }
                            .onSuccess {
                                authNotice = "Jeśli konto istnieje, wysłaliśmy wiadomość do ustawienia nowego hasła."
                                loading = false
                            }
                            .onFailure {
                                error = "Nie udało się wysłać wiadomości. Sprawdź połączenie i spróbuj ponownie."
                                loading = false
                            }
                    }
                }
            )
            passwordRecovery -> PasswordRecoveryScreen(
                loading = loading,
                error = error,
                save = { password, repeatedPassword ->
                    validateNewPassword(password, repeatedPassword)?.let {
                        error = it
                        return@PasswordRecoveryScreen
                    }
                    scope.launch {
                        loading = true
                        error = null
                        runCatching { repo.updatePassword(password) }
                            .onSuccess {
                                loading = false
                                authNotice = "Hasło zostało zmienione."
                                consumedPasswordRecovery()
                            }
                            .onFailure {
                                error = "Nie udało się zmienić hasła. Poproś o nowy link."
                                loading = false
                            }
                    }
                },
                cancel = {
                    scope.launch {
                        logout()
                        consumedPasswordRecovery()
                    }
                }
            )
            showWelcome -> WelcomeMyszy { showWelcome = false }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            inviteCode != null -> InviteCodeScreen(inviteCode!!) { scope.launch { inviteCode = null; refresh() } }
            membershipLoadFailed -> PairingUnavailable(error, retry = { scope.launch { refresh() } }, onLogout = { scope.launch { logout() } })
            member == null -> HouseholdSetup(
                invite = inviteCode,
                error = error,
                onCreate = { nickname ->
                    scope.launch {
                        loading = true
                        error = null
                        runCatching { repo.createHome(nickname) }
                            .onSuccess { created ->
                                inviteCode = created.inviteCode
                                loading = false
                            }
                            .onFailure {
                                error = it.message ?: "Nie udało się utworzyć Domu."
                                loading = false
                            }
                    }
                },
                onJoin = { code, nickname ->
                    scope.launch {
                        loading = true
                        error = null
                        runCatching {
                            repo.joinHome(code, nickname)
                            refresh()
                        }.onFailure {
                            error = it.message ?: "Nie udało się dołączyć do Domu."
                            loading = false
                        }
                    }
                },
                onLogout = { scope.launch { logout() } }
            )
            importDraft != null -> Add(initial=importDraft, members=householdMembers, currentUserId=member!!.userId, pawelUserId=pawelUserId, cancel={ importDraft=null; reviewingImport=true }) { expense -> scope.launch {
                val tx=importQueue.first(); loading=true
                runCatching { repo.addExpense(member!!.householdId,expense); bankStore.markAdded(tx.id); ExpenseNotifications.show(context,expense); importQueue=importQueue.drop(1); bankStore.save(importQueue); importDraft=null; reviewingImport=importQueue.isNotEmpty(); refresh() }.onFailure { error=it.message; loading=false }
            } }
            reviewingImport && importQueue.isNotEmpty() -> BankImportReview(importQueue.first(), bankStore.total() - importQueue.size + 1, bankStore.total(),
                yes = { scope.launch { val tx=importQueue.first(); loading=true; val expense=Expense(java.util.UUID.randomUUID().toString(),tx.description,tx.amount,60,member!!.nickname,null, category = suggestedCategory(tx.description), payerId = member!!.userId); runCatching { repo.addExpense(member!!.householdId,expense); bankStore.markAdded(tx.id); ExpenseNotifications.show(context,expense); importQueue=importQueue.drop(1); bankStore.save(importQueue); if(importQueue.isEmpty()) { reviewingImport=false; bankStore.clear() }; refresh() }.onFailure { error=it.message; loading=false } } },
                no = { importQueue=importQueue.drop(1); bankStore.save(importQueue); if(importQueue.isEmpty()) { reviewingImport=false; bankStore.clear() } },
                modify = { val tx=importQueue.first(); importDraft=Expense(java.util.UUID.randomUUID().toString(),tx.description,tx.amount,60,member!!.nickname,null, category = suggestedCategory(tx.description), payerId = member!!.userId); reviewingImport=false },
                stop = { reviewingImport=false })
            settling -> SettlementScreen(member!!.nickname, member!!.userId == pawelUserId, { settling=false }) { expense -> scope.launch { loading=true; runCatching { repo.addExpense(member!!.householdId,expense.copy(payerId = member!!.userId)); settling=false; refresh() }.onFailure { error=it.message; loading=false } } }
            showingSettlementHistory -> SettlementHistoryScreen(items.filter(::isSettlement), { showingSettlementHistory = false }, { editing = it; showingSettlementHistory = false })
            adding || editing != null -> Add(
                initial = editing,
                members = householdMembers,
                currentUserId = member!!.userId,
                pawelUserId = pawelUserId,
                cancel = { adding = false; editing = null },
                viewReceipt = { expense ->
                    scope.launch {
                        runCatching { repo.receiptUrl(expense.receipt!!) }
                            .onSuccess { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    }
                },
                removeReceipt = { expense ->
                    scope.launch {
                        loading = true
                        runCatching {
                            repo.removeReceipt(member!!.householdId, expense.id, expense.receipt!!)
                            editing = expense.copy(receipt = null)
                            refresh()
                        }.onFailure { error = it.message; loading = false }
                    }
                },
                deleteExpense = { expense ->
                    scope.launch {
                        loading = true
                        runCatching {
                            repo.deleteExpense(member!!.householdId, expense)
                            editing = null
                            refresh()
                        }.onFailure { error = it.message; loading = false; android.util.Log.e("MyszyDelete", "Nie udało się usunąć wydatku", it); android.widget.Toast.makeText(context, "Błąd usuwania: ${it.message}", android.widget.Toast.LENGTH_LONG).show() }
                    }
                },
                save = { expense ->
                    scope.launch {
                        loading = true
                        val wasEditing = editing != null
                        runCatching {
                            if (wasEditing) { repo.updateExpense(member!!.householdId, expense, editing?.receipt); lastActivity = "edytował/a" to expense } else { repo.addExpense(member!!.householdId, expense); lastActivity = "dodał/a" to expense }

                            refresh()
                            adding = false
                            editing = null
                            if (!wasEditing) {
                                celebration = expense.payer
                                ExpenseNotifications.show(context, expense)
                            }
                        }.onFailure { error = it.message; loading = false }
                    }
                }
            )            else -> Home(user=member!!.nickname,isPawelUser=member!!.userId == pawelUserId,pawelUserId=pawelUserId,xs=items,balanceCorrection=balanceCorrection,pending=pending,safeMode=safeMode,
                setSafeMode={ safeMode=it; localStore.setSafeMode(it) }, add={ adding=true }, edit={ editing=it },
                viewReceipt={ expense -> scope.launch { runCatching { repo.receiptUrl(expense.receipt!!) }.onSuccess { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(it))) }.onFailure { error=it.message } } },
                removeReceipt={ expense -> scope.launch { loading=true; runCatching { repo.removeReceipt(member!!.householdId,expense.id,expense.receipt!!); refresh() }.onFailure { error=it.message; loading=false } } },
                importBank={ importLauncher.launch(arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel")) }, resumeImport={ reviewingImport=true }, hasImport=importQueue.isNotEmpty(), canReadNotifications=member!!.userId == pawelUserId, settle={ settling=true }, settlementHistory={ showingSettlementHistory = true },
                lastActivity=lastActivity,
                accept={ payment -> scope.launch { loading=true; val expense=Expense(java.util.UUID.randomUUID().toString(),payment.merchant,payment.amount,60,member!!.nickname,null, payerId = member!!.userId); runCatching { repo.addExpense(member!!.householdId,expense,"alior_notification"); localStore.clear(); pending=null; refresh(); celebration=member!!.nickname; ExpenseNotifications.show(context,expense) }.onFailure { error=it.message; loading=false } } },
                reject={ localStore.clear(); pending=null }, logout={ scope.launch { logout() } })
        }
        celebration?.let { Celebration(it) { celebration=null } }
    }
}

@Composable
private fun WelcomeMyszy(done: () -> Unit) {
    val messages = listOf(
        "Mysz i Misio liczą 60/40 szybciej niż kalkulator zdąży ziewnąć.",
        "Tu widać, kto ma do oddania — bez detektywa i bez dramatu.",
        "Paragon nie mieszka już w kieszeni. Robimy mu zdjęcie i ma domek.",
        "PDF, Excel i bank? Myszy lubią porządek bardziej niż tabelki lubią kolumny.",
        "Wspólny bilans dla Myszy i Misia. Settle Up może spokojnie chrupać ser." ,
        "Najnowsze wydatki s\u0105 na g\u00f3rze, bo Myszy nie lubi\u0105 archeologii."
    )
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(messages.size - 1) {
            delay(3500)
            index++
        }
        delay(3500)
        done()
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFFFF2F7), Color(0xFFEDE5F7)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.mysza_ania), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                Image(painterResource(R.drawable.misio_pawel), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(24.dp))
            Text("Zanim zaczniemy...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BearBlue)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(28.dp), tonalElevation = 7.dp, shadowElevation = 4.dp) {
                AnimatedContent(targetState = index, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "welcome_bubble") { current ->
                    Text(messages[current], Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF39324A))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("${index + 1} / ${messages.size}", color = AniaPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
private enum class AuthMode { SIGN_IN, SIGN_UP, RESET_PASSWORD }

@Composable
private fun CloudLogin(
    loading: Boolean,
    error: String?,
    notice: String?,
    retrySession: () -> Unit,
    clearFeedback: () -> Unit,
    signIn: (String, String) -> Unit,
    signUp: (String, String, String) -> Unit,
    resetPassword: (String) -> Unit
) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var showPrivacy by remember { mutableStateOf(false) }
    val scroll = androidx.compose.foundation.rememberScrollState()

    fun changeMode(newMode: AuthMode) {
        mode = newMode
        password = ""
        repeatedPassword = ""
        clearFeedback()
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFF4F7), Color(0xFFEAF2FF), Color(0xFFF8F2FF))))
            .verticalScroll(scroll).imePadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Aplikacja do rozliczania wydatk\u00f3w Mysz\u00f3w", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))
        Surface(color = AniaPurpleSoft, shape = RoundedCornerShape(28.dp), tonalElevation = 3.dp) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.mysza_ania), null, Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                Image(painterResource(R.drawable.misio_pawel), null, Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip({}, { Text("\uD83C\uDF1F wsp\u00f3lny bilans") }); AssistChip({}, { Text("\uD83E\uDDFE paragony") }) }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip({}, { Text("\uD83C\uDFE6 import banku") }); AssistChip({}, { Text("\uD83E\uDD1D sp\u0142aty") }); AssistChip({}, { Text("\uD83D\uDD14 Alior") }) }
        TextButton({ showPrivacy = true }, colors = ButtonDefaults.textButtonColors(contentColor = AniaPurple)) { Text("Spokojnie Myszo \uD83D\uDC2D") }
        Spacer(Modifier.height(8.dp))
        Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = .92f), shape = RoundedCornerShape(24.dp), tonalElevation = 3.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (mode) {
                        AuthMode.SIGN_IN -> "Zaloguj się"
                        AuthMode.SIGN_UP -> "Załóż konto"
                        AuthMode.RESET_PASSWORD -> "Odzyskaj hasło"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (mode == AuthMode.SIGN_UP) {
                    Text("Najpierw tworzysz własne konto. Dom połączysz z drugą osobą w następnym kroku.", color = Color.Gray)
                }
                if (mode == AuthMode.RESET_PASSWORD) {
                    Text("Wyślemy bezpieczny link do ustawienia nowego hasła.", color = Color.Gray)
                }
                OutlinedTextField(login, { login = it }, label = { Text("E-mail") }, placeholder = { Text("twoj@email.pl") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                if (mode != AuthMode.RESET_PASSWORD) {
                    OutlinedTextField(password, { password = it }, label = { Text("Hasło") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                }
                if (mode == AuthMode.SIGN_UP) {
                    OutlinedTextField(repeatedPassword, { repeatedPassword = it }, label = { Text("Powtórz hasło") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    Text("Minimum 8 znaków.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                notice?.let { Text(it, color = MoneyGreen, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = {
                        when (mode) {
                            AuthMode.SIGN_IN -> signIn(login.trim(), password)
                            AuthMode.SIGN_UP -> signUp(login.trim(), password, repeatedPassword)
                            AuthMode.RESET_PASSWORD -> resetPassword(login.trim())
                        }
                    },
                    enabled = !loading && login.isNotBlank() && (mode == AuthMode.RESET_PASSWORD || password.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BearBlue)
                ) {
                    Text(
                        if (loading) "Chwileczkę..." else when (mode) {
                            AuthMode.SIGN_IN -> "Zaloguj się"
                            AuthMode.SIGN_UP -> "Załóż konto"
                            AuthMode.RESET_PASSWORD -> "Wyślij link"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                if (error?.contains("Spr\u00f3buj ponownie") == true) TextButton({ retrySession() }, enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Spr\u00f3buj ponownie") }
                when (mode) {
                    AuthMode.SIGN_IN -> {
                        TextButton({ changeMode(AuthMode.SIGN_UP) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Nie masz konta? Załóż je") }
                        TextButton({ changeMode(AuthMode.RESET_PASSWORD) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Nie pamiętasz hasła?") }
                    }
                    AuthMode.SIGN_UP, AuthMode.RESET_PASSWORD ->
                        TextButton({ changeMode(AuthMode.SIGN_IN) }, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Wróć do logowania") }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = .96f), shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, shadowElevation = 4.dp) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.misio_oto_ja), null, Modifier.size(76.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(10.dp))
                Column { Text("\u2601 Aplikacja zrobiona przez Myszo", color = BearBlue, fontWeight = FontWeight.Bold); Text("Oto ja!", color = MousePink, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
    if (showPrivacy) PrivacyMyszy { showPrivacy = false }
}

@Composable
private fun PasswordRecoveryScreen(
    loading: Boolean,
    error: String?,
    save: (String, String) -> Unit,
    cancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF4F7), Color(0xFFEAF2FF))))
            .imePadding()
            .navigationBarsPadding()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(24.dp), tonalElevation = 4.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ustaw nowe hasło", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Link z wiadomości został potwierdzony. Nowe hasło musi mieć co najmniej 8 znaków.", color = Color.Gray)
                OutlinedTextField(password, { password = it }, label = { Text("Nowe hasło") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(repeatedPassword, { repeatedPassword = it }, label = { Text("Powtórz nowe hasło") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    { save(password, repeatedPassword) },
                    enabled = !loading && password.isNotBlank() && repeatedPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (loading) "Zapisywanie..." else "Zapisz nowe hasło", fontWeight = FontWeight.Bold) }
                TextButton(cancel, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Wróć do logowania") }
            }
        }
    }
}
@Composable
private fun LegacyPrivacyMyszy(close: () -> Unit) {
    val cards = listOf(
        "\uD83D\uDCF7 Aparat\nTylko do zrobienia zdj\u0119cia paragonu.",
        "\uD83D\uDCC2 Pliki i zdj\u0119cia\nTylko gdy chcesz doda\u0107 paragon albo import PDF/Excel.",
        "\uD83D\uDD14 Powiadomienia\nTylko Myszo/Pawe\u0142, tylko po w\u0142\u0105czeniu i tylko dla propozycji Alior.",
        "\uD83D\uDEE1\uFE0F Bez stresu\nNie czytamy kontakt\u00f3w, SMS-\u00f3w ani hase\u0142 bankowych."
    )
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2_400); page = (page + 1) % cards.size } }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFFFF1F6), Color(0xFFEDE5F7)))), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.mysza_ania), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                Image(painterResource(R.drawable.misio_pawel), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(18.dp))
            Text("Spokojnie Myszo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AniaPurple)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
                AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "privacy_cards") { current ->
                    Text(cards[current], Modifier.padding(26.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF39324A))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("${page + 1} / ${cards.size}", color = BearBlue, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Button(close, colors = ButtonDefaults.buttonColors(containerColor = AniaPurple), shape = RoundedCornerShape(16.dp)) { Text("Rozumiem, lecimy dalej") }
        }
    }
}
@Composable
private fun PrivacyMyszy(close: () -> Unit) {
    val cards = listOf(
        "📷 Aparat i pliki\nTylko gdy dodajesz paragon albo importujesz swój PDF/Excel.",
        "🔔 Powiadomienia płatności\nOpcjonalnie przetwarzamy tylko powiadomienia Alior Mobile, Gmail dotyczące Alior oraz Portfela Google. Dostęp możesz wyłączyć w ustawieniach Androida.",
        "☁ Wspólny Dom\nWydatki i paragony zapisują się w prywatnym Domu w Supabase, aby były widoczne dla dwóch sparowanych kont.",
        "🔒 Mniej danych\nPowiadomienia nie pokazują kwoty ani sklepu, a sygnał FCM nie zawiera szczegółów finansowych.",
        "🛡️ Bez stresu\nNie czytamy kontaktów, SMS-ów ani haseł bankowych. Po wylogowaniu usuwamy lokalną kolejkę importu i oczekujące płatności."
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFFFF1F6), Color(0xFFEDE5F7)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(28.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.mysza_ania), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                Image(painterResource(R.drawable.misio_pawel), null, Modifier.size(70.dp).clip(CircleShape), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(18.dp))
            Text("Prywatność w Myszy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AniaPurple)
            Spacer(Modifier.height(14.dp))
            Surface(color = Color.White, shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.forEach { card ->
                        Text(card, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF39324A))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(close, colors = ButtonDefaults.buttonColors(containerColor = AniaPurple), shape = RoundedCornerShape(16.dp)) {
                Text("Rozumiem, lecimy dalej")
            }
        }
    }
}
@Composable
private fun PairingUnavailable(error: String?, retry: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Łączę z Domem Myszy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Konta Myszo i Mysza są już sparowane. Spróbuj ponownie połączyć dane.", color = Color.Gray)
        error?.let { Text(it, Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(18.dp))
        Button(retry, Modifier.fillMaxWidth()) { Text("Połącz ponownie") }
        TextButton(onLogout) { Text("Wyloguj") }
    }
}
@Composable
private fun HouseholdSetup(
    invite: String?,
    error: String?,
    onCreate: (String) -> Unit,
    onJoin: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var nickname by remember { mutableStateOf("Myszo") }
    var code by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF4F7), Color(0xFFEAF2FF), Color(0xFFF8F2FF)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painterResource(R.drawable.mysza_logo),
                contentDescription = "Myszy",
                modifier = Modifier.size(92.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                "Połączcie swój Dom",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AniaPurple
            )
            Text(
                "Jedna osoba tworzy Dom, druga dołącza kodem. W Domu mogą być tylko dwie osoby.",
                color = Color(0xFF554B63),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Utwórz Dom", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Dla osoby, która zaczyna wspólne rozliczenia.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Twój pseudonim") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = { onCreate(nickname.trim()) },
                        enabled = nickname.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BearBlue)
                    ) { Text("Utwórz Dom Myszy", fontWeight = FontWeight.Bold) }
                }
            }

            invite?.let {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = BearBlueSoft,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kod dla drugiej osoby", color = BearBlue, fontWeight = FontWeight.Bold)
                        Text(it, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = BearBlue)
                    }
                }
            }

            ElevatedCard(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. Dołącz do Domu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Wpisz kod otrzymany od drugiej osoby.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Kod zaproszenia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedButton(
                        onClick = { onJoin(code.trim(), nickname.trim()) },
                        enabled = code.isNotBlank() && nickname.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Dołącz do Domu", fontWeight = FontWeight.Bold) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            TextButton(onLogout) { Text("Wyloguj") }
        }
    }
}

@Composable
private fun InviteCodeScreen(code: String, continueToHome: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFF4F7), Color(0xFFEAF2FF), Color(0xFFF8F2FF)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(painterResource(R.drawable.mysza_logo), "Myszy", Modifier.size(104.dp), contentScale = ContentScale.Fit)
            Text("Dom gotowy!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AniaPurple)
            Text("Przekaż ten kod drugiej osobie.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF554B63))
            Surface(color = BearBlueSoft, shape = RoundedCornerShape(24.dp)) {
                Text(
                    code,
                    Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = BearBlue
                )
            }
            Text("Kod jest potrzebny tylko do sparowania kont.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Button(
                continueToHome,
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BearBlue)
            ) { Text("Przejdź do wydatków", fontWeight = FontWeight.Bold) }
        }
    }
}
