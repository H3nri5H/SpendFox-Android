package de.h3nri5h.spendfox.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.h3nri5h.spendfox.BuildConfig
import de.h3nri5h.spendfox.data.supabase.SupabaseRestClient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AuthSession(
    val userId: String,
    val email: String
)

data class AuthStatus(
    val session: AuthSession? = null,
    val isSupabaseConfigured: Boolean = false
) {
    val isLoggedIn: Boolean
        get() = session != null
}

sealed class RegistrationResult {
    data class PendingVerification(val email: String) : RegistrationResult()
    data class SignedIn(val session: AuthSession) : RegistrationResult()
}

class AuthRepository(context: Context) {
    private val prefs = createSecurePreferences(context)
    private val supabase = SupabaseRestClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<AuthStatus> = _status.asStateFlow()

    suspend fun register(email: String, password: String): Result<RegistrationResult> = withContext(Dispatchers.IO) {
        val validation = validateCredentials(email, password)
        validation.exceptionOrNull()?.let { return@withContext Result.failure(it) }
        val normalizedEmail = requireNotNull(validation.getOrNull())
        requireSupabaseConfigured().exceptionOrNull()?.let { return@withContext Result.failure(it) }

        runCatching<RegistrationResult> {
            supabase.signUp(normalizedEmail, password)
            RegistrationResult.PendingVerification(normalizedEmail)
        }
    }

    suspend fun login(email: String, password: String): Result<AuthSession> {
        return authenticate(email, password)
    }

    suspend fun verifyRegistrationCode(email: String, code: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val token = code.filter(Char::isDigit)
        if (!normalizedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben."))
        }
        if (token.length != 6) {
            return@withContext Result.failure(IllegalArgumentException("Bitte den 6-stelligen Code aus der E-Mail eingeben."))
        }
        requireSupabaseConfigured().exceptionOrNull()?.let { return@withContext Result.failure(it) }
        val result = runCatching {
            saveSession(supabase.verifySignupOtp(normalizedEmail, token))
        }
        _status.value = readStatus()
        return@withContext result
    }

    suspend fun resendRegistrationCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben."))
        }
        requireSupabaseConfigured().exceptionOrNull()?.let { return@withContext Result.failure(it) }
        runCatching { supabase.resendSignupOtp(normalizedEmail) }
    }

    suspend fun forgotPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben."))
        }
        requireSupabaseConfigured().exceptionOrNull()?.let { return@withContext Result.failure(it) }
        runCatching { supabase.recoverPassword(normalizedEmail) }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val token = currentAccessToken()
        if (supabase.isConfigured && !token.isNullOrBlank()) {
            supabase.signOut(token)
        }
        prefs?.edit()?.clear()?.apply()
        _status.value = readStatus()
    }

    fun currentAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    private suspend fun authenticate(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val validation = validateCredentials(email, password)
        validation.exceptionOrNull()?.let { return@withContext Result.failure(it) }
        val normalizedEmail = requireNotNull(validation.getOrNull())
        requireSupabaseConfigured().exceptionOrNull()?.let { return@withContext Result.failure(it) }

        val result = runCatching {
            saveSession(supabase.signIn(normalizedEmail, password))
        }

        _status.value = readStatus()
        return@withContext result
    }

    private fun validateCredentials(email: String, password: String): Result<String> {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben."))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Das Passwort muss mindestens 8 Zeichen haben."))
        }
        return Result.success(normalizedEmail)
    }

    private fun saveSession(response: de.h3nri5h.spendfox.data.supabase.SupabaseAuthResponse): AuthSession {
        val securePrefs = prefs ?: throw IOException("Sicherer Sitzungsspeicher ist nicht verfügbar.")
        securePrefs.edit()
            .putString(KEY_USER_ID, response.userId)
            .putString(KEY_EMAIL, response.email)
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .apply()
        return AuthSession(userId = response.userId, email = response.email)
    }

    private fun readStatus(): AuthStatus {
        val securePrefs = prefs
        if (!supabase.isConfigured || securePrefs == null) {
            return AuthStatus(session = null, isSupabaseConfigured = supabase.isConfigured)
        }
        val userId = securePrefs.getString(KEY_USER_ID, null)
        val email = securePrefs.getString(KEY_EMAIL, null)
        val accessToken = securePrefs.getString(KEY_ACCESS_TOKEN, null)
        val session = if (!userId.isNullOrBlank() && !email.isNullOrBlank() && !accessToken.isNullOrBlank()) AuthSession(userId, email) else null
        return AuthStatus(
            session = session,
            isSupabaseConfigured = supabase.isConfigured
        )
    }

    private fun requireSupabaseConfigured(): Result<Unit> {
        return if (supabase.isConfigured) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Supabase ist nicht konfiguriert. Nutzblick erlaubt keinen lokalen Konto-Modus mehr."))
        }
    }

    private fun createSecurePreferences(context: Context): SharedPreferences? {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "nutzblick_auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
