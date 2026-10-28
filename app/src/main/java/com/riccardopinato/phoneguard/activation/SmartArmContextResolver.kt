package com.riccardopinato.phoneguard.activation

import android.content.Context
import com.riccardopinato.phoneguard.accessory.AccessoryStateMonitor
import com.riccardopinato.phoneguard.charging.ChargingStateMonitor
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import com.riccardopinato.phoneguard.pocket.sensor.SamplingMode
import com.riccardopinato.phoneguard.pocket.sensor.SensorController
import kotlinx.coroutines.delay

data class SmartArmObservation(
    val lux: Float? = null,
    val proximityNear: Boolean? = null,
    val motionScore: Float = 0f,
    val rotationScore: Float = 0f,
    val accelerometerObserved: Boolean = false,
    val proximityObserved: Boolean = false,
    val lightObserved: Boolean = false,
    val gyroscopeObserved: Boolean = false
)

enum class SmartArmReason {
    EXTERNAL_POWER,
    ACCESSORY_CONNECTED,
    POCKET_CONTEXT,
    BAG_CONTEXT,
    STABLE_SURFACE,
    FALLBACK
}

data class SmartArmDecision(
    val mode: ProtectionMode,
    val reason: SmartArmReason,
    val confidence: Int
)

object SmartArmDecisionEngine {
    fun resolve(
        externalPowerConnected: Boolean,
        accessoryConnected: Boolean,
        observation: SmartArmObservation?,
        fallbackMode: ProtectionMode
    ): SmartArmDecision {
        if (externalPowerConnected) {
            return SmartArmDecision(
                mode = ProtectionMode.CHARGING,
                reason = SmartArmReason.EXTERNAL_POWER,
                confidence = 100
            )
        }

        if (accessoryConnected) {
            return SmartArmDecision(
                mode = ProtectionMode.ACCESSORY,
                reason = SmartArmReason.ACCESSORY_CONNECTED,
                confidence = 95
            )
        }

        if (
            observation != null &&
            observation.accelerometerObserved &&
            observation.motionScore.isFinite() &&
            observation.motionScore >= 0f &&
            (!observation.lightObserved ||
                (observation.lux != null && observation.lux.isFinite() &&
                    observation.lux >= 0f)) &&
            (!observation.gyroscopeObserved ||
                (observation.rotationScore.isFinite() &&
                    observation.rotationScore >= 0f))
        ) {
            if (
                observation.proximityObserved &&
                observation.proximityNear == true
            ) {
                return SmartArmDecision(
                    mode = ProtectionMode.POCKET,
                    reason = SmartArmReason.POCKET_CONTEXT,
                    confidence = 88
                )
            }

            if (
                observation.proximityNear != true &&
                observation.lightObserved &&
                observation.lux != null &&
                observation.lux in 0f..BAG_LUX_THRESHOLD &&
                (
                    (observation.gyroscopeObserved &&
                        observation.rotationScore >= BAG_ROTATION_EVIDENCE) ||
                        observation.motionScore >= BAG_MOTION_EVIDENCE
                )
            ) {
                return SmartArmDecision(
                    mode = ProtectionMode.BAG,
                    reason = SmartArmReason.BAG_CONTEXT,
                    confidence = 80
                )
            }

            if (observation.motionScore <= TABLE_MOTION_MAX &&
                (!observation.gyroscopeObserved ||
                    observation.rotationScore <= TABLE_ROTATION_MAX)
            ) {
                return SmartArmDecision(
                    mode = ProtectionMode.TABLE,
                    reason = SmartArmReason.STABLE_SURFACE,
                    confidence = 72
                )
            }
        }

        val safeFallback =
            when (fallbackMode) {
                ProtectionMode.CHARGING,
                ProtectionMode.ACCESSORY ->
                    ProtectionMode.TABLE

                else ->
                    fallbackMode
            }

        return SmartArmDecision(
            mode = safeFallback,
            reason = SmartArmReason.FALLBACK,
            confidence = 55
        )
    }

    private const val BAG_LUX_THRESHOLD =
        8f

    private const val BAG_MOTION_EVIDENCE = 0.02f
    private const val BAG_ROTATION_EVIDENCE = 0.018f
    private const val TABLE_MOTION_MAX = 0.035f
    private const val TABLE_ROTATION_MAX = 0.12f
}

class SmartArmContextResolver(
    context: Context
) {
    private val appContext =
        context.applicationContext

    suspend fun resolve(
        fallbackMode: ProtectionMode
    ): SmartArmDecision {
        if (
            ChargingStateMonitor(
                appContext
            ).isPowerConnected()
        ) {
            return SmartArmDecisionEngine.resolve(
                externalPowerConnected = true,
                accessoryConnected = false,
                observation = null,
                fallbackMode = fallbackMode
            )
        }

        if (
            AccessoryStateMonitor(
                appContext
            ).hasConnectedAccessory()
        ) {
            return SmartArmDecisionEngine.resolve(
                externalPowerConnected = false,
                accessoryConnected = true,
                observation = null,
                fallbackMode = fallbackMode
            )
        }

        return SmartArmDecisionEngine.resolve(
            externalPowerConnected = false,
            accessoryConnected = false,
            observation =
                sampleObservation(),
            fallbackMode =
                fallbackMode
        )
    }

    private suspend fun sampleObservation():
        SmartArmObservation? {
        var latest:
            SensorSnapshot? = null

        val controller =
            SensorController(
                appContext
            ) {
                snapshot ->
                latest =
                    snapshot
            }

        return try {
            controller.start(
                SamplingMode.ALERT
            )

            delay(
                SAMPLE_WINDOW_MS
            )

            latest
                ?.toObservation()
        } finally {
            controller.stop()
        }
    }

    private fun SensorSnapshot.toObservation():
        SmartArmObservation =
        SmartArmObservation(
            lux = lux,
            proximityNear =
                proximityNear,
            motionScore =
                motionScore,
            rotationScore =
                rotationScore,
            accelerometerObserved =
                accelerometerObserved,
            proximityObserved =
                proximityObserved,
            lightObserved =
                lightObserved,
            gyroscopeObserved =
                gyroscopeObserved
        )

    private companion object {
        const val SAMPLE_WINDOW_MS =
            900L
    }
}
