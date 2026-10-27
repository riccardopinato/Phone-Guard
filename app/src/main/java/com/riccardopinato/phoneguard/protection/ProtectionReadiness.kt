package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode

data class ProtectionReadiness(
    val mode: ProtectionMode,
    val ready: Boolean,
    val reason: String
)

object ProtectionReadinessEngine {

    fun evaluate(
        hardware: HardwareSnapshot,
        powerConnected: Boolean,
        journeyGuardMode: GuardMode
    ): List<ProtectionReadiness> {
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

        return ProtectionMode.entries.map {
            mode ->

            val supported =
                ProtectionPolicy.isSupported(
                    mode = mode,
                    journeyGuardMode =
                        journeyGuardMode,
                    capabilities =
                        capabilities
                )

            val ready =
                supported &&
                    (
                        mode !=
                            ProtectionMode.CHARGING ||
                            powerConnected
                        )

            ProtectionReadiness(
                mode = mode,
                ready = ready,
                reason =
                    when {
                        !supported ->
                            "Required hardware unavailable"

                        mode ==
                            ProtectionMode.CHARGING &&
                            !powerConnected ->
                            "Connect a charger to test this mode"

                        else ->
                            "Ready"
                    }
            )
        }
    }
}
