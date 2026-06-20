package de.h3nri5h.spendfox.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityRepository(context: Context) {
    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "spendfox_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse {
        context.getSharedPreferences("spendfox_secure_fallback", Context.MODE_PRIVATE)
    }

    fun hasPin(userId: String): Boolean = prefs.contains(hashKey(userId))

    fun setPin(userId: String, pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = randomHex(16)
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(saltKey(userId), salt)
            .putString(hashKey(userId), hash)
            .apply()
        return true
    }

    fun verifyPin(userId: String, pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val salt = prefs.getString(saltKey(userId), null) ?: return false
        val expected = prefs.getString(hashKey(userId), null) ?: return false
        return MessageDigest.isEqual(hashPin(pin, salt).toByteArray(), expected.toByteArray())
    }

    fun clearPin(userId: String) {
        prefs.edit()
            .remove(saltKey(userId))
            .remove(hashKey(userId))
            .apply()
    }

    private fun isValidPin(pin: String): Boolean = pin.length == 6 && pin.all { it.isDigit() }

    private fun randomHex(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun saltKey(userId: String) = "pin_salt_$userId"
    private fun hashKey(userId: String) = "pin_hash_$userId"
}
