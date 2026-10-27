// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/MainActivity.kt
package com.riccardopinato.phoneguard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.riccardopinato.phoneguard.auth.BiometricAuthManager
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import com.riccardopinato.phoneguard.ui.PhoneGuardRoot
import com.riccardopinato.phoneguard.ui.theme.TabletopAlarmTheme

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
            TabletopAlarmTheme {
                PhoneGuardRoot(
                    container = container,
                    biometricAuthManager =
                        biometricAuthManager
                )
            }
        }
    }
}
