// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/data/SecurityRepository.kt
package com.riccardopinato.phoneguard.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.securityDataStore by preferencesDataStore(name = "security")

class SecurityRepository(private val context: Context) {

    private object Keys {
        val pinSalt = stringPreferencesKey("pin_salt")
        val pinHash = stringPreferencesKey("pin_hash")
    }

    val hasPin: Flow<Boolean> = context.securityDataStore.data.map { prefs ->
        !prefs[Keys.pinSalt].isNullOrBlank() &&
            !prefs[Keys.pinHash].isNullOrBlank()
    }

    suspend fun setPin(pin: String) {
        require(pin.matches(Regex("\\d{4,6}"))) {
            "Il PIN deve contenere da 4 a 6 cifre."
        }

        val salt = ByteArray(SALT_BYTES).also {
            SecureRandom().nextBytes(it)
        }

        val hash = derive(pin, salt)

        context.securityDataStore.edit { prefs ->
            prefs[Keys.pinSalt] =
                Base64.encodeToString(salt, Base64.NO_WRAP)
            prefs[Keys.pinHash] =
                Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("\\d{4,6}"))) return false

        val prefs = context.securityDataStore.data.first()
        val saltEncoded = prefs[Keys.pinSalt] ?: return false
        val hashEncoded = prefs[Keys.pinHash] ?: return false

        return runCatching {
            val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
            val expected = Base64.decode(hashEncoded, Base64.NO_WRAP)
            val actual = derive(pin, salt)
            MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }

    suspend fun clearPin() {
        context.securityDataStore.edit { prefs ->
            prefs.remove(Keys.pinSalt)
            prefs.remove(Keys.pinHash)
        }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()

        return try {
            val spec = PBEKeySpec(
                chars,
                salt,
                ITERATIONS,
                KEY_LENGTH_BITS
            )

            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            chars.fill('\u0000')
        }
    }

    private companion object {
        const val ITERATIONS = 210_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_BYTES = 16
    }
}
