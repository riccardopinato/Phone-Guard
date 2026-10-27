// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/AppContainer.kt
package com.riccardopinato.phoneguard

import android.content.Context
import com.riccardopinato.phoneguard.data.EventRepository
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.data.SessionStateRepository
import com.riccardopinato.phoneguard.hardware.BatteryDiagnostics
import com.riccardopinato.phoneguard.hardware.HardwareDiagnostics

class AppContainer(context: Context) {
    val settingsRepository =
        SettingsRepository(context)

    val securityRepository =
        SecurityRepository(context)

    val eventRepository =
        EventRepository(context)

    val sessionStateRepository = SessionStateRepository(context)

    val hardwareDiagnostics =
        HardwareDiagnostics(context)

    val batteryDiagnostics = BatteryDiagnostics(context)
}
