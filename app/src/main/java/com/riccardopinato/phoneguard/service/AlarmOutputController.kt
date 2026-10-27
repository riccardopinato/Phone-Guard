// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/service/AlarmOutputController.kt
package com.riccardopinato.phoneguard.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class AlarmOutputController(
    private val context: Context
) {

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Default
        )

    private val cameraManager =
        context.getSystemService(
            Context.CAMERA_SERVICE
        ) as CameraManager

    private val audioManager =
        context.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    private var flashJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var audioFocusRequest:
        AudioFocusRequest? = null

    fun start(settings: AppSettings) {
        stop()

        if (settings.soundEnabled) {
            startSiren()
        }

        if (settings.vibrationEnabled) {
            startVibration()
        }

        if (settings.flashEnabled) {
            startFlash()
        }
    }

    fun stop() {
        stopFlash()
        stopVibration()
        stopSiren()
    }

    private fun startSiren() {
        runCatching {
            val attributes =
                AudioAttributes.Builder()
                    .setUsage(
                        AudioAttributes.USAGE_ALARM
                    )
                    .setContentType(
                        AudioAttributes
                            .CONTENT_TYPE_SONIFICATION
                    )
                    .build()

            val focusRequest =
                AudioFocusRequest.Builder(
                    AudioManager
                        .AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(attributes)
                    .build()

            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(
                focusRequest
            )

            val samples =
                createSirenSamples()

            val track =
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(
                                SAMPLE_RATE
                            )
                            .setEncoding(
                                AudioFormat
                                    .ENCODING_PCM_16BIT
                            )
                            .setChannelMask(
                                AudioFormat
                                    .CHANNEL_OUT_MONO
                            )
                            .build()
                    )
                    .setTransferMode(
                        AudioTrack.MODE_STATIC
                    )
                    .setBufferSizeInBytes(
                        samples.size * 2
                    )
                    .build()

            track.write(
                samples,
                0,
                samples.size,
                AudioTrack.WRITE_BLOCKING
            )

            track.setLoopPoints(
                0,
                samples.size,
                -1
            )

            track.setVolume(1.0f)
            track.play()
            audioTrack = track
        }
    }

    private fun stopSiren() {
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.release() }
        audioTrack = null

        audioFocusRequest?.let {
            runCatching {
                audioManager
                    .abandonAudioFocusRequest(it)
            }
        }

        audioFocusRequest = null
    }

    private fun createSirenSamples():
        ShortArray {

        val totalSamples = SAMPLE_RATE

        return ShortArray(totalSamples) {
            index ->

            val segment =
                index / (SAMPLE_RATE / 4)

            val frequency =
                if (segment % 2 == 0) {
                    880.0
                } else {
                    660.0
                }

            val time =
                index.toDouble() /
                    SAMPLE_RATE.toDouble()

            val sample =
                sin(
                    2.0 *
                        PI *
                        frequency *
                        time
                )

            (
                sample *
                    Short.MAX_VALUE *
                    0.62
                )
                .toInt()
                .toShort()
        }
    }

    private fun startVibration() {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val timings = longArrayOf(0,650,180,650,180)
        val amplitudes = intArrayOf(0,255,0,255,0)
        val effect = if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, 0) else VibrationEffect.createWaveform(timings, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build())
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
    }

    private fun stopVibration() {
        getVibrator()?.cancel()
    }

    private fun getVibrator(): Vibrator? {
        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {
            context
                .getSystemService(
                    VibratorManager::class.java
                )
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator
        }
    }

    private fun startFlash() {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cameraId =
            findFlashCameraId() ?: return

        flashJob = scope.launch {
            while (isActive) {
                runCatching {
                    cameraManager.setTorchMode(
                        cameraId,
                        true
                    )
                }

                delay(220)

                runCatching {
                    cameraManager.setTorchMode(
                        cameraId,
                        false
                    )
                }

                delay(180)
            }
        }
    }

    private fun stopFlash() {
        flashJob?.cancel()
        flashJob = null

        findFlashCameraId()?.let { id ->
            runCatching {
                cameraManager.setTorchMode(
                    id,
                    false
                )
            }
        }
    }

    private fun findFlashCameraId():
        String? {

        return runCatching {
            cameraManager
                .cameraIdList
                .firstOrNull { id ->
                    val c =
                        cameraManager
                            .getCameraCharacteristics(
                                id
                            )

                    val hasFlash =
                        c.get(
                            CameraCharacteristics
                                .FLASH_INFO_AVAILABLE
                        ) == true

                    val facing =
                        c.get(
                            CameraCharacteristics
                                .LENS_FACING
                        )

                    hasFlash &&
                        facing ==
                        CameraCharacteristics
                            .LENS_FACING_BACK
                }
                ?: cameraManager
                    .cameraIdList
                    .firstOrNull { id ->
                        cameraManager
                            .getCameraCharacteristics(
                                id
                            )
                            .get(
                                CameraCharacteristics
                                    .FLASH_INFO_AVAILABLE
                            ) == true
                    }
        }.getOrNull()
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
