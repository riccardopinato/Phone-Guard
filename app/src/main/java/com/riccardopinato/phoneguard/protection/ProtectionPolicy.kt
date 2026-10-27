package com.riccardopinato.phoneguard.protection

import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration

data class ProtectionCapabilities(
    val hasAccelerometer: Boolean,
    val hasProximity: Boolean,
    val hasLight: Boolean,
    val hasGyroscope: Boolean
)

object ProtectionPolicy {

    fun effectiveGuardMode(
        mode: ProtectionMode,
        journeyGuardMode: GuardMode
    ): GuardMode? =
        when (mode) {
            ProtectionMode.TABLE,
            ProtectionMode.CHARGING -> null
            ProtectionMode.POCKET -> GuardMode.POCKET
            ProtectionMode.BAG -> GuardMode.BAG
            ProtectionMode.JOURNEY -> journeyGuardMode
        }

    fun isSupported(
        mode: ProtectionMode,
        journeyGuardMode: GuardMode,
        capabilities: ProtectionCapabilities
    ): Boolean =
        when (mode) {
            ProtectionMode.CHARGING ->
                true

            else ->
                when (
                    effectiveGuardMode(
                        mode,
                        journeyGuardMode
                    )
                ) {
                    null ->
                        capabilities.hasAccelerometer

                    GuardMode.POCKET ->
                        capabilities.hasAccelerometer &&
                            capabilities.hasProximity

                    GuardMode.BAG ->
                        capabilities.hasAccelerometer &&
                            (
                                capabilities.hasLight ||
                                    capabilities.hasGyroscope
                                )
                }
        }

    fun journeyEndAt(
        now: Long,
        duration: JourneyDuration
    ): Long? =
        duration.durationMs
            ?.let { now + it }

    fun remainingJourneyMs(
        endAt: Long?,
        now: Long
    ): Long? =
        endAt?.let {
            (it - now)
                .coerceAtLeast(0L)
        }

    fun remainingJourneyMs(
        endAtElapsedRealtime: Long?,
        nowElapsedRealtime: Long,
        endAtWallClock: Long?,
        nowWallClock: Long
    ): Long? =
        endAtElapsedRealtime
            ?.let {
                (
                    it -
                        nowElapsedRealtime
                    )
                    .coerceAtLeast(0L)
            }
            ?: remainingJourneyMs(
                endAtWallClock,
                nowWallClock
            )

    fun isJourneyExpired(
        endAt: Long?,
        now: Long
    ): Boolean =
        endAt != null &&
            endAt <= now
}
