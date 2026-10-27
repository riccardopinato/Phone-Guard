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
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import com.riccardopinato.phoneguard.alarm.FlashStrobeController
import com.riccardopinato.phoneguard.model.AlarmOutputProfile
import kotlin.math.PI
import kotlin.math.sin

class AlarmOutputController(
    private val context: Context,
    private val volumeLease:
        AlarmVolumeLeaseManager =
        AlarmVolumeLeaseManager(context)
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val flashController =
        FlashStrobeController(context)

    private var audioTrack: AudioTrack? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    fun start(
        profile: AlarmOutputProfile
    ): AlarmOutputHealth {
        stopSignals()

        val volumeApplied =
            if (profile.soundEnabled) {
                volumeLease.acquire(
                    profile.alarmVolumePercent
                )
            } else {
                volumeLease.restoreIfOwned()
                true
            }

        val soundActive =
            if (profile.soundEnabled) {
                startSiren()
            } else {
                false
            }

        val vibrationActive =
            if (profile.vibrationEnabled) {
                startVibration()
            } else {
                false
            }

        val flashActive =
            if (profile.flashEnabled) {
                flashController.startContinuous(
                    profile.flashFrequencyHz
                )
            } else {
                false
            }

        val health =
            AlarmOutputHealth(
                soundRequested =
                    profile.soundEnabled,
                soundActive =
                    soundActive &&
                        volumeApplied,
                vibrationRequested =
                    profile.vibrationEnabled,
                vibrationActive =
                    vibrationActive,
                flashRequested =
                    profile.flashEnabled,
                flashActive =
                    flashActive,
                targetVolumePercent =
                    if (profile.soundEnabled) {
                        profile.alarmVolumePercent
                    } else {
                        0
                    },
                effectiveVolumePercent =
                    effectiveAlarmVolumePercent()
            )

        AlarmOutputHealthStore.update(
            health
        )

        return health
    }

    fun stop() {
        stopSignals()
        volumeLease.restoreIfOwned()
        AlarmOutputHealthStore.reset()
    }

    private fun stopSignals() {
        flashController.stop()
        stopVibration()
        stopSiren()
    }

    fun effectiveAlarmVolumePercent(): Int =
        volumeLease.currentPercent()

    private fun startSiren():
        Boolean =
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
            true
        }.getOrDefault(false)

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

    private fun startVibration():
        Boolean {
        val vibrator =
            getVibrator()
                ?: return false

        if (!vibrator.hasVibrator()) {
            return false
        }

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

        return true
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
