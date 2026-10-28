package com.riccardopinato.phoneguard.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.riccardopinato.phoneguard.model.Sensitivity
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.sqrt

data class MotionTrigger(
    val accelerationDelta: Float,
    val gyroscopeMagnitude: Float
)

data class MotionCalibration(
    val baselineX: Float,
    val baselineY: Float,
    val baselineZ: Float,
    val noiseFloor: Float
)

internal object MotionDetectionPolicy {
    private const val NOISE_MULTIPLIER = 5.0f

    fun accelerationThreshold(
        sensitivity: Sensitivity,
        noiseFloor: Float
    ): Float =
        max(
            when (sensitivity) {
                Sensitivity.HIGH -> 0.75f
                Sensitivity.MEDIUM -> 1.35f
                Sensitivity.LOW -> 2.20f
            },
            noiseFloor * NOISE_MULTIPLIER + 0.20f
        )

    fun gyroscopeThreshold(
        sensitivity: Sensitivity
    ): Float =
        when (sensitivity) {
            Sensitivity.HIGH -> 0.22f
            Sensitivity.MEDIUM -> 0.38f
            Sensitivity.LOW -> 0.65f
        }

    fun requiredHits(
        sensitivity: Sensitivity
    ): Int =
        when (sensitivity) {
            Sensitivity.HIGH -> 2
            Sensitivity.MEDIUM -> 3
            Sensitivity.LOW -> 3
        }
}

