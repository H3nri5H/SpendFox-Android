package de.h3nri5h.spendfox.data.auth

import android.content.Context
import de.h3nri5h.spendfox.BuildConfig
import de.h3nri5h.spendfox.data.supabase.SupabaseRestClient
import java.security.MessageDigest
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
    private val prefs = context.getSharedPreferences("spendfox_auth", Context.MODE_PRIVATE)
    private val supabase = SupabaseRestClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY)
    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<AuthStatus> = _status.asStateFlow()

    suspend fun register(email: String, password: String): Result<RegistrationResult> = withContext(Dispatchers.IO) {
        val validation = validateCredentials(email, password)
        validation.exceptionOrNull()?.let { return@withContext Result.failure(it) }
        val normalizedEmail = requireNotNull(validation.getOrNull())

        if (supabase.isConfigured) {
            runCatching<RegistrationResult> {
                supabase.signUp(normalizedEmail, password)
                RegistrationResult.PendingVerification(normalizedEmail)
            }
        } else {
            val session = createLocalSession(normalizedEmail)
            _status.value = readStatus()
            Result.success<RegistrationResult>(RegistrationResult.SignedIn(session))
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
        if (!supabase.isConfigured) {
            val session = createLocalSession(normalizedEmail)
            _status.value = readStatus()
            return@withContext Result.success(session)
        }
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
        if (supabase.isConfigured) {
            runCatching { supabase.resendSignupOtp(normalizedEmail) }
        } else {
            Result.success(Unit)
        }
    }

    suspend fun forgotPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Bitte eine gültige E-Mail-Adresse eingeben."))
        }
        if (supabase.isConfigured) {
            runCatching { supabase.recoverPassword(normalizedEmail) }
        } else {
            Result.success(Unit)
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        _status.value = readStatus()
    }

    fun currentAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    private suspend fun authenticate(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val validation = validateCredentials(email, password)
        validation.exceptionOrNull()?.let { return@withContext Result.failure(it) }
        val normalizedEmail = requireNotNull(validation.getOrNull())

        val result = if (supabase.isConfigured) {
            runCatching {
                saveSession(supabase.signIn(normalizedEmail, password))
            }
        } else {
            Result.success(createLocalSession(normalizedEmail))
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
        prefs.edit()
            .putString(KEY_USER_ID, response.userId)
            .putString(KEY_EMAIL, response.email)
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .apply()
        return AuthSession(userId = response.userId, email = response.email)
    }

    private fun createLocalSession(normalizedEmail: String): AuthSession {
        val session = AuthSession(
            userId = stableUserId(normalizedEmail),
            email = normalizedEmail
        )
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .apply()
        return session
    }

    private fun readStatus(): AuthStatus {
        val userId = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val session = if (!userId.isNullOrBlank() && !email.isNullOrBlank()) AuthSession(userId, email) else null
        return AuthStatus(
            session = session,
            isSupabaseConfigured = supabase.isConfigured
        )
    }

    private fun stableUserId(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
