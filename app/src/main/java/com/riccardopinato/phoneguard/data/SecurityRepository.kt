package com.riccardopinato.phoneguard.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by
    preferencesDataStore(
        name = "phone_guard_security"
    )

enum class SecurityHealth {
    NO_PIN,
    READY,
    KEY_UNAVAILABLE,
    CORRUPTED_VERIFIER
}

sealed interface PinVerificationResult {
    data object Success :
        PinVerificationResult

    data class Invalid(
        val attemptsBeforeLockout: Int
    ) : PinVerificationResult

    data class LockedOut(
        val remainingMs: Long
    ) : PinVerificationResult

    data class Unavailable(
        val health: SecurityHealth
    ) : PinVerificationResult
}

internal object PinRateLimitPolicy {
    const val FREE_FAILURES = 4

    fun lockoutDurationMs(
        failedAttempts: Int
    ): Long =
        when {
            failedAttempts <=
                FREE_FAILURES ->
                0L

            failedAttempts == 5 ->
                15_000L

            failedAttempts == 6 ->
                30_000L

            failedAttempts == 7 ->
                60_000L

            else ->
                300_000L
        }

    fun attemptsBeforeLockout(
        failedAttempts: Int
    ): Int =
        (
            FREE_FAILURES +
                1 -
                failedAttempts
            ).coerceAtLeast(0)
}

