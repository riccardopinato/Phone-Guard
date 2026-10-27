package com.riccardopinato.phoneguard.autoguard

import android.content.Context
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import com.riccardopinato.phoneguard.pocket.sensor.SamplingMode
import com.riccardopinato.phoneguard.pocket.sensor.SensorController
import kotlinx.coroutines.delay

/**
 * Bounded one-shot sampling. Registers listeners only for the sample window.
 * The foreground-service owner schedules calls; no background singleton or GPS.
 */
class AutoGuardSampler(context: Context) {
    private val appContext = context.applicationContext

    suspend fun sample(): AutoGuardObservation? {
        var latest: SensorSnapshot? = null
        var peakMotion = 0f
        var peakRotation = 0f
        val controller = SensorController(appContext) { snapshot ->
            latest = snapshot
            if (snapshot.accelerometerObserved) {
                peakMotion = maxOf(peakMotion, snapshot.motionScore)
            }
            if (snapshot.gyroscopeObserved) {
                peakRotation = maxOf(peakRotation, snapshot.rotationScore)
            }
        }
        return try {
            controller.start(SamplingMode.LOW_POWER)
            delay(SAMPLE_WINDOW_MS)
            latest?.let {
                AutoGuardObservation(
                    lightObserved = it.lightObserved,
                    lux = it.lux,
                    proximityObserved = it.proximityObserved,
                    proximityNear = it.proximityNear,
                    accelerometerObserved = it.accelerometerObserved,
                    motionScore = peakMotion,
                    gyroscopeObserved = it.gyroscopeObserved,
                    rotationScore = peakRotation
                )
            }
        } finally {
            controller.stop()
        }
    }

    private companion object {
        const val SAMPLE_WINDOW_MS = 1_600L
    }
}
