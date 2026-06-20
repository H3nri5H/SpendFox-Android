package de.h3nri5h.spendfox.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.h3nri5h.spendfox.BuildConfig
import de.h3nri5h.spendfox.data.CategoryScope
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelEntry
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.LOCAL_USER_ID
import de.h3nri5h.spendfox.data.MaintenanceItem
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.SpendFoxRepository
import de.h3nri5h.spendfox.data.SpendFoxSnapshot
import de.h3nri5h.spendfox.data.SyncState
import de.h3nri5h.spendfox.data.Trip
import de.h3nri5h.spendfox.data.UserCategory
import de.h3nri5h.spendfox.data.Vehicle
import de.h3nri5h.spendfox.data.auth.AuthRepository
import de.h3nri5h.spendfox.data.auth.AuthStatus
import de.h3nri5h.spendfox.data.auth.RegistrationResult
import de.h3nri5h.spendfox.data.exporting.ExpenseExportFormat
import de.h3nri5h.spendfox.data.exporting.ExpenseExporter
import de.h3nri5h.spendfox.data.exporting.FuelLogCsv
import de.h3nri5h.spendfox.data.importing.ExpenseImportReviewItem
import de.h3nri5h.spendfox.data.importing.IngCsvParser
import de.h3nri5h.spendfox.data.local.SpendFoxDatabase
import de.h3nri5h.spendfox.data.sync.SyncRepository
import de.h3nri5h.spendfox.domain.Money
import de.h3nri5h.spendfox.ui.theme.SpendFoxThemeMode
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UnlockMethod(val label: String, val description: String) {
    Biometric("Face ID/Fingerabdruck", "Biometrie bevorzugen, Geräteentsperrung bleibt als System-Fallback möglich."),
    DeviceCredential("Handy-PIN/Muster", "Nur die Geräte-PIN, das Muster oder Passwort des Handys verwenden.")
}

data class PersonalProfile(
    val salutation: String = "Herr",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val address: String = "",
    val mobileNumber: String = ""
)

private object SpendFoxPreferenceKeys {
    const val THEME_MODE = "theme_mode"
    const val UNLOCK_METHOD = "unlock_method"
    const val PROFILE_SALUTATION = "profile_salutation"
    const val PROFILE_FIRST_NAME = "profile_first_name"
    const val PROFILE_LAST_NAME = "profile_last_name"
    const val PROFILE_BIRTH_DATE = "profile_birth_date"
    const val PROFILE_ADDRESS = "profile_address"
    const val PROFILE_MOBILE_NUMBER = "profile_mobile_number"
}

class SpendFoxViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("spendfox_settings", Context.MODE_PRIVATE)
    private val authRepository = AuthRepository(application.applicationContext)
    private val syncRepository = SyncRepository(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY,
        accessTokenProvider = authRepository::currentAccessToken
    )
    private val database = SpendFoxDatabase.create(application.applicationContext)
    private val repository = SpendFoxRepository(database.dao(), syncRepository)
    private val ingCsvParser = IngCsvParser()
    private val expenseExporter = ExpenseExporter()
    private val fuelLogCsv = FuelLogCsv()

    private val _state = MutableStateFlow(
        SpendFoxUiState(
            themeMode = readThemeMode(),
            unlockMethod = readUnlockMethod(),
            personalProfile = readPersonalProfile()
        )
    )
    val state: StateFlow<SpendFoxUiState> = _state.asStateFlow()
    private var snapshotJob: Job? = null

    init {
        viewModelScope.launch {
            combine(authRepository.status, syncRepository.status) { auth, sync ->
                auth to sync
            }.collectLatest { (auth, sync) ->
                val session = auth.session
                val userId = session?.userId ?: LOCAL_USER_ID
                val mode = when {
                    session == null -> AppMode.LoggedOut
                    !_state.value.isUnlocked -> AppMode.Locked
                    else -> AppMode.Unlocked
                }
                _state.value = _state.value.copy(
                    authStatus = auth,
                    syncMessage = sync.message,
                    activeUserId = userId,
                    mode = mode,
                    appVersion = BuildConfig.VERSION_NAME
                )
                observeSnapshot(userId)
                withContext(Dispatchers.IO) {
                    repository.seedIfEmpty(userId)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = "")
            val result = authRepository.login(email, password)
            _state.value = _state.value.copy(isBusy = false)
            handleAuthResult(result)
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = "")
            val result = authRepository.register(email, password)
            _state.value = _state.value.copy(isBusy = false)
            handleRegistrationResult(result)
        }
    }

    fun verifyRegistrationCode(code: String) {
        val email = _state.value.pendingRegistrationEmail
        if (email.isNullOrBlank()) {
            _state.value = _state.value.copy(message = "Bitte Registrierung erneut starten.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = "")
            val result = authRepository.verifyRegistrationCode(email, code)
            _state.value = _state.value.copy(isBusy = false)
            handleAuthResult(result)
        }
    }

    fun resendRegistrationCode() {
        val email = _state.value.pendingRegistrationEmail
        if (email.isNullOrBlank()) {
            _state.value = _state.value.copy(message = "Bitte Registrierung erneut starten.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = "")
            authRepository.resendRegistrationCode(email)
                .onSuccess { _state.value = _state.value.copy(isBusy = false, message = "Neuer Code wurde angefordert.") }
                .onFailure { _state.value = _state.value.copy(isBusy = false, message = it.message.orEmpty()) }
        }
    }

    fun cancelRegistrationVerification() {
        _state.value = _state.value.copy(pendingRegistrationEmail = null, message = "")
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            authRepository.forgotPassword(email)
                .onSuccess { _state.value = _state.value.copy(message = "Passwort-Link vorbereitet.") }
                .onFailure { _state.value = _state.value.copy(message = it.message.orEmpty()) }
        }
    }

    fun socialLoginUnavailable(provider: String) {
        _state.value = _state.value.copy(message = "$provider Login ist vorbereitet, aber noch nicht konfiguriert.")
    }

    fun setUnlockMethod(method: UnlockMethod) {
        preferences.edit().putString(SpendFoxPreferenceKeys.UNLOCK_METHOD, method.name).apply()
        _state.value = _state.value.copy(unlockMethod = method)
    }

    fun setThemeMode(mode: SpendFoxThemeMode) {
        preferences.edit().putString(SpendFoxPreferenceKeys.THEME_MODE, mode.name).apply()
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun savePersonalProfile(profile: PersonalProfile) {
        preferences.edit()
            .putString(SpendFoxPreferenceKeys.PROFILE_SALUTATION, profile.salutation)
            .putString(SpendFoxPreferenceKeys.PROFILE_FIRST_NAME, profile.firstName.trim())
            .putString(SpendFoxPreferenceKeys.PROFILE_LAST_NAME, profile.lastName.trim())
            .putString(SpendFoxPreferenceKeys.PROFILE_BIRTH_DATE, profile.birthDate.trim())
            .putString(SpendFoxPreferenceKeys.PROFILE_ADDRESS, profile.address.trim())
            .putString(SpendFoxPreferenceKeys.PROFILE_MOBILE_NUMBER, profile.mobileNumber.trim())
            .apply()
        _state.value = _state.value.copy(
            personalProfile = profile.copy(
                firstName = profile.firstName.trim(),
                lastName = profile.lastName.trim(),
                birthDate = profile.birthDate.trim(),
                address = profile.address.trim(),
                mobileNumber = profile.mobileNumber.trim()
            ),
            message = "Persönliche Angaben gespeichert."
        )
    }

    fun resetPersonalProfile() {
        val reset = PersonalProfile()
        preferences.edit()
            .remove(SpendFoxPreferenceKeys.PROFILE_SALUTATION)
            .remove(SpendFoxPreferenceKeys.PROFILE_FIRST_NAME)
            .remove(SpendFoxPreferenceKeys.PROFILE_LAST_NAME)
            .remove(SpendFoxPreferenceKeys.PROFILE_BIRTH_DATE)
            .remove(SpendFoxPreferenceKeys.PROFILE_ADDRESS)
            .remove(SpendFoxPreferenceKeys.PROFILE_MOBILE_NUMBER)
            .apply()
        _state.value = _state.value.copy(personalProfile = reset, message = "Persönliche Angaben zurückgesetzt.")
    }

    fun requestAccountDeletion() {
        _state.value = _state.value.copy(message = "Kontolöschung ist vorbereitet und braucht später eine bestätigte Supabase-Löschfunktion.")
    }

    fun deviceUnlockSucceeded() {
        _state.value = _state.value.copy(isUnlocked = true, mode = AppMode.Unlocked, message = "")
    }

    fun deviceUnlockFailed(message: String) {
        _state.value = _state.value.copy(message = message)
    }

    fun logout() {
        val userId = _state.value.authStatus.session?.userId
        if (userId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearAccountData(userId)
            }
        }
        authRepository.logout()
        _state.value = SpendFoxUiState(themeMode = readThemeMode(), unlockMethod = readUnlockMethod(), personalProfile = readPersonalProfile())
    }

    fun lockForPrivacy() {
        if (_state.value.authStatus.session != null && _state.value.mode == AppMode.Unlocked) {
            _state.value = _state.value.copy(isUnlocked = false, mode = AppMode.Locked)
        }
    }

    fun upsertExpense(
        existingId: String?,
        amount: String,
        merchant: String,
        category: ExpenseCategory,
        customCategory: String,
        date: LocalDate,
        paymentAccount: String,
        bookingText: String,
        purpose: String,
        note: String,
        tags: String
    ): Boolean {
        val cents = Money.centsFrom(amount) ?: return false
        val trimmedMerchant = merchant.trim().ifBlank { return false }
        val now = System.currentTimeMillis()
        val existing = _state.value.expenses.firstOrNull { it.id == existingId }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertExpense(
                Expense(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    amountCents = cents,
                    merchant = trimmedMerchant,
                    category = category,
                    occurredAtEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    note = note.trim(),
                    customCategory = customCategory.trim(),
                    paymentAccount = paymentAccount.trim(),
                    bookingText = bookingText.trim(),
                    purpose = purpose.trim(),
                    tags = tags.trim(),
                    sourceType = existing?.sourceType.orEmpty(),
                    sourceHash = existing?.sourceHash.orEmpty(),
                    importedAtEpochMillis = existing?.importedAtEpochMillis,
                    userId = activeUserId(),
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    syncState = SyncState.PendingUpsert
                )
            )
        }
        return true
    }

    fun updateExpenseCategory(expenseId: String, category: ExpenseCategory, customCategory: String): Boolean {
        val existing = _state.value.expenses.firstOrNull { it.id == expenseId } ?: return false
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertExpense(
                existing.copy(
                    category = category,
                    customCategory = customCategory.trim(),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                    syncState = SyncState.PendingUpsert
                )
            )
        }
        return true
    }

    fun addProduct(
        existingId: String?,
        name: String,
        manufacturer: String,
        modelName: String,
        serialReference: String,
        price: String,
        category: ProductCategory,
        customCategory: String,
        purchasedAt: LocalDate,
        usageDurationMonths: String,
        warrantyUntil: LocalDate?,
        note: String
    ): Boolean {
        val cents = Money.centsFrom(price) ?: return false
        val trimmedName = name.trim().ifBlank { return false }
        val months = usageDurationMonths.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val now = System.currentTimeMillis()
        val existing = _state.value.products.firstOrNull { it.id == existingId }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertProduct(
                Product(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    name = trimmedName,
                    manufacturer = manufacturer.trim(),
                    purchasePriceCents = cents,
                    purchasedAtEpochMillis = purchasedAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    category = category,
                    note = note.trim(),
                    customCategory = customCategory.trim(),
                    modelName = modelName.trim(),
                    serialReference = serialReference.trim(),
                    usageDurationMonths = months,
                    warrantyUntilEpochMillis = warrantyUntil?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                    userId = activeUserId(),
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    syncState = SyncState.PendingUpsert
                )
            )
        }
        return true
    }

    fun addVehicle(existingId: String?, name: String, manufacturer: String, model: String, plate: String, fuelType: FuelType): Boolean {
        val trimmedName = name.trim().ifBlank { return false }
        val now = System.currentTimeMillis()
        val existing = _state.value.vehicles.firstOrNull { it.id == existingId }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertVehicle(
                Vehicle(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    displayName = trimmedName,
                    manufacturer = manufacturer.trim(),
                    modelName = model.trim(),
                    licensePlate = plate.trim(),
                    fuelType = fuelType,
                    userId = activeUserId(),
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    syncState = SyncState.PendingUpsert
                )
            )
        }
        return true
    }

    fun addTrip(vehicleId: String, startKm: String, endKm: String, purpose: String): Boolean {
        val start = startKm.toLongOrNull() ?: return false
        val end = endKm.toLongOrNull() ?: return false
        if (end < start || vehicleId.isBlank() || purpose.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertTrip(
                Trip(
                    vehicleId = vehicleId,
                    dateEpochMillis = System.currentTimeMillis(),
                    startOdometerKm = start,
                    endOdometerKm = end,
                    purpose = purpose.trim(),
                    userId = activeUserId()
                )
            )
        }
        return true
    }

    fun upsertFuelEntry(
        existingId: String?,
        vehicleId: String,
        date: LocalDate,
        odometerKm: String,
        liters: String,
        amount: String,
        fuelStation: String,
        fuelTypeLabel: String,
        note: String
    ): Boolean {
        val odometer = odometerKm.toLongOrNull() ?: return false
        val litersValue = liters.replace(",", ".").toDoubleOrNull() ?: return false
        val amountCents = Money.centsFrom(amount) ?: return false
        if (vehicleId.isBlank() || litersValue <= 0.0) return false
        val existing = _state.value.fuelEntries.firstOrNull { it.id == existingId }
        val previousOdometer = _state.value.fuelEntries
            .filter { it.vehicleId == vehicleId && it.id != existingId && it.odometerKm <= odometer }
            .maxByOrNull { it.odometerKm }
            ?.odometerKm
        val distance = previousOdometer?.let { (odometer - it).coerceAtLeast(0) } ?: existing?.distanceKm ?: 0
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertFuelEntry(
                FuelEntry(
                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                    vehicleId = vehicleId,
                    dateEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    odometerKm = odometer,
                    distanceKm = distance,
                    liters = litersValue,
                    amountCents = amountCents,
                    fuelStation = fuelStation.trim(),
                    fuelTypeLabel = fuelTypeLabel.trim(),
                    note = note.trim(),
                    sourceType = existing?.sourceType.orEmpty(),
                    sourceHash = existing?.sourceHash.orEmpty(),
                    userId = activeUserId(),
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    syncState = SyncState.PendingUpsert
                )
            )
        }
        return true
    }

    fun addMaintenanceItem(vehicleId: String, name: String, stockQuantity: String): Boolean {
        val stock = stockQuantity.toIntOrNull()?.coerceAtLeast(0) ?: 0
        if (vehicleId.isBlank() || name.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertMaintenanceItem(
                MaintenanceItem(
                    vehicleId = vehicleId,
                    name = name.trim(),
                    stockQuantity = stock,
                    userId = activeUserId()
                )
            )
        }
        return true
    }

    fun addCategory(scope: CategoryScope, label: String): Boolean {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return false
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertCategory(UserCategory(scope = scope, label = trimmed, userId = activeUserId()))
        }
        return true
    }

    fun deleteExpense(id: String) = viewModelScope.launch(Dispatchers.IO) { repository.deleteExpense(id) }
    fun deleteProduct(id: String) = viewModelScope.launch(Dispatchers.IO) { repository.deleteProduct(id) }
    fun deleteVehicle(id: String) = viewModelScope.launch(Dispatchers.IO) { repository.deleteVehicle(id) }
    fun deleteFuelEntry(id: String) = viewModelScope.launch(Dispatchers.IO) { repository.deleteFuelEntry(id) }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAccountData(activeUserId())
        }
    }

    fun parseIngCsv(bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.Default) {
            _state.value = _state.value.copy(isBusy = true, message = "")
            val existing = _state.value.expenses.mapNotNull { it.sourceHash.takeIf(String::isNotBlank) }.toSet()
            val reviewItems = ingCsvParser.parse(bytes, existing)
            _state.value = _state.value.copy(isBusy = false, importReviewItems = reviewItems, message = "${reviewItems.size} Ausgaben gefunden.")
        }
    }

    fun updateImportReviewItem(item: ExpenseImportReviewItem) {
        _state.value = _state.value.copy(
            importReviewItems = _state.value.importReviewItems.map { if (it.importId == item.importId) item else it }
        )
    }

    fun confirmImport() {
        val expenses = _state.value.importReviewItems
            .filter { it.shouldImport && !it.isDuplicate }
            .map { it.toExpense(activeUserId()) }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isBusy = true, message = "")
            repository.upsertExpenses(expenses)
            _state.value = _state.value.copy(isBusy = false, importReviewItems = emptyList(), message = "${expenses.size} Ausgaben importiert.")
        }
    }

    fun exportExpenses(format: ExpenseExportFormat): ByteArray {
        return expenseExporter.export(_state.value.expenses, format)
    }

    fun importFuelEntries(vehicleId: String, bytes: ByteArray) {
        viewModelScope.launch(Dispatchers.Default) {
            _state.value = _state.value.copy(isBusy = true, message = "")
            val existingHashes = _state.value.fuelEntries
                .filter { it.vehicleId == vehicleId }
                .mapNotNull { it.sourceHash.takeIf(String::isNotBlank) }
                .toSet()
            val result = fuelLogCsv.parse(bytes, vehicleId, activeUserId(), existingHashes)
            withContext(Dispatchers.IO) {
                repository.upsertFuelEntries(result.entries)
            }
            _state.value = _state.value.copy(isBusy = false, message = "${result.entries.size} Tank-Einträge importiert, ${result.skippedDuplicates} Duplikate übersprungen.")
        }
    }

    fun exportFuelEntries(vehicleId: String): ByteArray {
        val vehicle = _state.value.vehicles.firstOrNull { it.id == vehicleId }
            ?: Vehicle(displayName = "Fahrzeug", manufacturer = "", modelName = "", licensePlate = "", fuelType = FuelType.Other)
        return fuelLogCsv.export(vehicle, _state.value.fuelEntries.filter { it.vehicleId == vehicleId })
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = "")
    }

    private fun handleAuthResult(result: Result<de.h3nri5h.spendfox.data.auth.AuthSession>) {
        result.onSuccess { session ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.attachLocalDataToUser(session.userId)
                repository.upsertProfile(
                    de.h3nri5h.spendfox.data.UserProfile(
                        id = session.userId,
                        userId = session.userId,
                        email = session.email,
                        displayName = session.email.substringBefore("@")
                    )
                )
            }
            _state.value = _state.value.copy(
                activeUserId = session.userId,
                isUnlocked = false,
                mode = AppMode.Locked,
                pendingRegistrationEmail = null,
                message = ""
            )
        }.onFailure {
            _state.value = _state.value.copy(message = it.message.orEmpty())
        }
    }

    private fun handleRegistrationResult(result: Result<RegistrationResult>) {
        result.onSuccess { registration ->
            when (registration) {
                is RegistrationResult.PendingVerification -> {
                    _state.value = _state.value.copy(
                        pendingRegistrationEmail = registration.email,
                        message = "Wir haben dir einen 6-stelligen Code per E-Mail gesendet."
                    )
                }
                is RegistrationResult.SignedIn -> handleAuthResult(Result.success(registration.session))
            }
        }.onFailure {
            _state.value = _state.value.copy(message = it.message.orEmpty())
        }
    }

    private fun observeSnapshot(userId: String) {
        if (snapshotJob?.isActive == true && _state.value.activeUserId == userId) return
        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            repository.observeSnapshot(userId).collect { snapshot ->
                _state.value = _state.value.copy(snapshot = snapshot)
            }
        }
    }

    private fun activeUserId(): String = _state.value.authStatus.session?.userId ?: LOCAL_USER_ID

    private fun readThemeMode(): SpendFoxThemeMode {
        val saved = preferences.getString(SpendFoxPreferenceKeys.THEME_MODE, SpendFoxThemeMode.System.name)
        return SpendFoxThemeMode.entries.firstOrNull { it.name == saved } ?: SpendFoxThemeMode.System
    }

    private fun readUnlockMethod(): UnlockMethod {
        val saved = preferences.getString(SpendFoxPreferenceKeys.UNLOCK_METHOD, UnlockMethod.Biometric.name)
        return UnlockMethod.entries.firstOrNull { it.name == saved } ?: UnlockMethod.Biometric
    }

    private fun readPersonalProfile(): PersonalProfile {
        return PersonalProfile(
            salutation = preferences.getString(SpendFoxPreferenceKeys.PROFILE_SALUTATION, PersonalProfile().salutation) ?: PersonalProfile().salutation,
            firstName = preferences.getString(SpendFoxPreferenceKeys.PROFILE_FIRST_NAME, "").orEmpty(),
            lastName = preferences.getString(SpendFoxPreferenceKeys.PROFILE_LAST_NAME, "").orEmpty(),
            birthDate = preferences.getString(SpendFoxPreferenceKeys.PROFILE_BIRTH_DATE, "").orEmpty(),
            address = preferences.getString(SpendFoxPreferenceKeys.PROFILE_ADDRESS, "").orEmpty(),
            mobileNumber = preferences.getString(SpendFoxPreferenceKeys.PROFILE_MOBILE_NUMBER, "").orEmpty()
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                @Suppress("UNCHECKED_CAST")
                return SpendFoxViewModel(application) as T
            }
        }
    }
}

