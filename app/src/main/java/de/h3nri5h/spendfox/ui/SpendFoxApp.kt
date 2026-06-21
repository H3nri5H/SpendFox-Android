package de.h3nri5h.spendfox.ui

import android.graphics.Paint as AndroidPaint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.h3nri5h.spendfox.data.CategoryScope
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelEntry
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.MaintenanceItem
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.UserCategory
import de.h3nri5h.spendfox.data.Vehicle
import de.h3nri5h.spendfox.data.exporting.ExpenseExportFormat
import de.h3nri5h.spendfox.data.importing.ExpenseImportReviewItem
import de.h3nri5h.spendfox.domain.Money
import de.h3nri5h.spendfox.ui.theme.SpendFoxThemeMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class AppDestination(val title: String) {
    Overview("Übersicht"),
    Contracts("Verträge"),
    Analytics("Analysen"),
    Settings("Einstellungen"),
    FeatureMenu("Bereiche"),
    Expenses("Ausgaben"),
    ExpenseDetail("Abbuchung"),
    ExpenseCategoryPicker("Kategorie"),
    Products("Produkte"),
    Vehicles("Fahrzeuge"),
    CompoundInterest("Zinseszins"),
    ExpenseEditor("Ausgabe"),
    ProductEditor("Produkt"),
    VehicleEditor("Fahrzeug")
}

private val AppDestination.isEditor: Boolean
    get() = this == AppDestination.ExpenseEditor || this == AppDestination.ProductEditor || this == AppDestination.VehicleEditor

private val AppDestination.usesBackNavigation: Boolean
    get() = isEditor || this == AppDestination.FeatureMenu || this == AppDestination.ExpenseDetail || this == AppDestination.ExpenseCategoryPicker

private val AppDestination.hidesBottomBar: Boolean
    get() = usesBackNavigation

private enum class MainTab(val label: String, val icon: ImageVector, val destination: AppDestination) {
    Overview("Übersicht", Icons.Rounded.Home, AppDestination.Overview),
    Contracts("Verträge", Icons.Rounded.List, AppDestination.Contracts),
    Analytics("Analysen", Icons.Rounded.Search, AppDestination.Analytics),
    Settings("Einstellungen", Icons.Rounded.Settings, AppDestination.Settings)
}

private enum class WorkspaceFeature(val label: String, val icon: ImageVector, val destination: AppDestination) {
    Expenses("Ausgaben", Icons.Rounded.List, AppDestination.Expenses),
    Products("Produkte", Icons.Rounded.ShoppingCart, AppDestination.Products),
    Vehicles("Fahrzeuge", Icons.Rounded.Build, AppDestination.Vehicles),
    CompoundInterest("Zinseszins", Icons.Rounded.Search, AppDestination.CompoundInterest)
}

private enum class SettingsDetail(val title: String, val caption: String) {
    Profile("Persönliche Angaben", "Name, E-Mail und Konto"),
    Notifications("Mitteilungen", "Erinnerungen und Hinweise"),
    Categories("Kategorien", "Eigene Listen verwalten"),
    Appearance("Darstellung", "Hell, Dunkel oder System"),
    Security("Sicherheit & Datenschutz", "PIN, Sync und lokale Daten"),
    Legal("Rechtliche Hinweise", "App- und Fahrzeug-Hinweise")
}

private enum class AuthStage {
    Welcome,
    UnlockChoice,
    Credentials,
    EmailVerification
}

private enum class CompoundSolveMode(val label: String) {
    FutureValue("Endkapital"),
    Principal("Startkapital"),
    MonthlyContribution("Monatsrate"),
    InterestRate("Zinssatz")
}

