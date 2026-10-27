package com.riccardopinato.phoneguard.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

data class HardwareSnapshot(
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasProximity: Boolean,
    val hasLight: Boolean,
    val hasFlash: Boolean,
    val hasVibrator: Boolean
)

class HardwareDiagnostics(
    private val context: Context
) {
    private val sensorManager =
        context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

    private val cameraManager =
        context.getSystemService(
            Context.CAMERA_SERVICE
        ) as CameraManager

    fun snapshot(): HardwareSnapshot =
        HardwareSnapshot(
            hasAccelerometer =
                sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER
                ) != null,
            hasGyroscope =
                sensorManager.getDefaultSensor(
                    Sensor.TYPE_GYROSCOPE
                ) != null,
            hasProximity =
                sensorManager.getDefaultSensor(
                    Sensor.TYPE_PROXIMITY
                ) != null,
            hasLight =
                sensorManager.getDefaultSensor(
                    Sensor.TYPE_LIGHT
                ) != null,
            hasFlash =
                findFlashCameraId() != null,
            hasVibrator =
                getVibrator()?.hasVibrator() == true
        )

    fun testSound() {
        val tone =
            ToneGenerator(
                AudioManager.STREAM_ALARM,
                80
            )

        tone.startTone(
            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
            750
        )

        Handler(
            Looper.getMainLooper()
        ).postDelayed(
            {
                runCatching {
                    tone.release()
                }
            },
            900
        )
    }

    fun testVibration(): Boolean {
        val vibrator =
            getVibrator()
                ?: return false

        if (!vibrator.hasVibrator()) {
            return false
        }

        val effect =
            if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(
                    longArrayOf(0, 700, 150, 700),
                    intArrayOf(0, 255, 0, 255),
                    -1
                )
            } else {
                VibrationEffect.createWaveform(
                    longArrayOf(0, 700, 150, 700),
                    -1
                )
            }

        return runCatching {
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.Builder()
                        .setUsage(
                            VibrationAttributes.USAGE_ALARM
                        )
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(
                            AudioAttributes.USAGE_ALARM
                        )
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )
            }

            true
        }.getOrDefault(false)
    }

    suspend fun testFlash(): Boolean {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val id =
            findFlashCameraId()
                ?: return false

        return runCatching {
            cameraManager.setTorchMode(
                id,
                true
            )

            delay(550)

            cameraManager.setTorchMode(
                id,
                false
            )

            true
        }.getOrElse {
            runCatching {
                cameraManager.setTorchMode(
                    id,
                    false
                )
            }
            false
        }
    }

    private fun getVibrator(): Vibrator? =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            context.getSystemService(
                VibratorManager::class.java
            )?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator
        }

    private fun findFlashCameraId():
        String? =
        runCatching {
            cameraManager
                .cameraIdList
                .firstOrNull { id ->
                    val c =
                        cameraManager.getCameraCharacteristics(id)

                    val hasFlash =
                        c.get(
                            CameraCharacteristics.FLASH_INFO_AVAILABLE
                        ) == true

                    val facing =
                        c.get(
                            CameraCharacteristics.LENS_FACING
                        )

                    hasFlash &&
                        facing ==
                        CameraCharacteristics.LENS_FACING_BACK
                }
                ?: cameraManager
                    .cameraIdList
                    .firstOrNull { id ->
                        cameraManager
                            .getCameraCharacteristics(id)
                            .get(
                                CameraCharacteristics.FLASH_INFO_AVAILABLE
                            ) == true
                    }
        }.getOrNull()
}