enum class AppMode {
    LoggedOut,
    Locked,
    Unlocked
}

data class SpendFoxUiState(
    val snapshot: SpendFoxSnapshot = SpendFoxSnapshot(),
    val authStatus: AuthStatus = AuthStatus(),
    val mode: AppMode = AppMode.LoggedOut,
    val isUnlocked: Boolean = false,
    val activeUserId: String = LOCAL_USER_ID,
    val syncMessage: String = "",
    val message: String = "",
    val isBusy: Boolean = false,
    val themeMode: SpendFoxThemeMode = SpendFoxThemeMode.System,
    val unlockMethod: UnlockMethod = UnlockMethod.Biometric,
    val personalProfile: PersonalProfile = PersonalProfile(),
    val pendingRegistrationEmail: String? = null,
    val importReviewItems: List<ExpenseImportReviewItem> = emptyList(),
    val appVersion: String = "0.001.00"
) {
    val expenses: List<Expense> = snapshot.expenses
    val products: List<Product> = snapshot.products
    val vehicles: List<Vehicle> = snapshot.vehicles
    val trips: List<Trip> = snapshot.trips
    val fuelEntries: List<FuelEntry> = snapshot.fuelEntries
    val maintenanceItems: List<MaintenanceItem> = snapshot.maintenanceItems
    val categories: List<UserCategory> = snapshot.categories
    val currentMonthExpenses: List<Expense> = expenses.filter { isInCurrentMonth(it.occurredAtEpochMillis) }
    val expenseTotalCents: Long = currentMonthExpenses.sumOf { it.amountCents }
    val productValueCents: Long = products.sumOf { it.purchasePriceCents }
    val productResidualValueCents: Long = products.sumOf { it.residualValueCents() }
}

private fun isInCurrentMonth(epochMillis: Long): Boolean {
    val currentMonth = java.time.YearMonth.now()
    val itemMonth = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .let { java.time.YearMonth.from(it) }
    return itemMonth == currentMonth
}

private fun Product.residualValueCents(): Long {
    if (usageDurationMonths <= 0) return purchasePriceCents
    val purchaseMonth = java.time.Instant.ofEpochMilli(purchasedAtEpochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .let { java.time.YearMonth.from(it) }
    val monthsUsed = java.time.temporal.ChronoUnit.MONTHS.between(purchaseMonth, java.time.YearMonth.now()).coerceAtLeast(0)
    val remainingMonths = (usageDurationMonths - monthsUsed).coerceAtLeast(0)
    return (purchasePriceCents * remainingMonths / usageDurationMonths).coerceAtLeast(0)
}