@Composable
fun SpendFoxApp(viewModel: SpendFoxViewModel = viewModel(factory = SpendFoxViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.lockForPrivacy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nutzblickBackdrop())
    ) {
        when (state.mode) {
            AppMode.LoggedOut -> AuthScreen(state, viewModel)
            AppMode.Locked -> DeviceUnlockScreen(state, viewModel)
            AppMode.Unlocked -> MainApp(state, viewModel)
        }
        if (state.isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun AuthScreen(state: SpendFoxUiState, viewModel: SpendFoxViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRegistering by rememberSaveable { mutableStateOf(true) }
    var stage by rememberSaveable { mutableStateOf(AuthStage.Welcome) }
    val pendingRegistrationEmail = state.pendingRegistrationEmail
    LaunchedEffect(pendingRegistrationEmail) {
        if (!pendingRegistrationEmail.isNullOrBlank()) {
            stage = AuthStage.EmailVerification
        }
    }

    if (stage == AuthStage.Welcome) {
        AuthWelcomeScreen(
            onStart = {
                isRegistering = true
                stage = AuthStage.UnlockChoice
            },
            onLogin = {
                isRegistering = false
                stage = AuthStage.UnlockChoice
            }
        )
        return
    }

    if (stage == AuthStage.UnlockChoice) {
        AuthUnlockChoiceScreen(
            selectedMethod = state.unlockMethod,
            onBack = { stage = AuthStage.Welcome },
            onSelected = {
                viewModel.setUnlockMethod(it)
                stage = AuthStage.Credentials
            }
        )
        return
    }

    if (stage == AuthStage.EmailVerification && !pendingRegistrationEmail.isNullOrBlank()) {
        AuthEmailVerificationScreen(
            state = state,
            email = pendingRegistrationEmail,
            onBack = {
                viewModel.cancelRegistrationVerification()
                stage = AuthStage.Credentials
            },
            onVerify = viewModel::verifyRegistrationCode,
            onResend = viewModel::resendRegistrationCode
        )
        return
    }

    AuthCredentialsScreen(
        state = state,
        email = email,
        password = password,
        isRegistering = isRegistering,
        onEmailChange = { email = it },
        onPasswordChange = { password = it },
        onBack = { stage = AuthStage.UnlockChoice },
        onToggleMode = { isRegistering = !isRegistering },
        onSubmit = { if (isRegistering) viewModel.register(email, password) else viewModel.login(email, password) },
        onForgotPassword = { viewModel.forgotPassword(email) },
        onProvider = viewModel::socialLoginUnavailable
    )
}

@Composable
private fun AuthWelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Text("NB", modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text("Hallo, ich bin Finn Fuchs.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Ich bin dein Ansprechpartner in Nutzblick und halte Ausgaben, Produkte, Fahrzeuge und Verträge übersichtlich zusammen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        FeedbackButton(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Erste Schritte", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Ich habe schon ein Konto")
        }
        Spacer(Modifier.weight(1.25f))
    }
}

@Composable
private fun AuthUnlockChoiceScreen(
    selectedMethod: UnlockMethod,
    onBack: () -> Unit,
    onSelected: (UnlockMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
            Text("< Zurück", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(0.6f))
        Text(
            "Wie soll Nutzblick entsperrt werden?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Diese Auswahl gilt nur auf diesem Gerät und kann später in den Einstellungen geändert werden.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(8.dp))
        UnlockMethod.entries.forEach { method ->
            SelectableOptionCard(
                title = method.label,
                description = method.description,
                selected = selectedMethod == method,
                onClick = { onSelected(method) }
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AuthCredentialsScreen(
    state: SpendFoxUiState,
    email: String,
    password: String,
    isRegistering: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onProvider: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .spendFoxVerticalScrollbar(scrollState)
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
            Text("< Zurück", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (isRegistering) "Registrieren" else "Anmelden",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isRegistering) "Gib deine E-Mail-Adresse ein." else "Melde dich mit deinem Nutzblick-Konto an.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-Mail-Adresse") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Passwort") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggleMode, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                Text(if (isRegistering) "Stattdessen anmelden" else "Stattdessen registrieren", textAlign = TextAlign.Start)
            }
            FeedbackButton(
                onClick = onSubmit,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Weiter", fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(onClick = onForgotPassword, modifier = Modifier.fillMaxWidth()) {
            Text("Passwort vergessen")
        }
        AuthDivider()
        ProviderButton("Weiter mit Apple", "A") { onProvider("Apple") }
        ProviderButton("Weiter mit Google", "G") { onProvider("Google") }
        if (!state.authStatus.isSupabaseConfigured) {
            Text(
                text = "Supabase ist noch nicht konfiguriert. Nutzblick benötigt vor dem Test-Publish eine Supabase URL und einen Publishable Key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MessageText(state.message)
    }
}

@Composable
private fun AuthEmailVerificationScreen(
    state: SpendFoxUiState,
    email: String,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by rememberSaveable(email) { mutableStateOf("") }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .spendFoxVerticalScrollbar(scrollState)
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
            Text("< Zurück", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "E-Mail verifizieren",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Wir haben einen 6-stelligen Code an $email gesendet.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = filterIntegerInput(it).take(6) },
            label = { Text("Bestätigungscode") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
        )
        FeedbackButton(
            onClick = { onVerify(code) },
            enabled = code.length == 6 && !state.isBusy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Code bestätigen", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onResend, modifier = Modifier.fillMaxWidth(), enabled = !state.isBusy) {
            Text("Code erneut senden")
        }
        Text(
            text = "Der Link in der Mail wird dafür nicht benötigt. Entscheidend ist der 6-stellige Code.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        MessageText(state.message)
    }
}

@Composable
private fun AuthDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
        Text("oder", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
    }
}

@Composable
private fun ProviderButton(label: String, mark: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        contentColor = Color.Black,
        border = BorderStroke(1.dp, Color(0xFFE6E6E6))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mark, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeviceUnlockScreen(state: SpendFoxUiState, viewModel: SpendFoxViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    LaunchedEffect(activity, state.unlockMethod) {
        if (activity != null) {
            showDeviceUnlock(activity, viewModel, state.unlockMethod)
        }
    }
    CenterScreen {
        GlassCard {
            Text("Nutzblick entsperren", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(state.authStatus.session?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Nutzblick nutzt deine ausgewählte Geräteentsperrung: ${state.unlockMethod.label}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FeedbackButton(
                onClick = {
                    if (activity == null) {
                        viewModel.deviceUnlockFailed("Geräteentsperrung ist hier nicht verfügbar.")
                    } else {
                        showDeviceUnlock(activity, viewModel, state.unlockMethod)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Erneut entsperren")
            }
            TextButton(onClick = viewModel::logout) { Text("Abmelden") }
            MessageText(state.message)
        }
    }
}

private fun showDeviceUnlock(activity: FragmentActivity, viewModel: SpendFoxViewModel, unlockMethod: UnlockMethod) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                viewModel.deviceUnlockSucceeded()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                viewModel.deviceUnlockFailed(errString.toString())
            }

            override fun onAuthenticationFailed() {
                viewModel.deviceUnlockFailed("Entsperrung nicht erkannt.")
            }
        }
    )
    val allowedAuthenticators = when (unlockMethod) {
        UnlockMethod.Biometric -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        UnlockMethod.DeviceCredential -> DEVICE_CREDENTIAL
    }
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Nutzblick entsperren")
        .setSubtitle(unlockMethod.label)
        .setAllowedAuthenticators(allowedAuthenticators)
        .build()
    prompt.authenticate(promptInfo)
}

@Composable
private fun MainApp(state: SpendFoxUiState, viewModel: SpendFoxViewModel) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Overview) }
    var previousDestination by rememberSaveable { mutableStateOf(AppDestination.Overview) }
    var expenseReturnDestination by rememberSaveable { mutableStateOf(AppDestination.Expenses) }
    var expenseEditorReturnDestination by rememberSaveable { mutableStateOf(AppDestination.Expenses) }
    var selectedExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingExpenseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMainTab = MainTab.entries.firstOrNull { it.destination == destination }
    val selectedExpense = selectedExpenseId?.let { id -> state.expenses.firstOrNull { it.id == id } }
    val title = when (destination) {
        AppDestination.ExpenseEditor -> if (editingExpenseId == null) "Neue Ausgabe" else "Ausgabe bearbeiten"
        AppDestination.ProductEditor -> if (editingProductId == null) "Neues Produkt" else "Produkt bearbeiten"
        AppDestination.VehicleEditor -> if (editingVehicleId == null) "Neues Fahrzeug" else "Fahrzeug bearbeiten"
        else -> destination.title
    }
    val closeFeatureMenu: () -> Unit = {
        destination = previousDestination
    }
    val closeExpenseDetail: () -> Unit = {
        destination = expenseReturnDestination
        selectedExpenseId = null
    }
    val closeExpenseCategoryPicker: () -> Unit = {
        destination = AppDestination.ExpenseDetail
    }
    val closeEditor: () -> Unit = {
        destination = when (destination) {
            AppDestination.ExpenseEditor -> expenseEditorReturnDestination
            AppDestination.ProductEditor -> AppDestination.Products
            AppDestination.VehicleEditor -> AppDestination.Vehicles
            else -> AppDestination.Overview
        }
    }
    BackHandler(enabled = destination == AppDestination.FeatureMenu) {
        closeFeatureMenu()
    }
    BackHandler(enabled = destination == AppDestination.ExpenseDetail) {
        closeExpenseDetail()
    }
    BackHandler(enabled = destination == AppDestination.ExpenseCategoryPicker) {
        closeExpenseCategoryPicker()
    }
    BackHandler(enabled = destination.isEditor) {
        closeEditor()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            SpendFoxTopBar(
                title = title,
                showBack = destination.usesBackNavigation,
                onNavigationClick = {
                    if (destination.usesBackNavigation) {
                        when (destination) {
                            AppDestination.FeatureMenu -> closeFeatureMenu()
                            AppDestination.ExpenseDetail -> closeExpenseDetail()
                            AppDestination.ExpenseCategoryPicker -> closeExpenseCategoryPicker()
                            else -> closeEditor()
                        }
                    } else {
                        previousDestination = destination
                        destination = AppDestination.FeatureMenu
                    }
                },
                action = {
                    if (destination == AppDestination.ExpenseDetail && selectedExpense != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkThemeActive()) 0.92f else 0.88f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
                        ) {
                            TextButton(onClick = {
                                editingExpenseId = selectedExpense.id
                                expenseEditorReturnDestination = AppDestination.ExpenseDetail
                                destination = AppDestination.ExpenseEditor
                            }) {
                                Text("Bearbeiten")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!destination.hidesBottomBar) {
                GlassNavigationBar(selectedTab = selectedMainTab, onSelected = { destination = it.destination })
            }
        }
    ) { padding ->
        when (destination) {
            AppDestination.Overview -> DashboardScreen(
                state = state,
                padding = padding,
                onOpenFeature = { destination = it.destination },
                onOpenExpense = {
                    selectedExpenseId = it
                    expenseReturnDestination = AppDestination.Overview
                    destination = AppDestination.ExpenseDetail
                }
            )
            AppDestination.FeatureMenu -> FeatureMenuScreen(
                padding = padding,
                onOpenDestination = { destination = it }
            )
            AppDestination.Contracts -> ContractsScreen(state, padding)
            AppDestination.Analytics -> AnalyticsScreen(state, padding)
            AppDestination.Settings -> SettingsScreen(state, padding, viewModel)
            AppDestination.Expenses -> ExpensesScreen(
                state = state,
                padding = padding,
                viewModel = viewModel,
                onAddExpense = {
                    editingExpenseId = null
                    expenseEditorReturnDestination = AppDestination.Expenses
                    destination = AppDestination.ExpenseEditor
                },
                onOpenExpense = {
                    selectedExpenseId = it
                    expenseReturnDestination = AppDestination.Expenses
                    destination = AppDestination.ExpenseDetail
                }
            )
            AppDestination.ExpenseDetail -> ExpenseDetailScreen(
                expense = selectedExpense,
                allExpenses = state.expenses,
                padding = padding,
                onSelectCategory = {
                    if (selectedExpense != null) destination = AppDestination.ExpenseCategoryPicker
                }
            )
            AppDestination.ExpenseCategoryPicker -> ExpenseCategoryPickerScreen(
                expense = selectedExpense,
                userCategories = state.categories.filter { it.scope == CategoryScope.Expense },
                padding = padding,
                onSelect = { option ->
                    selectedExpense?.let { expense ->
                        if (viewModel.updateExpenseCategory(expense.id, option.category, option.customCategory)) {
                            destination = AppDestination.ExpenseDetail
                        }
                    }
                }
            )
            AppDestination.Products -> ProductsScreen(
                state = state,
                padding = padding,
                viewModel = viewModel,
                onAddProduct = {
                    editingProductId = null
                    destination = AppDestination.ProductEditor
                },
                onEditProduct = {
                    editingProductId = it
                    destination = AppDestination.ProductEditor
                }
            )
            AppDestination.CompoundInterest -> CompoundInterestScreen(padding)
            AppDestination.Vehicles -> VehiclesScreen(
                state = state,
                padding = padding,
                viewModel = viewModel,
                onAddVehicle = {
                    editingVehicleId = null
                    destination = AppDestination.VehicleEditor
                },
                onEditVehicle = {
                    editingVehicleId = it
                    destination = AppDestination.VehicleEditor
                }
            )
            AppDestination.ExpenseEditor -> ExpenseEditorScreen(
                expense = editingExpenseId?.let { id -> state.expenses.firstOrNull { it.id == id } },
                categories = state.categories.filter { it.scope == CategoryScope.Expense },
                padding = padding,
                onBack = closeEditor,
                onSave = { data ->
                    if (viewModel.upsertExpense(editingExpenseId, data.amount, data.merchant, data.category, data.customCategory, data.date, data.paymentAccount, data.bookingText, data.purpose, data.note, data.tags)) {
                        destination = expenseEditorReturnDestination
                        editingExpenseId = null
                    }
                }
            )
            AppDestination.ProductEditor -> ProductEditorScreen(
                product = editingProductId?.let { id -> state.products.firstOrNull { it.id == id } },
                categories = state.categories.filter { it.scope == CategoryScope.Product },
                padding = padding,
                onBack = closeEditor,
                onSave = { data ->
                    if (viewModel.addProduct(editingProductId, data.name, data.manufacturer, data.modelName, data.serialReference, data.price, data.category, data.customCategory, data.purchasedAt, data.usageMonths, data.warrantyUntil, data.note)) {
                        destination = AppDestination.Products
                        editingProductId = null
                    }
                }
            )
            AppDestination.VehicleEditor -> VehicleEditorScreen(
                vehicle = editingVehicleId?.let { id -> state.vehicles.firstOrNull { it.id == id } },
                padding = padding,
                onBack = closeEditor,
                onSave = { name, manufacturer, model, plate, fuelType ->
                    if (viewModel.addVehicle(editingVehicleId, name, manufacturer, model, plate, fuelType)) {
                        destination = AppDestination.Vehicles
                        editingVehicleId = null
                    }
                }
            )
        }
    }
}

@Composable
private fun FeatureMenuScreen(padding: PaddingValues, onOpenDestination: (AppDestination) -> Unit) {
    ListScreen(padding) {
        item { SectionTitle("Startseite") }
        item {
            DestinationShortcutCard(
                title = "Startseite",
                caption = "Übersicht, Monatswerte und letzte Einträge",
                icon = Icons.Rounded.Home,
                onClick = { onOpenDestination(AppDestination.Overview) }
            )
        }
        item { SectionTitle("Features") }
        items(WorkspaceFeature.entries, key = { it.name }) { feature ->
            DestinationShortcutCard(
                title = feature.label,
                caption = "",
                icon = feature.icon,
                onClick = { onOpenDestination(feature.destination) }
            )
        }
        item { SectionTitle("Hauptbereiche") }
        item {
            DestinationShortcutCard(
                title = "Verträge",
                caption = "Laufende Verträge und Fixkosten",
                icon = Icons.Rounded.List,
                onClick = { onOpenDestination(AppDestination.Contracts) }
            )
        }
        item {
            DestinationShortcutCard(
                title = "Analysen",
                caption = "Auswertungen und monatliche Entwicklung",
                icon = Icons.Rounded.Search,
                onClick = { onOpenDestination(AppDestination.Analytics) }
            )
        }
        item {
            DestinationShortcutCard(
                title = "Einstellungen",
                caption = "Darstellung, Sicherheit und App-Verhalten",
                icon = Icons.Rounded.Settings,
                onClick = { onOpenDestination(AppDestination.Settings) }
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    state: SpendFoxUiState,
    padding: PaddingValues,
    onOpenFeature: (WorkspaceFeature) -> Unit,
    onOpenExpense: (String) -> Unit
) {
    ListScreen(padding) {
        item { SummaryCard("Ausgaben im Monat", Money.format(state.expenseTotalCents), "Aktueller Monat") }
        item { SummaryCard("Produkt-Restwert", Money.format(state.productResidualValueCents), "Linear aus Nutzungsdauer") }
        item { SummaryCard("Fahrzeuge", state.vehicles.size.toString(), "Autos und Wartung") }
        item { SectionTitle("Bereiche") }
        item { FeatureShortcutCard(WorkspaceFeature.Expenses, Money.format(state.expenseTotalCents)) { onOpenFeature(WorkspaceFeature.Expenses) } }
        item { FeatureShortcutCard(WorkspaceFeature.Products, Money.format(state.productResidualValueCents)) { onOpenFeature(WorkspaceFeature.Products) } }
        item { FeatureShortcutCard(WorkspaceFeature.Vehicles, state.vehicles.size.toString()) { onOpenFeature(WorkspaceFeature.Vehicles) } }
        item { FeatureShortcutCard(WorkspaceFeature.CompoundInterest, "Planen") { onOpenFeature(WorkspaceFeature.CompoundInterest) } }
        item { SectionTitle("Letzte Ausgaben") }
        if (state.currentMonthExpenses.isEmpty()) {
            item { EmptyState("Noch keine Ausgaben im aktuellen Monat vorhanden.") }
        } else {
            items(state.currentMonthExpenses.take(5), key = { it.id }) { expense -> ExpenseCard(expense, onOpen = { onOpenExpense(expense.id) }, onDelete = null) }
        }
    }
}

@Composable
private fun ContractsScreen(state: SpendFoxUiState, padding: PaddingValues) {
    ListScreen(padding) {
        item { SectionTitle("Verträge") }
        item {
            EmptyState("Verträge sind als eigener Hauptbereich vorbereitet. Die Detailverwaltung können wir als nächstes sauber ergänzen.")
        }
        item { SummaryCard("Fixkosten sichtbar machen", Money.format(0), "Später für Abos, Versicherungen und laufende Verträge") }
    }
}

@Composable
private fun AnalyticsScreen(state: SpendFoxUiState, padding: PaddingValues) {
    val monthlyProductCost = state.products.sumOf { it.monthlyCostCents ?: 0L }
    ListScreen(padding) {
        item { SectionTitle("Analysen") }
        item { SummaryCard("Ausgaben im Monat", Money.format(state.expenseTotalCents), "Aktueller Monat") }
        item { SummaryCard("Produktkosten pro Monat", Money.format(monthlyProductCost), "Aus Nutzungsdauer berechnet") }
        item { SummaryCard("Produkt-Restwert", Money.format(state.productResidualValueCents), "Aktueller theoretischer Restwert") }
        item { SummaryCard("Fahrzeuge", state.vehicles.size.toString(), "Tankjournal und Wartung") }
    }
}

@Composable
private fun CompoundInterestScreen(padding: PaddingValues) {
    var solveMode by rememberSaveable { mutableStateOf(CompoundSolveMode.FutureValue) }
    var principal by rememberSaveable { mutableStateOf("10.000€") }
    var monthly by rememberSaveable { mutableStateOf("250€") }
    var rate by rememberSaveable { mutableStateOf("5") }
    var years by rememberSaveable { mutableStateOf("20") }
    var target by rememberSaveable { mutableStateOf("100.000€") }
    var selectedMonth by rememberSaveable { mutableIntStateOf(0) }
    val result = calculateCompoundResult(solveMode, principal, monthly, rate, years, target)
    val selectedIndex = selectedMonth.coerceIn(0, result?.timeline?.lastIndex ?: 0)

    ListScreen(padding) {
        item { SectionTitle("Zinseszinsrechner") }
        item {
            GlassCard {
                CategoryChips(CompoundSolveMode.entries, solveMode, { solveMode = it }) { it.label }
                Text("Gesucht wird: ${solveMode.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            GlassCard {
                if (solveMode != CompoundSolveMode.Principal) {
                    OutlinedTextField(principal, { principal = formatCurrencyInput(it, principal) }, label = { Text("Startkapital") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (solveMode != CompoundSolveMode.MonthlyContribution) {
                    OutlinedTextField(monthly, { monthly = formatCurrencyInput(it, monthly) }, label = { Text("Monatliche Einzahlung") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (solveMode != CompoundSolveMode.InterestRate) {
                    OutlinedTextField(rate, { rate = filterDecimalInput(it, rate, maxDecimals = 2) }, label = { Text("Zinssatz p.a. in %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(years, { years = filterDecimalInput(it, years, maxDecimals = 1) }, label = { Text("Laufzeit in Jahren") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (solveMode != CompoundSolveMode.FutureValue) {
                    OutlinedTextField(target, { target = formatCurrencyInput(it, target) }, label = { Text("Ziel-/Endkapital") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (result == null) {
            item { EmptyState("Bitte vollständige Werte eintragen, damit Nutzblick rechnen kann.") }
        } else {
            item {
                SummaryCard(result.title, result.valueLabel, result.caption)
            }
            item {
                CompoundChartCard(
                    result = result,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { selectedMonth = it }
                )
            }
        }
    }
}

@Composable
private fun CompoundChartCard(
    result: CompoundResult,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit
) {
    val values = result.timeline
    val comparisonValues = result.comparisonTimeline
    GlassCard {
        Text("Zeitverlauf", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (values.size > 1) {
            val selectedValue = values[selectedIndex]
            val selectedComparisonValue = comparisonValues.getOrNull(selectedIndex) ?: selectedValue
            Text(
                "Monat $selectedIndex: ${formatEuro(selectedValue)} | ohne Zinseszins ${formatEuro(selectedComparisonValue)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ChartLegendDot(MaterialTheme.colorScheme.primary, "Mit Zinseszins")
                ChartLegendDot(MaterialTheme.colorScheme.secondary, "Ohne Zinseszins")
            }
            CompoundLineChart(values, comparisonValues, selectedIndex)
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { onSelectedIndexChange(it.roundToInt().coerceIn(0, values.lastIndex)) },
                valueRange = 0f..values.lastIndex.toFloat(),
                steps = (values.size - 2).coerceAtLeast(0)
            )
        } else {
            Text("Für eine Grafik braucht der Rechner eine Laufzeit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompoundLineChart(values: List<Double>, comparisonValues: List<Double>, selectedIndex: Int) {
    val lineColor = MaterialTheme.colorScheme.primary
    val comparisonColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val markerColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(232.dp)) {
        val chartValues = values + comparisonValues
        val maxValue = chartValues.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        val minValue = minOf(0.0, chartValues.minOrNull() ?: 0.0).coerceAtMost(maxValue)
        val range = (maxValue - minValue).takeIf { abs(it) > 0.0001 } ?: 1.0
        val left = 58.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 18.dp.toPx()
        val bottom = size.height - 42.dp.toPx()
        val tickPaint = AndroidPaint().apply {
            isAntiAlias = true
            color = labelColor.toArgb()
            textSize = 10.sp.toPx()
            textAlign = AndroidPaint.Align.RIGHT
        }
        val axisPaint = AndroidPaint().apply {
            isAntiAlias = true
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
            textAlign = AndroidPaint.Align.CENTER
        }
        val canvas = drawContext.canvas.nativeCanvas
        repeat(4) { index ->
            val factor = index / 3f
            val y = top + (bottom - top) * factor
            val tickValue = maxValue - range * factor
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            canvas.drawText(formatCompactEuro(tickValue), left - 8.dp.toPx(), y + 3.dp.toPx(), tickPaint)
        }
        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 1.3.dp.toPx())
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.3.dp.toPx())
        drawCompoundSeries(comparisonValues, minValue, range, left, right, top, bottom, comparisonColor, 2.dp.toPx())
        drawCompoundSeries(values, minValue, range, left, right, top, bottom, lineColor, 3.dp.toPx())
        val lastIndex = values.lastIndex.coerceAtLeast(1)
        listOf(0, lastIndex / 2, lastIndex).distinct().forEach { month ->
            val x = left + (right - left) * month / lastIndex.toFloat()
            canvas.drawText(month.toString(), x, bottom + 17.dp.toPx(), axisPaint)
        }
        canvas.drawText("Laufzeit (Monate)", (left + right) / 2f, size.height - 6.dp.toPx(), axisPaint)
        canvas.save()
        canvas.rotate(-90f)
        canvas.drawText("Kapital (EUR)", -((top + bottom) / 2f), 12.dp.toPx(), axisPaint)
        canvas.restore()
        val selected = selectedIndex.coerceIn(values.indices)
        val selectedX = left + (right - left) * selected / lastIndex.toFloat()
        val selectedY = bottom - ((values[selected] - minValue) / range).toFloat() * (bottom - top)
        drawLine(markerColor.copy(alpha = 0.55f), Offset(selectedX, top), Offset(selectedX, bottom), strokeWidth = 2.dp.toPx())
        drawCircle(markerColor, radius = 5.dp.toPx(), center = Offset(selectedX, selectedY))
    }
}

private fun DrawScope.drawCompoundSeries(
    values: List<Double>,
    minValue: Double,
    range: Double,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    color: Color,
    strokeWidth: Float
) {
    if (values.size < 2) return
    val lastIndex = values.lastIndex.coerceAtLeast(1)
    values.zipWithNext().forEachIndexed { index, pair ->
        val x1 = left + (right - left) * index / lastIndex.toFloat()
        val x2 = left + (right - left) * (index + 1) / lastIndex.toFloat()
        val y1 = bottom - ((pair.first - minValue) / range).toFloat() * (bottom - top)
        val y2 = bottom - ((pair.second - minValue) / range).toFloat() * (bottom - top)
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = strokeWidth)
    }
}

private data class CompoundResult(
    val title: String,
    val valueLabel: String,
    val caption: String,
    val timeline: List<Double>,
    val comparisonTimeline: List<Double>
)

private fun calculateCompoundResult(
    solveMode: CompoundSolveMode,
    principalText: String,
    monthlyText: String,
    rateText: String,
    yearsText: String,
    targetText: String
): CompoundResult? {
    val years = parseDecimal(yearsText)?.takeIf { it > 0.0 } ?: return null
    val months = (years * 12.0).roundToInt().coerceAtLeast(1)
    val principal = if (solveMode == CompoundSolveMode.Principal) null else parseMoneyValue(principalText)
    val monthly = if (solveMode == CompoundSolveMode.MonthlyContribution) null else parseMoneyValue(monthlyText)
    val rate = if (solveMode == CompoundSolveMode.InterestRate) null else parseDecimal(rateText)
    val target = if (solveMode == CompoundSolveMode.FutureValue) null else parseMoneyValue(targetText)
    return when (solveMode) {
        CompoundSolveMode.FutureValue -> {
            val p = principal ?: return null
            val m = monthly ?: return null
            val r = rate ?: return null
            val value = compoundFutureValue(p, m, r, months)
            CompoundResult(
                "Endkapital",
                formatEuro(value),
                "Nach $months Monaten bei ${formatPercent(r)} p.a.",
                compoundTimeline(p, m, r, months),
                simpleInterestTimeline(p, m, r, months)
            )
        }
        CompoundSolveMode.Principal -> {
            val m = monthly ?: return null
            val r = rate ?: return null
            val t = target ?: return null
            val required = compoundRequiredPrincipal(t, m, r, months).coerceAtLeast(0.0)
            CompoundResult(
                "Benötigtes Startkapital",
                formatEuro(required),
                "Für ${formatEuro(t)} Zielkapital.",
                compoundTimeline(required, m, r, months),
                simpleInterestTimeline(required, m, r, months)
            )
        }
        CompoundSolveMode.MonthlyContribution -> {
            val p = principal ?: return null
            val r = rate ?: return null
            val t = target ?: return null
            val required = compoundRequiredMonthly(t, p, r, months).coerceAtLeast(0.0)
            CompoundResult(
                "Benötigte Monatsrate",
                formatEuro(required),
                "Für ${formatEuro(t)} Zielkapital.",
                compoundTimeline(p, required, r, months),
                simpleInterestTimeline(p, required, r, months)
            )
        }
        CompoundSolveMode.InterestRate -> {
            val p = principal ?: return null
            val m = monthly ?: return null
            val t = target ?: return null
            val required = compoundRequiredRate(t, p, m, months) ?: return null
            CompoundResult(
                "Benötigter Zinssatz",
                formatPercent(required),
                "Für ${formatEuro(t)} Zielkapital.",
                compoundTimeline(p, m, required, months),
                simpleInterestTimeline(p, m, required, months)
            )
        }
    }
}

private fun compoundFutureValue(principal: Double, monthlyContribution: Double, annualRatePercent: Double, months: Int): Double {
    val monthlyRate = annualRatePercent / 100.0 / 12.0
    if (abs(monthlyRate) < 0.0000001) return principal + monthlyContribution * months
    val growth = (1.0 + monthlyRate).pow(months.toDouble())
    return principal * growth + monthlyContribution * ((growth - 1.0) / monthlyRate)
}

private fun compoundRequiredPrincipal(target: Double, monthlyContribution: Double, annualRatePercent: Double, months: Int): Double {
    val monthlyRate = annualRatePercent / 100.0 / 12.0
    if (abs(monthlyRate) < 0.0000001) return target - monthlyContribution * months
    val growth = (1.0 + monthlyRate).pow(months.toDouble())
    val contributionGrowth = monthlyContribution * ((growth - 1.0) / monthlyRate)
    return (target - contributionGrowth) / growth
}

private fun compoundRequiredMonthly(target: Double, principal: Double, annualRatePercent: Double, months: Int): Double {
    val monthlyRate = annualRatePercent / 100.0 / 12.0
    if (abs(monthlyRate) < 0.0000001) return (target - principal) / months
    val growth = (1.0 + monthlyRate).pow(months.toDouble())
    return (target - principal * growth) / ((growth - 1.0) / monthlyRate)
}

private fun compoundRequiredRate(target: Double, principal: Double, monthlyContribution: Double, months: Int): Double? {
    if (target <= compoundFutureValue(principal, monthlyContribution, 0.0, months)) return 0.0
    var low = 0.0
    var high = 100.0
    if (target > compoundFutureValue(principal, monthlyContribution, high, months)) return null
    repeat(80) {
        val mid = (low + high) / 2.0
        if (compoundFutureValue(principal, monthlyContribution, mid, months) < target) {
            low = mid
        } else {
            high = mid
        }
    }
    return high
}

private fun compoundTimeline(principal: Double, monthlyContribution: Double, annualRatePercent: Double, months: Int): List<Double> {
    return (0..months).map { compoundFutureValue(principal, monthlyContribution, annualRatePercent, it) }
}

private fun simpleInterestTimeline(principal: Double, monthlyContribution: Double, annualRatePercent: Double, months: Int): List<Double> {
    val monthlyRate = annualRatePercent / 100.0 / 12.0
    return (0..months).map { month ->
        val contributionInterestMonths = month * (month - 1) / 2.0
        principal * (1.0 + monthlyRate * month) +
            monthlyContribution * month +
            monthlyContribution * monthlyRate * contributionInterestMonths
    }
}

private fun parseMoneyValue(value: String): Double? {
    val cleaned = value
        .trim()
        .replace("€", "")
        .replace(" ", "")
        .replace("\u00A0", "")
    if (cleaned.isBlank()) return null
    val normalized = if (cleaned.contains(',')) {
        cleaned.replace(".", "").replace(',', '.')
    } else {
        val dotCount = cleaned.count { it == '.' }
        if (dotCount == 1 && cleaned.substringAfter('.').length in 1..2) cleaned else cleaned.replace(".", "")
    }
    return normalized.toDoubleOrNull()?.takeIf { it >= 0.0 }
}

private val currencyInputRegex = Regex("""^[0-9.,€\s\u00A0]*$""")
private val decimalInputRegex = Regex("""^[0-9,.]*$""")

private fun formatCurrencyInput(input: String, fallback: String = ""): String {
    if (!currencyInputRegex.matches(input)) return fallback
    val raw = input
        .replace("€", "")
        .replace(" ", "")
        .replace("\u00A0", "")
    if (raw.isBlank()) return ""

    val dotIsDecimalSeparator = !raw.contains(',') &&
        raw.count { it == '.' } == 1 &&
        raw.substringAfter('.').length in 1..2
    val hasFraction = raw.contains(',') || dotIsDecimalSeparator
    val wholeRaw = when {
        raw.contains(',') -> raw.substringBefore(',')
        dotIsDecimalSeparator -> raw.substringBefore('.')
        else -> raw
    }
    val wholeDigits = wholeRaw.filter(Char::isDigit)
    if (wholeDigits.isBlank()) return if (hasFraction) "0," else ""

    val whole = formatThousands(wholeDigits)
    if (!hasFraction) return "$whole€"

    val fractionRaw = if (raw.contains(',')) raw.substringAfter(',', "") else raw.substringAfter('.', "")
    val fraction = fractionRaw.filter(Char::isDigit).take(2)
    return when {
        raw.endsWith(",") || raw.endsWith(".") -> "$whole,"
        fraction.length < 2 -> "$whole,$fraction"
        else -> "$whole,$fraction€"
    }
}

private fun formatCurrencyFromCents(cents: Long): String {
    val euros = cents / 100
    val fraction = (cents % 100).toInt()
    return if (fraction == 0) {
        "${formatThousands(euros.toString())}€"
    } else {
        "${formatThousands(euros.toString())},${fraction.toString().padStart(2, '0')}€"
    }
}

private fun filterDecimalInput(input: String, fallback: String = "", maxDecimals: Int = 2): String {
    if (!decimalInputRegex.matches(input)) return fallback
    val normalized = input.replace(',', '.')
    if (normalized.isBlank()) return ""
    if (normalized.count { it == '.' } > 1) return fallback

    val wholeDigits = normalized.substringBefore('.').filter(Char::isDigit).ifBlank { "0" }
    val hasSeparator = normalized.contains('.') || input.endsWith(',') || input.endsWith('.')
    val fraction = normalized.substringAfter('.', "").filter(Char::isDigit).take(maxDecimals)
    return if (hasSeparator) {
        "${wholeDigits.trimStart('0').ifBlank { "0" }},$fraction"
    } else {
        wholeDigits.trimStart('0').ifBlank { "0" }
    }
}

private fun filterIntegerInput(input: String): String = input.filter(Char::isDigit)

private fun filterPhoneInput(input: String): String {
    return input.filterIndexed { index, char ->
        char.isDigit() || char == ' ' || char == '/' || char == '-' || (char == '+' && index == 0)
    }.take(24)
}

private fun formatDateInput(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 4 || index == 6) append('-')
            append(char)
        }
    }
}

private fun formatGermanDateInput(input: String): String {
    val digits = input.filter(Char::isDigit).take(8)
    return buildString {
        digits.forEachIndexed { index, char ->
            if (index == 2 || index == 4) append('.')
            append(char)
        }
    }
}

private fun formatThousands(digits: String): String {
    val normalized = digits.trimStart('0').ifBlank { "0" }
    return normalized.reversed().chunked(3).joinToString(".").reversed()
}

private fun parseDecimal(value: String): Double? = value.trim().replace(",", ".").toDoubleOrNull()

private fun formatEuro(value: Double): String = Money.format((value * 100.0).roundToLong())

private fun formatCompactEuro(value: Double): String {
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000.0 -> "%.1f Mio.".format(Locale.GERMANY, value / 1_000_000.0)
        absolute >= 1_000.0 -> "%.0f Tsd.".format(Locale.GERMANY, value / 1_000.0)
        else -> "%.0f".format(Locale.GERMANY, value)
    }
}

private fun formatPercent(value: Double): String = "%.2f %%".format(Locale.GERMANY, value)

@Composable
private fun ExpenseDetailScreen(
    expense: Expense?,
    allExpenses: List<Expense>,
    padding: PaddingValues,
    onSelectCategory: () -> Unit
) {
    if (expense == null) {
        ListScreen(padding) {
            item { EmptyState("Diese Abbuchung wurde nicht gefunden.") }
        }
        return
    }
    val categoryLabel = expense.customCategory.ifBlank { expense.category.label }
    val merchantExpenses = allExpenses.filter { it.merchant.equals(expense.merchant, ignoreCase = true) }
    ListScreen(padding) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MerchantLogo(expense.merchant)
                Text(expense.merchant, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Surface(
                    modifier = Modifier.clickable(onClick = onSelectCategory),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(categoryLabel, fontWeight = FontWeight.SemiBold)
                        Text(">", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            GlassCard {
                DetailRow("Betrag", formatDebitAmount(expense.amountCents))
                DetailDivider()
                DetailRow("Buchungsdatum", relativeOrFormattedDate(expense.occurredAtEpochMillis))
                DetailDivider()
                DetailBlock("Zweck", expensePurposeText(expense))
                DetailDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Umbuchung", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(checked = false, onCheckedChange = {}, enabled = false)
                }
                DetailDivider()
                TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) { Text(if (expense.tags.isBlank()) "Tags hinzufügen" else expense.tags) }
                DetailDivider()
                TextButton(onClick = {}, contentPadding = PaddingValues(0.dp)) { Text(if (expense.note.isBlank()) "Notiz hinzufügen" else expense.note) }
            }
        }
        item {
            MerchantSpendChart(expense.merchant, merchantExpenses)
        }
    }
}

@Composable
private fun MerchantLogo(merchant: String) {
    val label = merchant.trim().take(4).uppercase(Locale.GERMANY).ifBlank { "NB" }
    Surface(
        modifier = Modifier.size(96.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = if (isDarkThemeActive()) 0.dp else 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DetailDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)))
}

@Composable
private fun MerchantSpendChart(merchant: String, expenses: List<Expense>) {
    val monthlyValues = merchantMonthlyValues(expenses)
    GlassCard {
        Text("Ausgaben bei $merchant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (monthlyValues.isEmpty()) {
            Text("Noch nicht genug Daten für eine Auswertung.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            MonthlyBarChart(monthlyValues)
        }
    }
}

@Composable
private fun MonthlyBarChart(values: List<Pair<String, Long>>) {
    val barColor = MaterialTheme.colorScheme.primary
    val mutedBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(126.dp)) {
            val maxValue = values.maxOfOrNull { it.second }?.takeIf { it > 0L } ?: 1L
            val barAreaTop = 8.dp.toPx()
            val barAreaBottom = size.height - 4.dp.toPx()
            val slotWidth = size.width / values.size.coerceAtLeast(1)
            values.forEachIndexed { index, item ->
                val heightRatio = item.second.toFloat() / maxValue.toFloat()
                val barHeight = (barAreaBottom - barAreaTop) * heightRatio
                val barWidth = slotWidth * 0.68f
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = barAreaBottom - barHeight
                drawRoundRect(
                    color = if (index >= values.lastIndex - 2) barColor else mutedBarColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(3.dp.toPx())),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            values.forEach { item ->
                Text(item.first, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExpenseCategoryPickerScreen(
    expense: Expense?,
    userCategories: List<UserCategory>,
    padding: PaddingValues,
    onSelect: (ExpenseCategoryOption) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    if (expense == null) {
        ListScreen(padding) { item { EmptyState("Diese Abbuchung wurde nicht gefunden.") } }
        return
    }
    val filteredGroups = expenseCategoryGroups(userCategories).mapNotNull { group ->
        val options = group.options.filter { option ->
            query.isBlank() ||
                option.label.contains(query, ignoreCase = true) ||
                group.title.contains(query, ignoreCase = true)
        }
        when {
            query.isBlank() -> group
            options.isNotEmpty() -> group.copy(options = options)
            else -> null
        }
    }
    ListScreen(padding) {
        item { SearchField(query) { query = it } }
        items(filteredGroups, key = { it.title }) { group ->
            val isExpanded = query.isNotBlank() || expandedGroup == group.title
            GlassCard(modifier = Modifier.clickable {
                expandedGroup = if (isExpanded && query.isBlank()) null else group.title
            }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(group.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${group.options.size} Unterkategorien", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (isExpanded) "−" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (isExpanded) {
                    group.options.forEach { option ->
                        CategoryOptionRow(
                            option = option,
                            selected = expense.category == option.category && expense.customCategory == option.customCategory,
                            onClick = { onSelect(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionRow(option: ExpenseCategoryOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(option.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (selected) Text("Ausgewählt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private data class ExpenseCategoryGroup(
    val title: String,
    val options: List<ExpenseCategoryOption>
)

private data class ExpenseCategoryOption(
    val label: String,
    val category: ExpenseCategory,
    val customCategory: String = ""
)

private fun expenseCategoryGroups(userCategories: List<UserCategory>): List<ExpenseCategoryGroup> {
    val baseGroups = listOf(
        ExpenseCategoryGroup("Alltag", listOf(
            ExpenseCategoryOption("Lebensmittel", ExpenseCategory.Groceries),
            ExpenseCategoryOption("Supermarkt", ExpenseCategory.Groceries, "Supermarkt"),
            ExpenseCategoryOption("Drogerie", ExpenseCategory.Groceries, "Drogerie"),
            ExpenseCategoryOption("Haushalt", ExpenseCategory.Other, "Haushalt")
        )),
        ExpenseCategoryGroup("Wohnen", listOf(
            ExpenseCategoryOption("Miete", ExpenseCategory.Housing, "Miete"),
            ExpenseCategoryOption("Nebenkosten", ExpenseCategory.Housing, "Nebenkosten"),
            ExpenseCategoryOption("Strom", ExpenseCategory.Housing, "Strom"),
            ExpenseCategoryOption("Internet", ExpenseCategory.Housing, "Internet"),
            ExpenseCategoryOption("Einrichtung", ExpenseCategory.Housing, "Einrichtung")
        )),
        ExpenseCategoryGroup("Mobilität", listOf(
            ExpenseCategoryOption("Tanken", ExpenseCategory.Mobility, "Tanken"),
            ExpenseCategoryOption("Auto", ExpenseCategory.Mobility, "Auto"),
            ExpenseCategoryOption("ÖPNV", ExpenseCategory.Mobility, "ÖPNV"),
            ExpenseCategoryOption("Parken", ExpenseCategory.Mobility, "Parken"),
            ExpenseCategoryOption("Reisen", ExpenseCategory.Mobility, "Reisen")
        )),
        ExpenseCategoryGroup("Freizeit", listOf(
            ExpenseCategoryOption("Restaurant", ExpenseCategory.Leisure, "Restaurant"),
            ExpenseCategoryOption("Streaming", ExpenseCategory.Leisure, "Streaming"),
            ExpenseCategoryOption("Shopping", ExpenseCategory.Leisure, "Shopping"),
            ExpenseCategoryOption("Hobby", ExpenseCategory.Leisure, "Hobby"),
            ExpenseCategoryOption("Urlaub", ExpenseCategory.Leisure, "Urlaub")
        )),
        ExpenseCategoryGroup("Gesundheit", listOf(
            ExpenseCategoryOption("Apotheke", ExpenseCategory.Health, "Apotheke"),
            ExpenseCategoryOption("Arzt", ExpenseCategory.Health, "Arzt"),
            ExpenseCategoryOption("Fitness", ExpenseCategory.Health, "Fitness"),
            ExpenseCategoryOption("Versicherung", ExpenseCategory.Health, "Versicherung")
        )),
        ExpenseCategoryGroup("Sonstiges", listOf(
            ExpenseCategoryOption("Gebühren", ExpenseCategory.Other, "Gebühren"),
            ExpenseCategoryOption("Umbuchung", ExpenseCategory.Other, "Umbuchung"),
            ExpenseCategoryOption("Spenden", ExpenseCategory.Other, "Spenden"),
            ExpenseCategoryOption("Sonstiges", ExpenseCategory.Other)
        ))
    )
    val customOptions = userCategories
        .filter { it.label.isNotBlank() }
        .map { ExpenseCategoryOption(it.label, ExpenseCategory.Other, it.label) }
    return if (customOptions.isEmpty()) baseGroups else baseGroups + ExpenseCategoryGroup("Eigene Kategorien", customOptions)
}

private fun expensePurposeText(expense: Expense): String {
    return listOf(expense.purpose, expense.bookingText, expense.paymentAccount)
        .filter(String::isNotBlank)
        .ifEmpty { listOf("Keine weiteren Buchungsdetails vorhanden.") }
        .joinToString("\n")
}

private fun formatDebitAmount(cents: Long): String = "-${Money.format(cents)}"

private fun relativeOrFormattedDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Heute"
        today.minusDays(1) -> "Gestern"
        else -> Money.formatDate(epochMillis)
    }
}

private fun merchantMonthlyValues(expenses: List<Expense>): List<Pair<String, Long>> {
    if (expenses.isEmpty()) return emptyList()
    val currentMonth = java.time.YearMonth.now()
    return (5 downTo 0).map { offset ->
        val month = currentMonth.minusMonths(offset.toLong())
        val value = expenses.filter {
            val itemMonth = Instant.ofEpochMilli(it.occurredAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .let { date -> java.time.YearMonth.from(date) }
            itemMonth == month
        }.sumOf { it.amountCents }
        month.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.GERMANY).take(1).uppercase(Locale.GERMANY) to value
    }
}

private fun expenseLocalDate(epochMillis: Long): LocalDate {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun expenseDateHeaderLabel(date: LocalDate): String {
    val day = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.GERMANY).replace(".", "")
    val formattedDate = date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY))
    return "$formattedDate $day"
}

private fun merchantLogoLabel(merchant: String): String {
    val normalized = merchant.trim()
    if (normalized.isBlank()) return "?"
    return when {
        normalized.contains("paypal", ignoreCase = true) -> "P"
        normalized.contains("amazon", ignoreCase = true) -> "a"
        normalized.contains("steam", ignoreCase = true) -> "S"
        normalized.contains("rewe", ignoreCase = true) -> "REWE"
        normalized.contains("netto", ignoreCase = true) -> "Netto"
        normalized.contains("scalable", ignoreCase = true) -> "S"
        else -> normalized.take(2).uppercase(Locale.GERMANY)
    }
}

private fun merchantLogoColor(merchant: String): Color {
    return when {
        merchant.contains("paypal", ignoreCase = true) -> Color(0xFF0066B3)
        merchant.contains("amazon", ignoreCase = true) -> Color(0xFF111111)
        merchant.contains("steam", ignoreCase = true) -> Color(0xFF111111)
        merchant.contains("rewe", ignoreCase = true) -> Color(0xFFE4232E)
        merchant.contains("netto", ignoreCase = true) -> Color(0xFFE6A300)
        merchant.contains("scalable", ignoreCase = true) -> Color(0xFF16B6B1)
        else -> Color(0xFFE47737)
    }
}

@Composable
private fun ExpensesScreen(
    state: SpendFoxUiState,
    padding: PaddingValues,
    viewModel: SpendFoxViewModel,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf(ExpenseExportFormat.Csv) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> viewModel.parseIngCsv(stream.readBytes()) } }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(exportFormat.mimeType)) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportExpenses(exportFormat))
            }
        }
    }
    val filtered = state.expenses.filter {
        listOf(it.merchant, it.category.label, it.customCategory, it.purpose, it.note, it.tags).joinToString(" ").contains(query, ignoreCase = true)
    }

    ListScreen(padding = padding, itemSpacing = 0.dp) {
        item {
            PrimaryActionButton("Neue Ausgabe", onAddExpense)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { importLauncher.launch("*/*") }, modifier = Modifier.weight(1f)) { Text("ING Import") }
                OutlinedButton(
                    onClick = {
                        exportFormat = ExpenseExportFormat.Csv
                        exportLauncher.launch("nutzblick-ausgaben.csv")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("CSV Export") }
                OutlinedButton(
                    onClick = {
                        exportFormat = ExpenseExportFormat.Xlsx
                        exportLauncher.launch("nutzblick-ausgaben.xlsx")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Excel") }
            }
        }
        item { SearchField(query) { query = it } }
        if (filtered.isEmpty()) {
            item { EmptyState("Keine passenden Ausgaben gefunden.") }
        } else {
            val groupedExpenses = filtered
                .sortedByDescending { it.occurredAtEpochMillis }
                .groupBy { expenseLocalDate(it.occurredAtEpochMillis) }
            groupedExpenses.forEach { (date, expensesForDate) ->
                item(key = "date-${date}") {
                    ExpenseDateHeader(date)
                }
                items(expensesForDate, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onOpen = { onOpenExpense(expense.id) },
                        onDelete = { viewModel.deleteExpense(expense.id) }
                    )
                }
            }
        }
    }
    if (state.importReviewItems.isNotEmpty()) {
        ImportReviewDialog(state.importReviewItems, viewModel::updateImportReviewItem, viewModel::confirmImport)
    }
}

@Composable
private fun ProductsScreen(
    state: SpendFoxUiState,
    padding: PaddingValues,
    viewModel: SpendFoxViewModel,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    ListScreen(padding) {
        item { PrimaryActionButton("Neues Produkt", onAddProduct) }
        if (state.products.isEmpty()) {
            item { EmptyState("Noch keine Produkte vorhanden.") }
        } else {
            items(state.products, key = { it.id }) { product ->
                ProductCard(product, onEdit = { onEditProduct(product.id) }, onDelete = { viewModel.deleteProduct(product.id) })
            }
        }
    }
}

@Composable
private fun VehiclesScreen(
    state: SpendFoxUiState,
    padding: PaddingValues,
    viewModel: SpendFoxViewModel,
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit
) {
    var selectedVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedVehicle = state.vehicles.firstOrNull { it.id == selectedVehicleId }
    if (selectedVehicle == null) {
        ListScreen(padding) {
            item { PrimaryActionButton("Neues Fahrzeug", onAddVehicle) }
            if (state.vehicles.isEmpty()) {
                item { EmptyState("Noch keine Fahrzeuge vorhanden.") }
            } else {
                items(state.vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onOpen = { selectedVehicleId = vehicle.id },
                        onEdit = { onEditVehicle(vehicle.id) },
                        onDelete = { viewModel.deleteVehicle(vehicle.id) }
                    )
                }
            }
        }
    } else {
        VehicleDetailScreen(
            padding = padding,
            vehicle = selectedVehicle,
            fuelEntries = state.fuelEntries.filter { it.vehicleId == selectedVehicle.id },
            maintenanceItems = state.maintenanceItems.filter { it.vehicleId == selectedVehicle.id },
            onBack = { selectedVehicleId = null },
            viewModel = viewModel
        )
    }
}

@Composable
private fun VehicleDetailScreen(
    padding: PaddingValues,
    vehicle: Vehicle,
    fuelEntries: List<FuelEntry>,
    maintenanceItems: List<MaintenanceItem>,
    onBack: () -> Unit,
    viewModel: SpendFoxViewModel
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFuelForm by rememberSaveable { mutableStateOf(false) }
    var showMaintenanceForm by rememberSaveable { mutableStateOf(false) }
    val fuelImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> viewModel.importFuelEntries(vehicle.id, stream.readBytes()) } }
    }
    val fuelExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(viewModel.exportFuelEntries(vehicle.id))
            }
        }
    }
    if (showFuelForm) {
        FuelEntryEditorScreen(
            padding = padding,
            vehicle = vehicle,
            onBack = { showFuelForm = false },
            onSave = { data ->
                if (viewModel.upsertFuelEntry(null, vehicle.id, data.date, data.odometerKm, data.liters, data.amount, data.fuelStation, data.fuelTypeLabel, data.note)) {
                    showFuelForm = false
                }
            }
        )
        return
    }
    if (showMaintenanceForm) {
        MaintenanceEditorScreen(
            padding = padding,
            onBack = { showMaintenanceForm = false },
            onSave = { name, stock ->
                if (viewModel.addMaintenanceItem(vehicle.id, name, stock)) showMaintenanceForm = false
            }
        )
        return
    }
    var horizontalDrag by remember { mutableStateOf(0f) }
    ListScreen(
        padding = padding,
        modifier = Modifier.pointerInput(selectedTab) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    when {
                        horizontalDrag > 90f -> selectedTab = (selectedTab - 1).coerceAtLeast(0)
                        horizontalDrag < -90f -> selectedTab = (selectedTab + 1).coerceAtMost(2)
                    }
                    horizontalDrag = 0f
                },
                onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount }
            )
        }
    ) {
        item {
            GlassCard {
                TextButton(onClick = onBack) { Text("Zurück") }
                Text(vehicle.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(listOf(vehicle.manufacturer, vehicle.modelName, vehicle.licensePlate).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("Übersicht") })
                    FilterChip(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("Tanken") })
                    FilterChip(selected = selectedTab == 2, onClick = { selectedTab = 2 }, label = { Text("Wartung") })
                }
            }
        }
        when (selectedTab) {
            0 -> {
                item { SummaryCard("Gefahrene km", fuelEntries.sumOf { it.distanceKm }.toString(), "Aus Tank-Einträgen") }
                item { SummaryCard("Ø Verbrauch", fuelEntries.averageConsumptionLabel(), "Liter pro 100 km") }
                item { SummaryCard("Tankkosten", Money.format(fuelEntries.sumOf { it.amountCents }), "Alle Tankungen") }
                item { SummaryCard("Wartungsteile", maintenanceItems.size.toString(), "Ersatzteile und Intervalle") }
            }
            1 -> {
                item { PrimaryActionButton("Tankung eintragen") { showFuelForm = true } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { fuelImportLauncher.launch("*/*") }, modifier = Modifier.weight(1f)) { Text("Import") }
                        OutlinedButton(onClick = { fuelExportLauncher.launch("${vehicle.displayName}-tankjournal.csv") }, modifier = Modifier.weight(1f)) { Text("Export") }
                    }
                }
                if (fuelEntries.isEmpty()) {
                    item { EmptyState("Noch keine Tankungen vorhanden.") }
                } else {
                    items(fuelEntries, key = { it.id }) { entry ->
                        RecordCard(
                            title = entry.fuelStation.ifBlank { Money.formatDate(entry.dateEpochMillis) },
                            value = Money.format(entry.amountCents),
                            subtitle = listOf(
                                "${entry.odometerKm} km",
                                "${entry.distanceKm} km gefahren",
                                "${entry.liters.formatLiters()} L",
                                entry.consumptionLitersPer100Km?.let { "${it.formatOneDecimal()} L/100 km" }.orEmpty()
                            ).filter(String::isNotBlank).joinToString(" · "),
                            onEdit = null,
                            onDelete = { viewModel.deleteFuelEntry(entry.id) }
                        )
                    }
                }
            }
            2 -> {
                item { PrimaryActionButton("Ersatzteil/Wartung hinzufügen") { showMaintenanceForm = true } }
                items(maintenanceItems, key = { it.id }) { item -> RecordCard(item.name, "${item.stockQuantity}x", item.note, null, null) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: SpendFoxUiState, padding: PaddingValues, viewModel: SpendFoxViewModel) {
    var selectedDetail by rememberSaveable { mutableStateOf<SettingsDetail?>(null) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(false) }
    var categoryLabel by rememberSaveable { mutableStateOf("") }
    var categoryScope by rememberSaveable { mutableStateOf(CategoryScope.Expense) }
    if (selectedDetail == null) {
        ListScreen(padding) {
            item { SectionTitle("Einstellungen") }
            items(SettingsDetail.entries, key = { it.name }) { detail ->
                SettingsRow(detail) { selectedDetail = detail }
            }
            item {
                Button(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) { Text("Ausloggen") }
            }
            item {
                Text("Version ${state.appVersion}", modifier = Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        val detail = selectedDetail ?: return
        if (detail == SettingsDetail.Profile) {
            PersonalProfileSettingsScreen(
                state = state,
                padding = padding,
                onCancel = { selectedDetail = null },
                onDone = {
                    viewModel.savePersonalProfile(it)
                    selectedDetail = null
                },
                onReset = viewModel::resetPersonalProfile,
                onDeleteRequest = viewModel::requestAccountDeletion
            )
            return
        }
        ListScreen(padding) {
            item {
                TextButton(onClick = { selectedDetail = null }) { Text("Zurück") }
            }
            item { SectionTitle(detail.title) }
            when (detail) {
                SettingsDetail.Profile -> Unit
                SettingsDetail.Notifications -> item {
                    GlassCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Wartungserinnerungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Lokale Hinweise für anstehende Wartungen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                        }
                    }
                }
                SettingsDetail.Categories -> item {
                    GlassCard {
                        CategoryChips(CategoryScope.entries, categoryScope, { categoryScope = it }) { it.label }
                        OutlinedTextField(categoryLabel, { categoryLabel = it }, label = { Text("Eigene Kategorie") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { if (viewModel.addCategory(categoryScope, categoryLabel)) categoryLabel = "" }, modifier = Modifier.fillMaxWidth()) { Text("Kategorie hinzufügen") }
                        if (state.categories.isEmpty()) {
                            Text("Noch keine eigenen Kategorien.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            state.categories.forEach { Text("${it.scope.label}: ${it.label}", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                SettingsDetail.Appearance -> item {
                    GlassCard {
                        Text("Design", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Legt fest, ob Nutzblick hell, dunkel oder nach Systemeinstellung dargestellt wird.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        CategoryChips(SpendFoxThemeMode.entries, state.themeMode, viewModel::setThemeMode) { it.label }
                    }
                }
                SettingsDetail.Security -> {
                    item {
                        GlassCard {
                            Text("Entsperrung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Nutzblick sperrt sich, sobald die App in den Hintergrund gelegt wird.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            UnlockMethod.entries.forEach { method ->
                                SelectableOptionCard(
                                    title = method.label,
                                    description = method.description,
                                    selected = state.unlockMethod == method,
                                    onClick = { viewModel.setUnlockMethod(method) }
                                )
                            }
                        }
                    }
                    item {
                        SettingsSection("Datenschutz", "Supabase-Sync nutzt dein Nutzerkonto und RLS trennt Daten pro Konto. Die Entsperr-Auswahl bleibt lokal auf diesem Gerät.")
                    }
                }
                SettingsDetail.Legal -> item {
                    SettingsSection("Rechtliche Hinweise", "Das Fahrzeugjournal ist für private Übersicht, Wartung und Kosten gedacht. Es ist nicht als steuerlich zertifiziertes Fahrtenbuch ausgewiesen.")
                }
            }
        }
    }
}

@Composable
private fun PersonalProfileSettingsScreen(
    state: SpendFoxUiState,
    padding: PaddingValues,
    onCancel: () -> Unit,
    onDone: (PersonalProfile) -> Unit,
    onReset: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val saved = state.personalProfile
    val displayName = state.snapshot.profile?.displayName.orEmpty().takeUnless { it.contains("@") }.orEmpty()
    val email = state.authStatus.session?.email ?: "Nicht angemeldet"
    var salutation by rememberSaveable(saved.salutation) { mutableStateOf(saved.salutation.ifBlank { "Herr" }) }
    var firstName by rememberSaveable(saved.firstName, displayName) { mutableStateOf(saved.firstName.ifBlank { displayName.substringBefore(" ").trim() }) }
    var lastName by rememberSaveable(saved.lastName, displayName) { mutableStateOf(saved.lastName.ifBlank { displayName.substringAfter(" ", "").trim() }) }
    var birthDate by rememberSaveable(saved.birthDate) { mutableStateOf(saved.birthDate) }
    var address by rememberSaveable(saved.address) { mutableStateOf(saved.address) }
    var mobileNumber by rememberSaveable(saved.mobileNumber) { mutableStateOf(saved.mobileNumber) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Benutzerkonto zurücksetzen?") },
            text = { Text("Dadurch werden die lokal gespeicherten persönlichen Angaben auf diesem Gerät gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    onReset()
                }) { Text("Zurücksetzen") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Abbrechen") } }
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Benutzerkonto löschen?") },
            text = { Text("Das endgültige Löschen muss später serverseitig über Supabase bestätigt werden. Nutzblick merkt diese Aktion deshalb nur vor.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDeleteRequest()
                }) { Text("Löschen vormerken") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Abbrechen") } }
        )
    }

    ListScreen(padding) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                    Text("Abbrechen", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = {
                        onDone(
                            PersonalProfile(
                                salutation = salutation,
                                firstName = firstName,
                                lastName = lastName,
                                birthDate = birthDate,
                                address = address,
                                mobileNumber = mobileNumber
                            )
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                ) {
                    Text("Fertig", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            GlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Dein Konto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    VerifiedBadge()
                }
                ProfileInputField(
                    label = "E-Mail-Adresse",
                    value = email,
                    onValueChange = {},
                    readOnly = true
                )
                Text(
                    "Deine E-Mail-Adresse dient zur Synchronisation und Wiederherstellung deines Benutzerkontos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            GlassCard {
                Text("Persönliche Daten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Frau", "Herr", "Divers").forEach { option ->
                        ProfileChoicePill(
                            label = option,
                            selected = salutation == option,
                            onClick = { salutation = option }
                        )
                    }
                }
                ProfileInputField("Vorname", firstName, { firstName = it }, singleLine = true)
                ProfileInputField("Nachname", lastName, { lastName = it }, singleLine = true)
                ProfileInputField(
                    label = "Geburtsdatum",
                    value = birthDate,
                    onValueChange = { birthDate = formatGermanDateInput(it) },
                    keyboardType = KeyboardType.Number,
                    trailingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true
                )
                ProfileInputField(
                    label = "Adresse hinzufügen",
                    value = address,
                    onValueChange = { address = it },
                    trailingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true
                )
                ProfileInputField(
                    label = "Mobilfunknummer",
                    value = mobileNumber,
                    onValueChange = { mobileNumber = filterPhoneInput(it) },
                    keyboardType = KeyboardType.Phone,
                    singleLine = true
                )
            }
        }
        item {
            DangerSettingsCard(
                title = "Benutzerkonto zurücksetzen",
                body = "Dadurch werden alle Einstellungen und Änderungen gelöscht. Deine verknüpften Konten und die zugehörigen Buchungen bleiben erhalten.",
                buttonLabel = "Benutzerkonto zurücksetzen",
                onClick = { showResetConfirmation = true }
            )
        }
        item {
            DangerSettingsCard(
                title = "Benutzerkonto löschen",
                body = "Alle deine Daten werden unwiderruflich gelöscht und du musst dich erneut registrieren, um Nutzblick nutzen zu können.",
                buttonLabel = "Benutzerkonto löschen",
                onClick = { showDeleteConfirmation = true }
            )
        }
        if (state.message.isNotBlank()) {
            item { MessageText(state.message) }
        }
    }
}

@Composable
private fun SpendFoxTopBar(
    title: String,
    showBack: Boolean,
    onNavigationClick: () -> Unit,
    action: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        val chromeColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkThemeActive()) 0.92f else 0.88f)
        Surface(
            modifier = Modifier.align(Alignment.CenterStart),
            shape = RoundedCornerShape(16.dp),
            color = chromeColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        ) {
            if (showBack) {
                TextButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.height(42.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Zurück", fontWeight = FontWeight.SemiBold)
                }
            } else {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Rounded.List, contentDescription = "Bereiche öffnen")
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = RoundedCornerShape(18.dp),
            color = chromeColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            action()
        }
    }
}

@Composable
private fun GlassNavigationBar(selectedTab: MainTab?, onSelected: (MainTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, top = 2.dp, end = 18.dp, bottom = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        val barAlpha = if (isDarkThemeActive()) 0.94f else 0.92f
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = barAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = if (isDarkThemeActive()) 0.dp else 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelected(tab) }
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(18.dp))
                        Text(
                            tab.label,
                            color = tint,
                            fontSize = 8.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterScreen(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ListScreen(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    itemSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .spendFoxLazyScrollbar(listState),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        content = content
    )
}

@Composable
private fun Modifier.spendFoxLazyScrollbar(state: LazyListState): Modifier {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkThemeActive()) 0.82f else 0.68f)
    val thumbWidth = 3.dp
    val endPadding = 4.dp
    val minThumbHeight = 38.dp
    return drawWithContent {
        drawContent()
        val visibleItems = state.layoutInfo.visibleItemsInfo
        val totalItems = state.layoutInfo.totalItemsCount
        if (visibleItems.isEmpty() || totalItems <= visibleItems.size) return@drawWithContent
        val averageItemHeight = visibleItems.map { it.size }.average().toFloat().coerceAtLeast(1f)
        val estimatedContentHeight = (averageItemHeight * totalItems).coerceAtLeast(size.height + 1f)
        val maxScroll = (estimatedContentHeight - size.height).coerceAtLeast(1f)
        val scrollOffset = state.firstVisibleItemIndex * averageItemHeight + state.firstVisibleItemScrollOffset
        val thumbHeight = (size.height * size.height / estimatedContentHeight).coerceIn(minThumbHeight.toPx(), size.height)
        val thumbTop = ((scrollOffset / maxScroll).coerceIn(0f, 1f) * (size.height - thumbHeight)).coerceIn(0f, size.height - thumbHeight)
        val widthPx = thumbWidth.toPx()
        val left = size.width - widthPx - endPadding.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(left, thumbTop),
            size = Size(widthPx, thumbHeight),
            cornerRadius = CornerRadius(widthPx, widthPx)
        )
    }
}

@Composable
private fun Modifier.spendFoxVerticalScrollbar(state: ScrollState): Modifier {
    val color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkThemeActive()) 0.82f else 0.68f)
    val thumbWidth = 3.dp
    val endPadding = 4.dp
    val minThumbHeight = 38.dp
    return drawWithContent {
        drawContent()
        val maxScroll = state.maxValue.toFloat()
        if (maxScroll <= 0f) return@drawWithContent
        val estimatedContentHeight = size.height + maxScroll
        val thumbHeight = (size.height * size.height / estimatedContentHeight).coerceIn(minThumbHeight.toPx(), size.height)
        val thumbTop = ((state.value / maxScroll).coerceIn(0f, 1f) * (size.height - thumbHeight)).coerceIn(0f, size.height - thumbHeight)
        val widthPx = thumbWidth.toPx()
        val left = size.width - widthPx - endPadding.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(left, thumbTop),
            size = Size(widthPx, thumbHeight),
            cornerRadius = CornerRadius(widthPx, widthPx)
        )
    }
}

@Composable
private fun PrimaryActionButton(label: String, onClick: () -> Unit) {
    FeedbackButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPressed) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
            contentColor = if (isPressed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Suchen") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp))
}

@Composable
private fun SummaryCard(title: String, value: String, caption: String) {
    GlassCard {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureShortcutCard(tab: WorkspaceFeature, value: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(tab.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tab.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun DestinationShortcutCard(title: String, caption: String, icon: ImageVector, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (caption.isNotBlank()) {
                        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ExpenseDateHeader(date: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkThemeActive()) 0.82f else 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(0.dp)
    ) {
        Text(
            text = expenseDateHeaderLabel(date),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExpenseCard(expense: Expense, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpenseMerchantIcon(expense.merchant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    expense.merchant,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    expense.customCategory.ifBlank { expense.category.label },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            ExpenseAmountLabel(expense.amountCents)
        }
    }
}

@Composable
private fun ExpenseMerchantIcon(merchant: String) {
    val label = merchantLogoLabel(merchant)
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        contentColor = merchantLogoColor(merchant),
        border = BorderStroke(1.dp, Color(0xFFE7E7E7))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ExpenseAmountLabel(amountCents: Long) {
    val isPositive = amountCents < 0L
    val amount = Money.format(kotlin.math.abs(amountCents))
    if (isPositive) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0E3D21),
            contentColor = Color(0xFF65E08A)
        ) {
            Text(
                amount,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
    } else {
        Text(
            "-$amount",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}

@Composable
private fun ProductCard(product: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    val monthly = product.monthlyCostCents?.let(Money::format) ?: "—"
    RecordCard(product.name, monthly, "Kaufpreis ${Money.format(product.purchasePriceCents)} · ${product.customCategory.ifBlank { product.category.label }}", onEdit, onDelete)
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(vehicle.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(listOf(vehicle.manufacturer, vehicle.modelName, vehicle.licensePlate).filter(String::isNotBlank).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Build, contentDescription = "Bearbeiten") }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun RecordCard(title: String, value: String, subtitle: String, onEdit: (() -> Unit)?, onDelete: (() -> Unit)?, editLabel: String = "Bearbeiten") {
    val cardModifier = if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier
    GlassCard(modifier = cardModifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        if (onEdit != null || onDelete != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onEdit != null) TextButton(onClick = onEdit) { Text(editLabel) }
                if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    GlassCard { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
}

@Composable
private fun SettingsSection(title: String, body: String) {
    GlassCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerifiedBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF153B24),
        contentColor = Color(0xFF5CE073)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(shape = CircleShape, color = Color(0xFF5CE073), contentColor = Color(0xFF153B24), modifier = Modifier.size(18.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Verifiziert", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        ) {
            if (selected) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val fieldColor = if (isDarkThemeActive()) Color(0xFF333335) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = readOnly,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldColor,
            unfocusedContainerColor = fieldColor,
            disabledContainerColor = fieldColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun DangerSettingsCard(title: String, body: String, buttonLabel: String, onClick: () -> Unit) {
    GlassCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B2422),
                contentColor = Color(0xFFFF6B68)
            )
        ) {
            Text(buttonLabel, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsRow(detail: SettingsDetail, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkThemeActive()) 0.92f else 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(detail.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SelectableOptionCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkThemeActive()) 0.92f else 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {}
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cardAlpha = if (isDarkThemeActive()) 0.92f else 0.94f
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = cardAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun EditorScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState)
            .spendFoxVerticalScrollbar(scrollState)
            .padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Abbrechen")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("Sichern")
            }
        }
    }
}

@Composable
private fun ExpenseEditorScreen(
    expense: Expense?,
    categories: List<UserCategory>,
    padding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ExpenseEditorData) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf(expense?.amountCents?.let(::formatCurrencyFromCents).orEmpty()) }
    var merchant by rememberSaveable { mutableStateOf(expense?.merchant.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(expense?.category ?: ExpenseCategory.Other) }
    var customCategory by rememberSaveable { mutableStateOf(expense?.customCategory.orEmpty()) }
    var dateText by rememberSaveable { mutableStateOf(expense?.occurredAtEpochMillis?.let(::localDateText) ?: LocalDate.now().toString()) }
    var paymentAccount by rememberSaveable { mutableStateOf(expense?.paymentAccount.orEmpty()) }
    var bookingText by rememberSaveable { mutableStateOf(expense?.bookingText.orEmpty()) }
    var purpose by rememberSaveable { mutableStateOf(expense?.purpose.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(expense?.note.orEmpty()) }
    var tags by rememberSaveable { mutableStateOf(expense?.tags.orEmpty()) }
    EditorScreen(
        padding = padding,
        onBack = onBack,
        onSave = { onSave(ExpenseEditorData(amount, merchant, category, customCategory, parseLocalDate(dateText), paymentAccount, bookingText, purpose, note, tags)) }
    ) {
        Text("Basis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(amount, { amount = formatCurrencyInput(it, amount) }, label = { Text("Betrag") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Händler") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        CategoryChips(ExpenseCategory.entries, category, { category = it }) { it.label }
        if (categories.isNotEmpty()) UserCategoryChips(categories, customCategory) { customCategory = it.label }
        OutlinedTextField(customCategory, { customCategory = it }, label = { Text("Eigene Kategorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dateText, { dateText = formatDateInput(it) }, label = { Text("Datum yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Bankdaten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(paymentAccount, { paymentAccount = it }, label = { Text("Konto/Zahlung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(bookingText, { bookingText = it }, label = { Text("Buchungstext") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(purpose, { purpose = it }, label = { Text("Verwendungszweck") }, modifier = Modifier.fillMaxWidth())
        Text("Notizen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(tags, { tags = it }, label = { Text("Tags") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProductEditorScreen(
    product: Product?,
    categories: List<UserCategory>,
    padding: PaddingValues,
    onBack: () -> Unit,
    onSave: (ProductEditorData) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(product?.name.orEmpty()) }
    var manufacturer by rememberSaveable { mutableStateOf(product?.manufacturer.orEmpty()) }
    var modelName by rememberSaveable { mutableStateOf(product?.modelName.orEmpty()) }
    var serialReference by rememberSaveable { mutableStateOf(product?.serialReference.orEmpty()) }
    var price by rememberSaveable { mutableStateOf(product?.purchasePriceCents?.let(::formatCurrencyFromCents).orEmpty()) }
    var category by rememberSaveable { mutableStateOf(product?.category ?: ProductCategory.Technology) }
    var customCategory by rememberSaveable { mutableStateOf(product?.customCategory.orEmpty()) }
    var purchasedAt by rememberSaveable { mutableStateOf(product?.purchasedAtEpochMillis?.let(::localDateText) ?: LocalDate.now().toString()) }
    var usageMonths by rememberSaveable { mutableStateOf(product?.usageDurationMonths?.takeIf { it > 0 }?.toString().orEmpty()) }
    var warrantyUntil by rememberSaveable { mutableStateOf(product?.warrantyUntilEpochMillis?.let(::localDateText).orEmpty()) }
    var note by rememberSaveable { mutableStateOf(product?.note.orEmpty()) }
    EditorScreen(
        padding = padding,
        onBack = onBack,
        onSave = { onSave(ProductEditorData(name, manufacturer, modelName, serialReference, price, category, customCategory, parseLocalDate(purchasedAt), usageMonths, warrantyUntil.takeIf(String::isNotBlank)?.let(::parseLocalDate), note)) }
    ) {
        Text("Produkt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(manufacturer, { manufacturer = it }, label = { Text("Hersteller") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(modelName, { modelName = it }, label = { Text("Modell") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(serialReference, { serialReference = it }, label = { Text("Seriennummer/Referenz") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        CategoryChips(ProductCategory.entries, category, { category = it }) { it.label }
        if (categories.isNotEmpty()) UserCategoryChips(categories, customCategory) { customCategory = it.label }
        OutlinedTextField(customCategory, { customCategory = it }, label = { Text("Eigene Kategorie") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Kosten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(price, { price = formatCurrencyInput(it, price) }, label = { Text("Kaufpreis") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(usageMonths, { usageMonths = filterIntegerInput(it) }, label = { Text("Nutzungsdauer in Monaten") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(purchasedAt, { purchasedAt = formatDateInput(it) }, label = { Text("Kaufdatum yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(warrantyUntil, { warrantyUntil = formatDateInput(it) }, label = { Text("Garantie bis yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun VehicleEditorScreen(
    vehicle: Vehicle?,
    padding: PaddingValues,
    onBack: () -> Unit,
    onSave: (String, String, String, String, FuelType) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(vehicle?.displayName.orEmpty()) }
    var manufacturer by rememberSaveable { mutableStateOf(vehicle?.manufacturer.orEmpty()) }
    var model by rememberSaveable { mutableStateOf(vehicle?.modelName.orEmpty()) }
    var plate by rememberSaveable { mutableStateOf(vehicle?.licensePlate.orEmpty()) }
    var fuelType by rememberSaveable { mutableStateOf(vehicle?.fuelType ?: FuelType.Petrol) }
    EditorScreen(
        padding = padding,
        onBack = onBack,
        onSave = { onSave(name, manufacturer, model, plate, fuelType) }
    ) {
        Text("Fahrzeug", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(manufacturer, { manufacturer = it }, label = { Text("Hersteller") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(model, { model = it }, label = { Text("Modell") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(plate, { plate = it }, label = { Text("Kennzeichen") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        CategoryChips(FuelType.entries, fuelType, { fuelType = it }) { it.label }
    }
}

private data class ExpenseEditorData(
    val amount: String,
    val merchant: String,
    val category: ExpenseCategory,
    val customCategory: String,
    val date: LocalDate,
    val paymentAccount: String,
    val bookingText: String,
    val purpose: String,
    val note: String,
    val tags: String
)

private data class ProductEditorData(
    val name: String,
    val manufacturer: String,
    val modelName: String,
    val serialReference: String,
    val price: String,
    val category: ProductCategory,
    val customCategory: String,
    val purchasedAt: LocalDate,
    val usageMonths: String,
    val warrantyUntil: LocalDate?,
    val note: String
)

private data class FuelEntryEditorData(
    val date: LocalDate,
    val odometerKm: String,
    val liters: String,
    val amount: String,
    val fuelStation: String,
    val fuelTypeLabel: String,
    val note: String
)

@Composable
private fun FuelEntryEditorScreen(
    padding: PaddingValues,
    vehicle: Vehicle,
    onBack: () -> Unit,
    onSave: (FuelEntryEditorData) -> Unit
) {
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var odometer by rememberSaveable { mutableStateOf("") }
    var liters by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var station by rememberSaveable { mutableStateOf("") }
    var fuelType by rememberSaveable { mutableStateOf(vehicle.fuelType.label) }
    var note by rememberSaveable { mutableStateOf("") }
    EditorScreen(
        padding = padding,
        onBack = onBack,
        onSave = { onSave(FuelEntryEditorData(parseLocalDate(dateText), odometer, liters, amount, station, fuelType, note)) }
    ) {
        Text("Tankung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(dateText, { dateText = formatDateInput(it) }, label = { Text("Datum yyyy-mm-dd") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(odometer, { odometer = filterIntegerInput(it) }, label = { Text("Kilometerstand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(liters, { liters = filterDecimalInput(it, liters, maxDecimals = 2) }, label = { Text("Getankt/L") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = formatCurrencyInput(it, amount) }, label = { Text("Betrag") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(station, { station = it }, label = { Text("Tankstelle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(fuelType, { fuelType = it }, label = { Text("Kraftstoff") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MaintenanceEditorScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("0") }
    EditorScreen(
        padding = padding,
        onBack = onBack,
        onSave = { onSave(name, stock) }
    ) {
        Text("Ersatzteil/Wartung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(name, { name = it }, label = { Text("Teil/Service") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(stock, { stock = filterIntegerInput(it) }, label = { Text("Bestand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ImportReviewDialog(items: List<ExpenseImportReviewItem>, onUpdate: (ExpenseImportReviewItem) -> Unit, onConfirm: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = {},
        title = { Text("ING Import prüfen") },
        text = {
            Column(
                Modifier
                    .verticalScroll(scrollState)
                    .spendFoxVerticalScrollbar(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.take(30).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.shouldImport && !item.isDuplicate, onCheckedChange = { onUpdate(item.copy(shouldImport = it == true)) }, enabled = !item.isDuplicate)
                        Column {
                            Text(item.merchant, fontWeight = FontWeight.SemiBold)
                            Text("${Money.format(item.amountCents)} · ${item.suggestedCategory.label}${if (item.isDuplicate) " · Duplikat" else ""}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (items.size > 30) Text("${items.size - 30} weitere Einträge werden ebenfalls übernommen, wenn aktiviert.")
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Import speichern") } }
    )
}

@Composable
private fun <T> CategoryChips(options: List<T>, selected: T, onSelected: (T) -> Unit, label: (T) -> String) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelected(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun UserCategoryChips(options: List<UserCategory>, selectedLabel: String, onSelected: (UserCategory) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selectedLabel == option.label,
                onClick = { onSelected(option) },
                label = { Text(option.label) }
            )
        }
    }
}

@Composable
private fun MessageText(message: String) {
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
}

private fun localDateText(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

private fun parseLocalDate(value: String): LocalDate {
    return runCatching { LocalDate.parse(value.trim()) }.getOrDefault(LocalDate.now())
}

private fun List<FuelEntry>.averageConsumptionLabel(): String {
    val values = mapNotNull { it.consumptionLitersPer100Km }
    return if (values.isEmpty()) "—" else "${values.average().formatOneDecimal()} L"
}

private fun Double.formatLiters(): String = "%.2f".format(Locale.GERMANY, this)

private fun Double.formatOneDecimal(): String = "%.1f".format(Locale.GERMANY, this)

@Composable
private fun isDarkThemeActive(): Boolean = MaterialTheme.colorScheme.background == Color.Black

@Composable
private fun nutzblickBackdrop(): Brush {
    return if (isDarkThemeActive()) {
        Brush.verticalGradient(
            colors = listOf(Color.Black, Color.Black)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF4F7F2), Color(0xFFE7EFE8), Color(0xFFFFE4D3))
        )
    }
}