class MotionDetector(
    context: Context,
    private val sensitivity: Sensitivity,
    private val initialCalibration: MotionCalibration? = null,
    private val onCalibrated: (MotionCalibration) -> Unit,
    private val onMovement: (MotionTrigger) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER,
            true
        ) ?: sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )

    private val gyroscope =
        sensorManager.getDefaultSensor(
            Sensor.TYPE_GYROSCOPE,
            true
        ) ?: sensorManager.getDefaultSensor(
            Sensor.TYPE_GYROSCOPE
        )

    private val calibrationSamples =
        mutableListOf<FloatArray>()

    private val hitTimes =
        ArrayDeque<Long>()

    private var startedAt = 0L
    private var calibrated = false
    private var triggered = false

    private var baselineX = 0f
    private var baselineY = 0f
    private var baselineZ = 0f
    private var noiseFloor = 0f

    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private var filterInitialized = false

    fun start(): Boolean {
        val accel = accelerometer ?: return false

        reset()
        startedAt = SystemClock.elapsedRealtime()

        initialCalibration?.let {
            baselineX = it.baselineX
            baselineY = it.baselineY
            baselineZ = it.baselineZ
            noiseFloor = it.noiseFloor
            filteredX = it.baselineX
            filteredY = it.baselineY
            filteredZ = it.baselineZ
            filterInitialized = true
            calibrated = true
        }

        val accelOk = sensorManager.registerListener(
            this,
            accel,
            SENSOR_PERIOD_US
        )

        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SENSOR_PERIOD_US
            )
        }

        if (accelOk && initialCalibration != null) {
            onCalibrated(initialCalibration)
        }

        return accelOk
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (triggered) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER ->
                handleAccelerometer(event)

            Sensor.TYPE_GYROSCOPE ->
                handleGyroscope(event)
        }
    }

    private fun handleAccelerometer(
        event: SensorEvent
    ) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!calibrated) {
            calibrationSamples.add(
                floatArrayOf(x, y, z)
            )
            maybeFinishCalibration()
            return
        }

        if (!filterInitialized) {
            filteredX = x
            filteredY = y
            filteredZ = z
            filterInitialized = true
        } else {
            filteredX =
                LOW_PASS_ALPHA * filteredX +
                    (1f - LOW_PASS_ALPHA) * x

            filteredY =
                LOW_PASS_ALPHA * filteredY +
                    (1f - LOW_PASS_ALPHA) * y

            filteredZ =
                LOW_PASS_ALPHA * filteredZ +
                    (1f - LOW_PASS_ALPHA) * z
        }

        val delta = distance(
            filteredX,
            filteredY,
            filteredZ,
            baselineX,
            baselineY,
            baselineZ
        )

        val threshold =
            MotionDetectionPolicy
                .accelerationThreshold(
                    sensitivity,
                    noiseFloor
                )

        if (delta >= threshold) {
            registerHit(
                accelerationDelta = delta,
                gyroscopeMagnitude = 0f
            )
        } else if (delta < threshold * 0.30f) {
            baselineX =
                baselineX * 0.999f +
                    filteredX * 0.001f

            baselineY =
                baselineY * 0.999f +
                    filteredY * 0.001f

            baselineZ =
                baselineZ * 0.999f +
                    filteredZ * 0.001f
        }
    }

    private fun handleGyroscope(
        event: SensorEvent
    ) {
        if (!calibrated) return

        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]

        val magnitude =
            sqrt(gx * gx + gy * gy + gz * gz)

        if (
            magnitude >=
            MotionDetectionPolicy
                .gyroscopeThreshold(
                    sensitivity
                )
        ) {
            registerHit(
                accelerationDelta = 0f,
                gyroscopeMagnitude = magnitude
            )
        }
    }

    private fun maybeFinishCalibration() {
        val elapsed =
            SystemClock.elapsedRealtime() - startedAt

        if (
            elapsed < CALIBRATION_MS ||
            calibrationSamples.size <
                MIN_CALIBRATION_SAMPLES
        ) {
            return
        }

        baselineX =
            calibrationSamples
                .map { it[0] }
                .average()
                .toFloat()

        baselineY =
            calibrationSamples
                .map { it[1] }
                .average()
                .toFloat()

        baselineZ =
            calibrationSamples
                .map { it[2] }
                .average()
                .toFloat()

        val squaredDistances =
            calibrationSamples.map {
                val d = distance(
                    it[0],
                    it[1],
                    it[2],
                    baselineX,
                    baselineY,
                    baselineZ
                )
                d * d
            }

        noiseFloor =
            sqrt(squaredDistances.average())
                .toFloat()

        filteredX = baselineX
        filteredY = baselineY
        filteredZ = baselineZ
        filterInitialized = true
        calibrated = true
        calibrationSamples.clear()

        onCalibrated(
            MotionCalibration(
                baselineX = baselineX,
                baselineY = baselineY,
                baselineZ = baselineZ,
                noiseFloor = noiseFloor
            )
        )
    }

    private fun registerHit(
        accelerationDelta: Float,
        gyroscopeMagnitude: Float
    ) {
        val now =
            SystemClock.elapsedRealtime()

        while (hitTimes.isNotEmpty()) {
            val oldest = hitTimes.peekFirst() ?: break
            if (now - oldest <= HIT_WINDOW_MS) break
            hitTimes.removeFirst()
        }

        hitTimes.addLast(now)

        if (
            hitTimes.size >=
            MotionDetectionPolicy
                .requiredHits(
                    sensitivity
                )
        ) {
            triggered = true

            onMovement(
                MotionTrigger(
                    accelerationDelta =
                        accelerationDelta,
                    gyroscopeMagnitude =
                        gyroscopeMagnitude
                )
            )
        }
    }

    private fun reset() {
        calibrationSamples.clear()
        hitTimes.clear()
        calibrated = false
        triggered = false
        filterInitialized = false
        noiseFloor = 0f
        baselineX = 0f
        baselineY = 0f
        baselineZ = 0f
        filteredX = 0f
        filteredY = 0f
        filteredZ = 0f
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) = Unit

    private fun distance(
        x1: Float,
        y1: Float,
        z1: Float,
        x2: Float,
        y2: Float,
        z2: Float
    ): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        val dz = z1 - z2

        return sqrt(
            dx * dx + dy * dy + dz * dz
        )
    }

    private companion object {
        const val SENSOR_PERIOD_US = 50_000
        const val CALIBRATION_MS = 1_500L
        const val MIN_CALIBRATION_SAMPLES = 20
        const val HIT_WINDOW_MS = 350L
        const val LOW_PASS_ALPHA = 0.72f
    }
}
