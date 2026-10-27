package com.riccardopinato.phoneguard.protection

import android.content.Context
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.pocket.calibration.CalibrationCollector
import com.riccardopinato.phoneguard.pocket.detection.SmartProtectionEngine
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.DetectionDecision
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
import com.riccardopinato.phoneguard.pocket.model.SensorCapabilities
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import com.riccardopinato.phoneguard.pocket.sensor.SamplingMode
import com.riccardopinato.phoneguard.pocket.sensor.SensorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PocketProtectionController(
    context: Context,
    private val scope: CoroutineScope,
    private val onDecision: (DetectionDecision) -> Unit
) {
    private val appContext =
        context.applicationContext

    private val sensors =
        SensorController(
            context = context,
            onSnapshot = ::handleSnapshot
        )

    private var calibrationCollector:
        CalibrationCollector? = null

    private var calibrationJob: Job? = null

    private var engine:
        SmartProtectionEngine? = null

    private var lastSnapshot:
        SensorSnapshot? = null

    val capabilities: SensorCapabilities
        get() = sensors.capabilities

    fun calibrate(
        mode: GuardMode,
        durationMs: Long = DEFAULT_CALIBRATION_MS,
        onComplete: (CalibrationProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()

        if (!capabilities.canRun(mode)) {
            onError(
                appContext.getString(
                    R.string.svc_sensors_insufficient,
                    mode.name
                )
            )
            return
        }

        calibrationCollector =
            CalibrationCollector(
                mode = mode,
                hasLightSensor =
                    capabilities.hasLight,
                hasGyroscopeSensor =
                    capabilities.hasGyroscope,
                hasAccelerometerSensor =
                    capabilities.hasAccelerometer,
                hasProximitySensor =
                    capabilities.hasProximity
            )

        sensors.start(SamplingMode.NORMAL)

        calibrationJob =
            scope.launch {
                delay(durationMs)

                val collector =
                    calibrationCollector
                        ?: return@launch

                val evidenceReady =
                    collector.hasRequiredEvidence()

                val profile =
                    collector.build(
                        System.currentTimeMillis()
                    )

                calibrationCollector = null
                calibrationJob = null
                sensors.stop()

                if (!evidenceReady) {
                    onError(
                        appContext.getString(
                            R.string.svc_calibration_evidence_insufficient,
                            mode.name
                        )
                    )
                    return@launch
                }

                onComplete(profile)
            }
    }

    fun start(
        settings: GuardSettings,
        profile: CalibrationProfile
    ): Boolean {
        stop()

        if (!capabilities.canRun(settings.mode)) {
            return false
        }

        engine =
            SmartProtectionEngine(
                settings = settings,
                profile = profile
            )

        sensors.start(SamplingMode.NORMAL)
        return true
    }

    fun currentState(): GuardState? =
        engine?.currentState()

    fun lastSensorSnapshot():
        SensorSnapshot? =
        lastSnapshot

    fun ownerVerified(): DetectionDecision? {
        val active = engine ?: return null

        if (
            active.currentState() !=
            GuardState.OWNER_CHECK
        ) {
            return null
        }

        val decision =
            active.ownerVerified()

        sensors.setSamplingMode(
            SamplingMode.NORMAL
        )

        return decision
    }

    fun ownerCheckExpired(): DetectionDecision? {
        val active = engine ?: return null

        if (
            active.currentState() !=
            GuardState.OWNER_CHECK
        ) {
            return null
        }

        val decision =
            active.ownerCheckExpired()

        sensors.setSamplingMode(
            SamplingMode.ALERT
        )

        return decision
    }

    fun stop() {
        calibrationJob?.cancel()
        calibrationJob = null
        calibrationCollector = null
        engine = null
        lastSnapshot = null
        sensors.stop()
    }

    private fun handleSnapshot(
        snapshot: SensorSnapshot
    ) {
        lastSnapshot = snapshot
        calibrationCollector?.add(snapshot)

        val activeEngine =
            engine ?: return

        val decision =
            activeEngine.process(snapshot)

        when (decision.state) {
            GuardState.POCKET_CONFIRMED ->
                sensors.setSamplingMode(
                    SamplingMode.LOW_POWER
                )

            GuardState.REMOVAL_SUSPECTED,
            GuardState.OWNER_CHECK,
            GuardState.ALARM ->
                sensors.setSamplingMode(
                    SamplingMode.ALERT
                )

            GuardState.WAITING_FOR_POCKET ->
                sensors.setSamplingMode(
                    SamplingMode.NORMAL
                )

            else -> Unit
        }

        onDecision(decision)
    }

    private companion object {
        const val DEFAULT_CALIBRATION_MS =
            1_500L
    }
}
