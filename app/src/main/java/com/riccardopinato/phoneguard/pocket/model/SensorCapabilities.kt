package com.riccardopinato.phoneguard.pocket.model

data class SensorCapabilities(
    val hasProximity: Boolean,
    val hasLight: Boolean,
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val proximityWakeUp: Boolean,
    val lightWakeUp: Boolean,
    val accelerometerWakeUp: Boolean,
    val gyroscopeWakeUp: Boolean,
    val hasFlash: Boolean
) {
    val canRunPocket: Boolean get() = hasProximity && hasAccelerometer
    val canRunBag: Boolean get() = hasAccelerometer

    fun canRun(mode: GuardMode): Boolean = when (mode) {
        GuardMode.POCKET -> canRunPocket
        GuardMode.BAG -> canRunBag
    }
}
