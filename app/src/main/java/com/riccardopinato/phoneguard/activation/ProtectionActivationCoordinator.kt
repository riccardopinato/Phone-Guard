package com.riccardopinato.phoneguard.activation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.charging.ChargingStateMonitor
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.protection.ProtectionCapabilities
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import kotlinx.coroutines.flow.first

enum class ActivationSource {
    APP,
    TILE,
    WIDGET,
    SHORTCUT,
    AUTO_CHARGER,
    PRESET
}

enum class ActivationBlockReason {
    ALREADY_ACTIVE,
    RECOVERY_PENDING,
    SECURITY_NOT_READY,
    MODE_UNSUPPORTED,
    CHARGER_NOT_CONNECTED,
    NOTIFICATION_PERMISSION,
    CAMERA_PERMISSION
}

sealed interface ActivationOutcome {
    data object Started : ActivationOutcome

    data class Blocked(
        val reason: ActivationBlockReason
    ) : ActivationOutcome
}

object ProtectionActivationCoordinator {

    suspend fun evaluate(
        context: Context,
        mode: ProtectionMode
    ): ActivationOutcome {
        val app =
            context.applicationContext as PhoneGuardApp

        val container =
            app.container

        if (
            AlarmStateStore.state.value.phase !=
            AlarmPhase.DISARMED
        ) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.ALREADY_ACTIVE
            )
        }

        if (
            container
                .sessionStateRepository
                .read() != null
        ) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.RECOVERY_PENDING
            )
        }

        val security =
            container.securityRepository
                .securityHealth
                .first()

        if (security != SecurityHealth.READY) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.SECURITY_NOT_READY
            )
        }

        val settings =
            container.settingsRepository
                .settings
                .first()

        val hardware =
            container.hardwareDiagnostics
                .snapshot()

        val supported =
            ProtectionPolicy.isSupported(
                mode = mode,
                journeyGuardMode =
                    settings.journeyGuardMode,
                capabilities =
                    ProtectionCapabilities(
                        hasAccelerometer =
                            hardware.hasAccelerometer,
                        hasProximity =
                            hardware.hasProximity,
                        hasLight =
                            hardware.hasLight,
                        hasGyroscope =
                            hardware.hasGyroscope
                    )
            )

        if (!supported) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.MODE_UNSUPPORTED
            )
        }

        if (
            mode == ProtectionMode.CHARGING &&
            !ChargingStateMonitor(context)
                .isPowerConnected()
        ) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.CHARGER_NOT_CONNECTED
            )
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.NOTIFICATION_PERMISSION
            )
        }

        val profile =
            settings.alarmProfile(mode)

        if (
            profile.flashEnabled &&
            hardware.hasFlash &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ActivationOutcome.Blocked(
                ActivationBlockReason.CAMERA_PERMISSION
            )
        }

        return ActivationOutcome.Started
    }

    suspend fun requestArm(
        context: Context,
        mode: ProtectionMode,
        source: ActivationSource
    ): ActivationOutcome {
        val outcome =
            evaluate(
                context = context,
                mode = mode
            )

        if (outcome == ActivationOutcome.Started) {
            ProtectionForegroundService.arm(
                context = context,
                mode = mode,
                source = source
            )
        }

        return outcome
    }
}
