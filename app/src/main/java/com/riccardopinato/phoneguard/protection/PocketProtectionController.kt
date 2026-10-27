package com.riccardopinato.phoneguard.protection

import android.content.Context
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
                "Sensori insufficienti per la modalità ${mode.name}."
            )
            return
        }

        calibrationCollector =
            CalibrationCollector(mode)

        sensors.start(SamplingMode.NORMAL)

        calibrationJob =
            scope.launch {
                delay(durationMs)

                val collector =
                    calibrationCollector
                        ?: return@launch

                val profile =
                    collector.build(
                        System.currentTimeMillis()
                    )

                calibrationCollector = null
                sensors.stop()
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

        onDecision(decision)
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

        onDecision(decision)
        return decision
    }

    fun stop() {
        calibrationJob?.cancel()
        calibrationJob = null
        calibrationCollector = null
        engine = null
        sensors.stop()
    }

    private fun handleSnapshot(
        snapshot: SensorSnapshot
    ) {
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
