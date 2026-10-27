package com.riccardopinato.phoneguard

import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.quick.PhoneGuardTileService
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import com.riccardopinato.phoneguard.widget.PhoneGuardWidgetProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectionServiceManifestInstrumentedTest {

    @Suppress("DEPRECATION")
    @Test
    fun serviceIsPrivateAndDeclaresSpecialUseFgs() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val info =
            context.packageManager
                .getServiceInfo(
                    ComponentName(
                        context,
                        ProtectionForegroundService::class.java
                    ),
                    0
                )

        assertFalse(
            info.exported
        )

        assertTrue(
            info.foregroundServiceType and
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_SPECIAL_USE !=
                0
        )
    }


    @Suppress("DEPRECATION")
    @Test
    fun quickTileRequiresSystemBindingPermission() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val info =
            context.packageManager
                .getServiceInfo(
                    ComponentName(
                        context,
                        PhoneGuardTileService::class.java
                    ),
                    0
                )

        assertTrue(
            info.exported
        )

        assertTrue(
            info.permission ==
                "android.permission.BIND_QUICK_SETTINGS_TILE"
        )
    }

    @Suppress("DEPRECATION")
    @Test
    fun widgetProviderIsRegisteredForLauncherUpdates() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val info =
            context.packageManager
                .getReceiverInfo(
                    ComponentName(
                        context,
                        PhoneGuardWidgetProvider::class.java
                    ),
                    0
                )

        assertTrue(
            info.exported
        )
    }
}
