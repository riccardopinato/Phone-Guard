package com.riccardopinato.phoneguard.auth

import androidx.biometric.BiometricManager
import com.riccardopinato.phoneguard.R
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
                    activity.getString(R.string.biometric_prompt_title)
                )
                .setSubtitle(
                    activity.getString(R.string.biometric_prompt_subtitle)
                )
                .setAllowedAuthenticators(
                    BiometricManager
                        .Authenticators
                        .BIOMETRIC_STRONG
                )
                .setNegativeButtonText(
                    activity.getString(R.string.biometric_prompt_use_pin)
                )
                .build()

        prompt.authenticate(info)
    }
}
