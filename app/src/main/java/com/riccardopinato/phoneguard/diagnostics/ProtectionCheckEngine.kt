package com.riccardopinato.phoneguard.diagnostics

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.accessory.AccessoryStateMonitor
import com.riccardopinato.phoneguard.charging.ChargingStateMonitor
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.protection.ProtectionCapabilities
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import kotlinx.coroutines.flow.first

enum class ProtectionCheckId {
    SECURITY,
    SELECTED_MODE,
    ACCELEROMETER,
    PROXIMITY,
    AUXILIARY_SENSOR,
    FLASH,
    VIBRATION,
    NOTIFICATIONS,
    ALARM_VOLUME,
    DO_NOT_DISTURB,
    BATTERY_OPTIMIZATION,
    POWER_CONNECTION,
    ACCESSORY_CONNECTION,
    OEM_RELIABILITY,
    LAST_EXIT,
    SYSTEM_THEFT_PROTECTION
}

data class ProtectionCheckItem(
    val id: ProtectionCheckId,
    val ready: Boolean,
    val blocking: Boolean,
    val detail: String
)

data class ProtectionCheckReport(
    val items: List<ProtectionCheckItem>
) {
    val readyCount: Int =
        items.count { it.ready }

    val totalCount: Int =
        items.size

    val hasBlockingIssue: Boolean =
        items.any {
            it.blocking &&
                !it.ready
        }
}

