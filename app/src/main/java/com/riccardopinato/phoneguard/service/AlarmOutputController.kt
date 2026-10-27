package com.riccardopinato.phoneguard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.riccardopinato.phoneguard.alarm.AlarmVolumeController
import com.riccardopinato.phoneguard.alarm.FlashStrobeController
import com.riccardopinato.phoneguard.model.AppSettings
import kotlin.math.PI
import kotlin.math.sin

class AlarmOutputController(
    private val context: Context
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val flashController =
        FlashStrobeController(context)

    private val volumeController =
        AlarmVolumeController(context)

    private var audioTrack: AudioTrack? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    fun start(settings: AppSettings) {
        stop()

        if (settings.soundEnabled) {
            volumeController.applyTarget(settings.alarmVolumePercent)
            startSiren()
        }

        if (settings.vibrationEnabled) {
            startVibration()
        }

        if (settings.flashEnabled) {
            flashController.startContinuous(
                settings.flashFrequencyHz
            )
        }
    }

    fun stop() {
        flashController.stop()
        stopVibration()
        stopSiren()
        volumeController.restore()
    }

    fun effectiveAlarmVolumePercent(): Int =
        volumeController.currentPercent()

    private fun startSiren() {
        runCatching {
            val attributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()

            val focusRequest =
                AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(attributes)
                    .build()

            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)

            val samples = createSirenSamples()

            val track =
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                            .setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO
                            )
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * 2)
                    .build()

            track.write(
                samples,
                0,
                samples.size,
                AudioTrack.WRITE_BLOCKING
            )

            track.setLoopPoints(0, samples.size, -1)
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
                audioManager.abandonAudioFocusRequest(it)
            }
        }
        audioFocusRequest = null
    }

    private fun createSirenSamples(): ShortArray {
        val totalSamples = SAMPLE_RATE

        return ShortArray(totalSamples) { index ->
            val segment = index / (SAMPLE_RATE / 4)
            val frequency =
                if (segment % 2 == 0) 880.0 else 660.0
            val time =
                index.toDouble() / SAMPLE_RATE.toDouble()

            (
                sin(2.0 * PI * frequency * time) *
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

        val timings =
            longArrayOf(0, 650, 180, 650, 180)
        val amplitudes =
            intArrayOf(0, 255, 0, 255, 0)

        val effect =
            if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(
                    timings,
                    amplitudes,
                    0
                )
            } else {
                VibrationEffect.createWaveform(
                    timings,
                    0
                )
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )
        }
    }

    private fun stopVibration() {
        getVibrator()?.cancel()
    }

    private fun getVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context
                .getSystemService(VibratorManager::class.java)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as? Vibrator
        }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}
