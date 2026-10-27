package com.riccardopinato.phoneguard.pocket.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorCapabilities
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot

enum class SamplingMode {
    LOW_POWER,
    NORMAL,
    ALERT
}

class SensorController(
    context: Context,
    private val onSnapshot: (SensorSnapshot) -> Unit
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val proximity =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val light =
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gyroscope =
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val motionDetector = MotionDetector()
    private val rotationDetector = RotationDetector()

    private var currentLux: Float? =
        if (light == null) null else Float.POSITIVE_INFINITY
    private var proximityNear: Boolean? = null
    private var registered = false
    private var samplingMode = SamplingMode.NORMAL
    private var accelerometerObserved = false
    private var proximityObserved = false
    private var lightObserved = false
    private var gyroscopeObserved = false

    val capabilities = detectCapabilities(appContext)

    fun start(mode: SamplingMode = SamplingMode.NORMAL) {
        if (registered) {
            setSamplingMode(mode)
            return
        }
        samplingMode = mode
        registerAll()
        registered = true
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        motionDetector.reset()
        rotationDetector.reset()
        currentLux = if (light == null) null else Float.POSITIVE_INFINITY
        proximityNear = null
        accelerometerObserved = false
        proximityObserved = false
        lightObserved = false
        gyroscopeObserved = false
        registered = false
    }

    fun setSamplingMode(value: SamplingMode) {
        if (!registered || samplingMode == value) return
        samplingMode = value
        sensorManager.unregisterListener(this)
        registerAll()
    }

    private fun registerAll() {
        proximity?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        light?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        val periodUs = when (samplingMode) {
            SamplingMode.LOW_POWER -> 120_000
            SamplingMode.NORMAL -> 60_000
            SamplingMode.ALERT -> 20_000
        }

        accelerometer?.let {
            sensorManager.registerListener(this, it, periodUs)
        }

        gyroscope?.let {
            sensorManager.registerListener(this, it, periodUs)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = SystemClock.elapsedRealtime()

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                lightObserved = true
                currentLux = event.values.firstOrNull()
            }

            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.firstOrNull() ?: return
                proximityObserved = true
                proximityNear = distance < event.sensor.maximumRange
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size < 3) return
                accelerometerObserved = true
                motionDetector.update(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    now
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                if (event.values.size < 3) return
                gyroscopeObserved = true
                rotationDetector.update(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    now
                )
            }
        }

        onSnapshot(
            SensorSnapshot(
                lux = currentLux,
                proximityNear = proximityNear,
                motionScore = motionDetector.current(now),
                rotationScore = rotationDetector.current(now),
                timestampMs = now,
                accelerometerObserved = accelerometerObserved,
                proximityObserved = proximityObserved,
                lightObserved = lightObserved,
                gyroscopeObserved = gyroscopeObserved
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        fun detectCapabilities(context: Context): SensorCapabilities {
            val manager =
                context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

            fun sensor(type: Int): Sensor? =
                manager.getDefaultSensor(type, true)
                    ?: manager.getDefaultSensor(type)

            val proximity = sensor(Sensor.TYPE_PROXIMITY)
            val light = sensor(Sensor.TYPE_LIGHT)
            val accelerometer = sensor(Sensor.TYPE_ACCELEROMETER)
            val gyroscope = sensor(Sensor.TYPE_GYROSCOPE)

            return SensorCapabilities(
                hasProximity = proximity != null,
                hasLight = light != null,
                hasAccelerometer = accelerometer != null,
                hasGyroscope = gyroscope != null,
                proximityWakeUp = proximity?.isWakeUpSensor == true,
                lightWakeUp = light?.isWakeUpSensor == true,
                accelerometerWakeUp = accelerometer?.isWakeUpSensor == true,
                gyroscopeWakeUp = gyroscope?.isWakeUpSensor == true,
                hasFlash = detectFlash(context)
            )
        }

        private fun detectFlash(context: Context): Boolean {
            return runCatching {
                val cameraManager =
                    context.getSystemService(CameraManager::class.java)

                cameraManager.cameraIdList.any { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }.getOrDefault(false)
        }
    }
}
