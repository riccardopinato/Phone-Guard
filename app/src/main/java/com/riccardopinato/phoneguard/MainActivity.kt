package com.riccardopinato.phoneguard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.riccardopinato.phoneguard.auth.BiometricAuthManager
import com.riccardopinato.phoneguard.quickaccess.QuickArmCoordinator
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import com.riccardopinato.phoneguard.ui.PhoneGuardRoot
import com.riccardopinato.phoneguard.ui.theme.PhoneGuardTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        val container =
            (application as PhoneGuardApp)
                .container

        val biometricAuthManager =
            BiometricAuthManager(this)

        if (
            container
                .sessionStateRepository
                .read() != null
        ) {
            ProtectionForegroundService
                .restore(this)
        }

        setContent {
            PhoneGuardTheme {
                PhoneGuardRoot(
                    container = container,
                    biometricAuthManager =
                        biometricAuthManager
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(
        intent: android.content.Intent
    ) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(
        intent: android.content.Intent?
    ) {
        if (
            intent?.action !=
            ACTION_QUICK_ARM
        ) {
            return
        }

        lifecycleScope.launchWhenStarted {
            QuickArmCoordinator
                .arm(this@MainActivity)
        }

        intent.action = null
    }

    private companion object {
        const val ACTION_QUICK_ARM =
            "com.riccardopinato.phoneguard.action.QUICK_ARM"
    }
}
