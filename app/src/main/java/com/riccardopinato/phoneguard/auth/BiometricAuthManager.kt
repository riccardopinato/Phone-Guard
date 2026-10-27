// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/auth/BiometricAuthManager.kt
package com.riccardopinato.phoneguard.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val activity: FragmentActivity
) {

    fun isAvailable(): Boolean {
        return BiometricManager
            .from(activity)
            .canAuthenticate(
                BiometricManager
                    .Authenticators
                    .BIOMETRIC_STRONG
            ) ==
            BiometricManager
                .BIOMETRIC_SUCCESS
    }

    fun authenticate(
        onResult: (Boolean) -> Unit
    ) {
        if (!isAvailable()) {
            onResult(false)
            return
        }

        val executor =
            ContextCompat
                .getMainExecutor(activity)

        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object :
                    BiometricPrompt
                        .AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result:
                            BiometricPrompt
                                .AuthenticationResult
                    ) {
                        onResult(true)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        onResult(false)
                    }

                    override fun onAuthenticationFailed() =
                        Unit
                }
            )

        val info =
            BiometricPrompt
                .PromptInfo
                .Builder()
                .setTitle(
                    "Disattiva Phone Guard"
                )
                .setSubtitle(
                    "Conferma la tua identità"
                )
                .setAllowedAuthenticators(
                    BiometricManager
                        .Authenticators
                        .BIOMETRIC_STRONG
                )
                .setNegativeButtonText(
                    "Usa PIN"
                )
                .build()

        prompt.authenticate(info)
    }
}
