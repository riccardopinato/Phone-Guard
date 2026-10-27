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
import com.riccardopinato.phoneguard.model.AlarmSoundProfile
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
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
        settings: AppSettings,
        mode: ProtectionMode
    ): AlarmOutputHealth {
        stopSignals()

        val volumeApplied =
            if (settings.soundEnabled) {
                volumeLease.acquire(
                    settings.alarmVolumePercent
                )
            } else {
                volumeLease.restoreIfOwned()
                true
            }

        val soundActive =
            if (settings.soundEnabled) {
                startSound(
                    settings.soundProfileFor(
                        mode
                    )
                )
            } else {
                false
            }

        val vibrationActive =
            if (settings.vibrationEnabled) {
                startVibration()
            } else {
                false
            }

        val flashActive =
            if (settings.flashEnabled) {
                flashController.startContinuous(
                    settings.flashFrequencyHz
                )
            } else {
                false
            }

        val health =
            AlarmOutputHealth(
                soundRequested =
                    settings.soundEnabled,
                soundActive =
                    soundActive &&
                        volumeApplied,
                vibrationRequested =
                    settings.vibrationEnabled,
                vibrationActive =
                    vibrationActive,
                flashRequested =
                    settings.flashEnabled,
                flashActive =
                    flashActive,
                targetVolumePercent =
                    if (settings.soundEnabled) {
                        settings.alarmVolumePercent
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

    private fun startSound(
        profile: AlarmSoundProfile
    ): Boolean =
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

            val samples =
                createSoundSamples(
                    profile
                )

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

    private fun createSoundSamples(
        profile: AlarmSoundProfile
    ): ShortArray {
        val totalSamples = SAMPLE_RATE

        return ShortArray(totalSamples) {
            index ->

            val time =
                index.toDouble() /
                    SAMPLE_RATE.toDouble()

            val amplitude =
                when (profile) {
                    AlarmSoundProfile.SIREN -> {
                        val segment =
                            index /
                                (
                                    SAMPLE_RATE /
                                        4
                                    )

                        val frequency =
                            if (
                                segment % 2 ==
                                0
                            ) {
                                880.0
                            } else {
                                660.0
                            }

                        sin(
                            2.0 *
                                PI *
                                frequency *
                                time
                        ) * 0.62
                    }

                    AlarmSoundProfile.WARNING -> {
                        val segment =
                            index /
                                (
                                    SAMPLE_RATE /
                                        8
                                    )

                        val frequency =
                            if (
                                segment % 2 ==
                                0
                            ) {
                                1_020.0
                            } else {
                                820.0
                            }

                        sin(
                            2.0 *
                                PI *
                                frequency *
                                time
                        ) * 0.54
                    }

                    AlarmSoundProfile.BEEP -> {
                        val withinCycle =
                            index %
                                (
                                    SAMPLE_RATE /
                                        2
                                    )

                        if (
                            withinCycle <
                            SAMPLE_RATE /
                                7
                        ) {
                            sin(
                                2.0 *
                                    PI *
                                    1_100.0 *
                                    time
                            ) * 0.58
                        } else {
                            0.0
                        }
                    }
                }

            (
                amplitude *
                    Short.MAX_VALUE
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