class ProtectionCheckEngine(
    private val context: Context
) {
    suspend fun run(
        selectedMode: ProtectionMode
    ): ProtectionCheckReport {
        val app =
            context.applicationContext as PhoneGuardApp

        val container =
            app.container

        val settings =
            container.settingsRepository
                .settings
                .first()

        val security =
            container.securityRepository
                .securityHealth
                .first()

        val hardware =
            container.hardwareDiagnostics
                .snapshot()

        val environment =
            container.hardwareDiagnostics
                .alarmEnvironmentSnapshot()

        val notifications =
            context.getSystemService(
                NotificationManager::class.java
            ).areNotificationsEnabled() &&
                (
                    Build.VERSION.SDK_INT <
                        Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) ==
                        PackageManager.PERMISSION_GRANTED
                    )

        val capabilities =
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

        val chargingConnected =
            ChargingStateMonitor(context)
                .isPowerConnected()

        val accessoryConnected =
            AccessoryStateMonitor(context)
                .hasConnectedAccessory()

        val modeReady =
            ProtectionPolicy.isSupported(
                selectedMode,
                settings.journeyGuardMode,
                capabilities
            ) &&
                (
                    selectedMode !=
                        ProtectionMode.CHARGING ||
                        chargingConnected
                    ) &&
                (
                    selectedMode !=
                        ProtectionMode.ACCESSORY ||
                        accessoryConnected
                    )

        val reliability =
            ProtectionReliabilityDiagnostics(
                context
            ).snapshot()

        val profile =
            settings.alarmProfile(
                selectedMode
            )

        val items =
            buildList {
                add(
                    ProtectionCheckItem(
                        id = ProtectionCheckId.SECURITY,
                        ready =
                            security ==
                                SecurityHealth.READY,
                        blocking = true,
                        detail = security.name
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.SELECTED_MODE,
                        ready = modeReady,
                        blocking = true,
                        detail =
                            selectedMode.name
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.ACCELEROMETER,
                        ready =
                            hardware.hasAccelerometer,
                        blocking =
                            selectedMode !=
                                ProtectionMode.CHARGING &&
                                selectedMode !=
                                    ProtectionMode.ACCESSORY,
                        detail =
                            if (hardware.hasAccelerometer) {
                                "available"
                            } else {
                                "missing"
                            }
                    )
                )

                add(
                    ProtectionCheckItem(
                        id = ProtectionCheckId.PROXIMITY,
                        ready =
                            hardware.hasProximity,
                        blocking =
                            selectedMode ==
                                ProtectionMode.POCKET ||
                                (
                                    selectedMode ==
                                        ProtectionMode.JOURNEY &&
                                        settings.journeyGuardMode ==
                                            GuardMode.POCKET
                                    ),
                        detail =
                            if (hardware.hasProximity) {
                                "available"
                            } else {
                                "missing"
                            }
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.AUXILIARY_SENSOR,
                        ready =
                            hardware.hasLight ||
                                hardware.hasGyroscope,
                        blocking =
                            selectedMode ==
                                ProtectionMode.BAG ||
                                (
                                    selectedMode ==
                                        ProtectionMode.JOURNEY &&
                                        settings.journeyGuardMode ==
                                            GuardMode.BAG
                                    ),
                        detail =
                            "light=${hardware.hasLight}; gyro=${hardware.hasGyroscope}"
                    )
                )

                val flashPermission =
                    !profile.flashEnabled ||
                        !hardware.hasFlash ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) ==
                        PackageManager.PERMISSION_GRANTED

                add(
                    ProtectionCheckItem(
                        id = ProtectionCheckId.FLASH,
                        ready =
                            !profile.flashEnabled ||
                                (
                                    hardware.hasFlash &&
                                        flashPermission
                                    ),
                        blocking =
                            profile.flashEnabled &&
                                hardware.hasFlash &&
                                !flashPermission,
                        detail =
                            "requested=${profile.flashEnabled}; hardware=${hardware.hasFlash}"
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.VIBRATION,
                        ready =
                            !profile.vibrationEnabled ||
                                hardware.hasVibrator,
                        blocking = false,
                        detail =
                            "requested=${profile.vibrationEnabled}; hardware=${hardware.hasVibrator}"
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.NOTIFICATIONS,
                        ready = notifications,
                        blocking = true,
                        detail =
                            if (notifications) {
                                "enabled"
                            } else {
                                "blocked"
                            }
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.ALARM_VOLUME,
                        ready =
                            !profile.soundEnabled ||
                                environment
                                    .alarmVolumePercent >
                                    0,
                        blocking = false,
                        detail =
                            "${environment.alarmVolumePercent}%"
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.DO_NOT_DISTURB,
                        ready =
                            !environment
                                .doNotDisturbActive,
                        blocking = false,
                        detail =
                            if (
                                environment
                                    .doNotDisturbActive
                            ) {
                                "active"
                            } else {
                                "inactive"
                            }
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.BATTERY_OPTIMIZATION,
                        ready =
                            container
                                .batteryDiagnostics
                                .isIgnoringBatteryOptimizations(),
                        blocking = false,
                        detail = "OEM dependent"
                    )
                )

                if (
                    selectedMode ==
                    ProtectionMode.CHARGING
                ) {
                    add(
                        ProtectionCheckItem(
                            id =
                                ProtectionCheckId.POWER_CONNECTION,
                            ready =
                                chargingConnected,
                            blocking = true,
                            detail =
                                if (
                                    chargingConnected
                                ) {
                                    "connected"
                                } else {
                                    "disconnected"
                                }
                        )
                    )
                }

                if (
                    selectedMode ==
                    ProtectionMode.ACCESSORY
                ) {
                    add(
                        ProtectionCheckItem(
                            id =
                                ProtectionCheckId.ACCESSORY_CONNECTION,
                            ready =
                                accessoryConnected,
                            blocking = true,
                            detail =
                                if (
                                    accessoryConnected
                                ) {
                                    "protected audio accessory detected"
                                } else {
                                    "connect wired, USB or Bluetooth audio first"
                                }
                        )
                    )
                }

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.OEM_RELIABILITY,
                        ready = true,
                        blocking = false,
                        detail =
                            reliability.manufacturerAdvice
                    )
                )

                val lastExitIssue =
                    reliability.lastExitReason
                        ?.let {
                            reason ->
                            reason.contains(
                                "crash",
                                ignoreCase = true
                            ) ||
                                reason.contains(
                                    "ANR",
                                    ignoreCase = true
                                ) ||
                                reason.contains(
                                    "low memory",
                                    ignoreCase = true
                                ) ||
                                reason.contains(
                                    "resource",
                                    ignoreCase = true
                                )
                        }
                        ?: false

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.LAST_EXIT,
                        ready =
                            !lastExitIssue,
                        blocking = false,
                        detail =
                            reliability.lastExitReason
                                ?: "no previous exit reason reported by Android"
                    )
                )

                add(
                    ProtectionCheckItem(
                        id =
                            ProtectionCheckId.SYSTEM_THEFT_PROTECTION,
                        ready = true,
                        blocking = false,
                        detail =
                            reliability.systemTheftProtectionGuidance
                    )
                )
            }

        return ProtectionCheckReport(
            items = items
        )
    }
}