class SecurityRepository(
    private val context: Context
) {

    private object Keys {
        val pinSalt =
            stringPreferencesKey(
                "pin_salt"
            )

        val pinHash =
            stringPreferencesKey(
                "pin_hash"
            )

        val failedAttempts =
            intPreferencesKey(
                "failed_attempts"
            )

        val lockoutUntil =
            longPreferencesKey(
                "lockout_until"
            )
    }

    val securityHealth:
        Flow<SecurityHealth> =
        context.securityDataStore.data
            .map(::healthFrom)

    val hasPin:
        Flow<Boolean> =
        securityHealth.map {
            it !=
                SecurityHealth.NO_PIN
        }

    suspend fun setPin(
        pin: String
    ) {
        require(
            pin.matches(PIN_REGEX)
        ) {
            "Il PIN deve contenere da 4 a 6 cifre."
        }

        val salt =
            ByteArray(
                SALT_BYTES
            ).also {
                SecureRandom()
                    .nextBytes(it)
            }

        val key =
            getOrCreateKeystoreKey()

        val hash =
            deriveVerifier(
                pin = pin,
                salt = salt,
                key = key
            )

        context.securityDataStore
            .edit { prefs ->
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

                prefs[Keys.failedAttempts] =
                    0

                prefs[Keys.lockoutUntil] =
                    0L
            }
    }

    suspend fun verifyPin(
        pin: String
    ): PinVerificationResult {
        if (
            !pin.matches(
                PIN_REGEX
            )
        ) {
            return PinVerificationResult
                .Invalid(
                    attemptsBeforeLockout =
                        PinRateLimitPolicy
                            .FREE_FAILURES
                )
        }

        val prefs =
            context.securityDataStore
                .data
                .first()

        val health =
            healthFrom(prefs)

        if (
            health !=
            SecurityHealth.READY
        ) {
            return PinVerificationResult
                .Unavailable(
                    health
                )
        }

        val now =
            System.currentTimeMillis()

        val lockoutUntil =
            prefs[
                Keys.lockoutUntil
            ] ?: 0L

        if (
            lockoutUntil >
            now
        ) {
            return PinVerificationResult
                .LockedOut(
                    remainingMs =
                        lockoutUntil -
                            now
                )
        }

        val salt =
            runCatching {
                Base64.decode(
                    prefs[Keys.pinSalt],
                    Base64.NO_WRAP
                )
            }.getOrNull()
                ?: return PinVerificationResult
                    .Unavailable(
                        SecurityHealth
                            .CORRUPTED_VERIFIER
                    )

        val expected =
            runCatching {
                Base64.decode(
                    prefs[Keys.pinHash],
                    Base64.NO_WRAP
                )
            }.getOrNull()
                ?: return PinVerificationResult
                    .Unavailable(
                        SecurityHealth
                            .CORRUPTED_VERIFIER
                    )

        val key =
            getExistingKeystoreKey()
                ?: return PinVerificationResult
                    .Unavailable(
                        SecurityHealth
                            .KEY_UNAVAILABLE
                    )

        val actual =
            runCatching {
                deriveVerifier(
                    pin = pin,
                    salt = salt,
                    key = key
                )
            }.getOrNull()
                ?: return PinVerificationResult
                    .Unavailable(
                        SecurityHealth
                            .KEY_UNAVAILABLE
                    )

        if (
            MessageDigest.isEqual(
                expected,
                actual
            )
        ) {
            resetRateLimit()

            return PinVerificationResult
                .Success
        }

        val failures =
            (
                prefs[
                    Keys.failedAttempts
                ] ?: 0
            ) + 1

        val lockoutMs =
            PinRateLimitPolicy
                .lockoutDurationMs(
                    failures
                )

        context.securityDataStore
            .edit { mutable ->
                mutable[
                    Keys.failedAttempts
                ] = failures

                mutable[
                    Keys.lockoutUntil
                ] =
                    if (
                        lockoutMs >
                        0L
                    ) {
                        now +
                            lockoutMs
                    } else {
                        0L
                    }
            }

        return if (
            lockoutMs >
            0L
        ) {
            PinVerificationResult
                .LockedOut(
                    lockoutMs
                )
        } else {
            PinVerificationResult
                .Invalid(
                    attemptsBeforeLockout =
                        PinRateLimitPolicy
                            .attemptsBeforeLockout(
                                failures
                            )
                )
        }
    }

    suspend fun clearPin() {
        context.securityDataStore
            .edit { prefs ->
                prefs.clear()
            }

        deleteKeystoreKey()
    }

    private suspend fun resetRateLimit() {
        context.securityDataStore
            .edit { prefs ->
                prefs[
                    Keys.failedAttempts
                ] = 0

                prefs[
                    Keys.lockoutUntil
                ] = 0L
            }
    }

    private fun healthFrom(
        prefs: Preferences
    ): SecurityHealth {
        val saltEncoded =
            prefs[Keys.pinSalt]

        val hashEncoded =
            prefs[Keys.pinHash]

        if (
            saltEncoded.isNullOrBlank() &&
            hashEncoded.isNullOrBlank()
        ) {
            return SecurityHealth.NO_PIN
        }

        if (
            saltEncoded.isNullOrBlank() ||
            hashEncoded.isNullOrBlank()
        ) {
            return SecurityHealth
                .CORRUPTED_VERIFIER
        }

        val validEncoding =
            runCatching {
                val salt =
                    Base64.decode(
                        saltEncoded,
                        Base64.NO_WRAP
                    )

                val hash =
                    Base64.decode(
                        hashEncoded,
                        Base64.NO_WRAP
                    )

                salt.size ==
                    SALT_BYTES &&
                    hash.size ==
                    HMAC_BYTES
            }.getOrDefault(false)

        if (!validEncoding) {
            return SecurityHealth
                .CORRUPTED_VERIFIER
        }

        return if (
            getExistingKeystoreKey() !=
            null
        ) {
            SecurityHealth.READY
        } else {
            SecurityHealth
                .KEY_UNAVAILABLE
        }
    }

    private fun deriveVerifier(
        pin: String,
        salt: ByteArray,
        key: SecretKey
    ): ByteArray {
        val chars =
            pin.toCharArray()

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
                        .generateSecret(
                            spec
                        )
                        .encoded
                } finally {
                    spec.clearPassword()
                }
            } finally {
                chars.fill(
                    ' '
                )
            }

        val mac =
            Mac.getInstance(
                "HmacSHA256"
            )

        mac.init(key)

        return try {
            mac.doFinal(
                derived
            )
        } finally {
            derived.fill(0)
        }
    }

    private fun getExistingKeystoreKey():
        SecretKey? =
        runCatching {
            val keyStore =
                KeyStore.getInstance(
                    "AndroidKeyStore"
                ).apply {
                    load(null)
                }

            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey
        }.getOrNull()

    private fun getOrCreateKeystoreKey():
        SecretKey {
        getExistingKeystoreKey()
            ?.let {
                return it
            }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties
                    .KEY_ALGORITHM_HMAC_SHA256,
                "AndroidKeyStore"
            )

        generator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or
                        KeyProperties
                            .PURPOSE_VERIFY
                )
                .build()
        )

        return generator
            .generateKey()
    }

    private fun deleteKeystoreKey() {
        runCatching {
            val keyStore =
                KeyStore.getInstance(
                    "AndroidKeyStore"
                ).apply {
                    load(null)
                }

            if (
                keyStore.containsAlias(
                    KEY_ALIAS
                )
            ) {
                keyStore.deleteEntry(
                    KEY_ALIAS
                )
            }
        }
    }

    companion object {
        val PIN_REGEX =
            Regex("\d{4,6}")

        const val PBKDF2_ITERATIONS =
            210_000

        const val KEY_LENGTH_BITS =
            256

        const val SALT_BYTES =
            16

        const val HMAC_BYTES =
            32

        const val KEY_ALIAS =
            "phone_guard_pin_hmac"
    }
}
