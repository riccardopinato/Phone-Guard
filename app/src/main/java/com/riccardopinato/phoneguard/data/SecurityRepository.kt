package com.riccardopinato.phoneguard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.securityDataStore by preferencesDataStore(
    name = "phone_guard_security"
)

class SecurityRepository(private val context: Context) {

    private object Keys {
        val pinSalt = stringPreferencesKey("pin_salt")
        val pinHash = stringPreferencesKey("pin_hash")
    }

    val hasPin: Flow<Boolean> =
        context.securityDataStore.data.map { prefs ->
            !prefs[Keys.pinSalt].isNullOrBlank() &&
                !prefs[Keys.pinHash].isNullOrBlank()
        }

    suspend fun setPin(pin: String) {
        require(pin.matches(PIN_REGEX)) {
            "Il PIN deve contenere da 4 a 6 cifre."
        }

        val salt =
            ByteArray(SALT_BYTES).also {
                SecureRandom().nextBytes(it)
            }

        val hash = deriveVerifier(pin, salt)

        context.securityDataStore.edit { prefs ->
            prefs[Keys.pinSalt] =
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            prefs[Keys.pinHash] =
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        if (!pin.matches(PIN_REGEX)) return false

        val prefs =
            context.securityDataStore.data.first()

        val saltEncoded =
            prefs[Keys.pinSalt] ?: return false
        val hashEncoded =
            prefs[Keys.pinHash] ?: return false

        return runCatching {
            val salt =
                Base64.decode(
                    saltEncoded,
                    Base64.NO_WRAP
                )

            val expected =
                Base64.decode(
                    hashEncoded,
                    Base64.NO_WRAP
                )

            val actual =
                deriveVerifier(
                    pin,
                    salt
                )

            MessageDigest.isEqual(
                expected,
                actual
            )
        }.getOrDefault(false)
    }

    suspend fun clearPin() {
        context.securityDataStore.edit { prefs ->
            prefs.remove(Keys.pinSalt)
            prefs.remove(Keys.pinHash)
        }
    }

    private fun deriveVerifier(
        pin: String,
        salt: ByteArray
    ): ByteArray {
        val chars = pin.toCharArray()

        val derived =
            try {
                val spec =
                    PBEKeySpec(
                        chars,
                        salt,
                        PBKDF2_ITERATIONS,
                        KEY_LENGTH_BITS
                    )

                try {
                    SecretKeyFactory
                        .getInstance(
                            "PBKDF2WithHmacSHA256"
                        )
                        .generateSecret(spec)
                        .encoded
                } finally {
                    spec.clearPassword()
                }
            } finally {
                chars.fill('\u0000')
            }

        val mac =
            Mac.getInstance("HmacSHA256")
        mac.init(getOrCreateKeystoreKey())

        return try {
            mac.doFinal(derived)
        } finally {
            derived.fill(0)
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore =
            KeyStore.getInstance(
                "AndroidKeyStore"
            ).apply {
                load(null)
            }

        val existing =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existing != null) {
            return existing
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                "AndroidKeyStore"
            )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or
                    KeyProperties.PURPOSE_VERIFY
            ).build()
        )

        return generator.generateKey()
    }

    private companion object {
        val PIN_REGEX =
            Regex("\\d{4,6}")

        const val PBKDF2_ITERATIONS =
            210_000

        const val KEY_LENGTH_BITS =
            256

        const val SALT_BYTES =
            16

        const val KEY_ALIAS =
            "phone_guard_pin_hmac"
    }
}
