package com.riccardopinato.phoneguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.riccardopinato.phoneguard.activation.ActivationSource
import com.riccardopinato.phoneguard.activation.ProtectionActivationCoordinator
import com.riccardopinato.phoneguard.auth.BiometricAuthManager
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import com.riccardopinato.phoneguard.ui.PhoneGuardRoot
import com.riccardopinato.phoneguard.ui.theme.PhoneGuardTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        processLaunchIntent(
            intent
        )

        setContent {
            PhoneGuardTheme {
                PhoneGuardRoot(
                    container = container,
                    biometricAuthManager =
                        biometricAuthManager
                )
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(
            intent
        )
        setIntent(
            intent
        )
        processLaunchIntent(
            intent
        )
    }

    private fun processLaunchIntent(
        launchIntent: Intent?
    ) {
        val source =
            when (
                launchIntent?.action
            ) {
                ACTION_ARM_LAST_SHORTCUT ->
                    ActivationSource.SHORTCUT

                ACTION_ARM_WIDGET ->
                    ActivationSource.WIDGET

                else ->
                    return
            }

        launchIntent.action = null

        lifecycleScope.launch {
            val container =
                (application as PhoneGuardApp)
                    .container

            val settings =
                container
                    .settingsRepository
                    .settings
                    .first()

            ProtectionActivationCoordinator
                .requestArm(
                    context =
                        this@MainActivity,
                    mode =
                        settings.selectedMode,
                    source =
                        source
                )
        }
    }

    private companion object {
        const val ACTION_ARM_LAST_SHORTCUT =
            "com.riccardopinato.phoneguard.action.ARM_LAST_SHORTCUT"

        const val ACTION_ARM_WIDGET =
            "com.riccardopinato.phoneguard.action.ARM_WIDGET"
    }
}
