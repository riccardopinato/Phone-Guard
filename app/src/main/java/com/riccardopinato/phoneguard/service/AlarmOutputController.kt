package com.riccardopinato.phoneguard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import com.riccardopinato.phoneguard.alarm.FlashStrobeController
import com.riccardopinato.phoneguard.model.AlarmOutputProfile
import com.riccardopinato.phoneguard.model.PreAlarmStyle
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

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private val customAudioThread =
        HandlerThread(
            "PhoneGuardCustomAudio"
        ).apply {
            start()
        }

    private val customAudioHandler =
        Handler(
            customAudioThread.looper
        )

    @Volatile
    private var customRequestToken =
        0L

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var preAlarmTone: ToneGenerator? = null
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
                startSiren(
                    profile.customSoundUri
                )
            } else {
                false
            }

        if (
            profile.soundEnabled &&
            !soundActive
        ) {
            volumeLease.restoreIfOwned()
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

    fun startPreAlarm(
        style: PreAlarmStyle
    ): Boolean {
        stopPreAlarm()

        return when (style) {
            PreAlarmStyle.SILENT ->
                true

            PreAlarmStyle.VIBRATION ->
                startPreAlarmVibration()

            PreAlarmStyle.BEEP ->
                startPreAlarmBeep()
        }
    }

    fun stopPreAlarm() {
        runCatching {
            preAlarmTone
                ?.stopTone()
        }
        runCatching {
            preAlarmTone
                ?.release()
        }
        preAlarmTone = null
        stopVibration()
    }

    fun stop() {
        stopSignals()
        volumeLease.restoreIfOwned()
        AlarmOutputHealthStore.reset()
    }

    private fun stopSignals() {
        stopPreAlarm()
        flashController.stop()
        stopVibration()
        stopSiren()
    }

    fun effectiveAlarmVolumePercent(): Int =
        volumeLease.currentPercent()

    private fun startSiren(
        customSoundUri: String?
    ): Boolean {
        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ALARM
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        requestAudioFocus(
            attributes
        )

        val fallbackStarted =
            startGeneratedSiren(
                attributes
            )

        customSoundUri
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                prepareCustomSoundAsync(
                    uriString = it,
                    attributes = attributes
                )
            }

        if (
            !fallbackStarted &&
            customSoundUri
                .isNullOrBlank()
        ) {
            stopSiren()
        }

        return fallbackStarted
    }

    private fun requestAudioFocus(
        attributes: AudioAttributes
    ): Boolean =
        runCatching {
            val request =
                AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                    .setAudioAttributes(
                        attributes
                    )
                    .build()

            audioFocusRequest =
                request

            audioManager
                .requestAudioFocus(
                    request
                ) ==
                AudioManager
                    .AUDIOFOCUS_REQUEST_GRANTED
        }.getOrDefault(
            false
        )

    private fun prepareCustomSoundAsync(
        uriString: String,
        attributes: AudioAttributes
    ) {
        val token =
            ++customRequestToken

        customAudioHandler.post {
            var candidate:
                MediaPlayer? = null

            try {
                if (
                    token !=
                    customRequestToken
                ) {
                    return@post
                }

                val player =
                    MediaPlayer()

                candidate =
                    player

                player.setAudioAttributes(
                    attributes
                )
                player.setDataSource(
                    context,
                    Uri.parse(
                        uriString
                    )
                )
                player.isLooping =
                    true

                player.setOnPreparedListener {
                    prepared ->
                    mainHandler.post {
                        if (
                            token !=
                            customRequestToken
                        ) {
                            releaseMediaPlayerAsync(
                                prepared
                            )
                            return@post
                        }

                        val started =
                            runCatching {
                                prepared.start()
                                true
                            }.getOrDefault(
                                false
                            )

                        if (
                            started
                        ) {
                            mediaPlayer =
                                prepared

                            stopGeneratedSiren()

                            val current =
                                AlarmOutputHealthStore
                                    .state
                                    .value

                            if (
                                current
                                    .soundRequested
                            ) {
                                AlarmOutputHealthStore
                                    .update(
                                        current.copy(
                                            soundActive =
                                                true,
                                            effectiveVolumePercent =
                                                effectiveAlarmVolumePercent()
                                        )
                                    )
                            }
                        } else {
                            releaseMediaPlayerAsync(
                                prepared
                            )
                        }
                    }
                }

                player.setOnErrorListener {
                    failed,
                    _,
                    _ ->
                    mainHandler.post {
                        if (
                            mediaPlayer ===
                            failed
                        ) {
                            mediaPlayer =
                                null
                        }

                        releaseMediaPlayerAsync(
                            failed
                        )
                    }
                    true
                }

                player.prepareAsync()

                mainHandler.postDelayed(
                    {
                        if (
                            token ==
                                customRequestToken &&
                            mediaPlayer ==
                                null
                        ) {
                            customRequestToken +=
                                1L

                            candidate
                                ?.let(
                                    ::releaseMediaPlayerAsync
                                )
                        }
                    },
                    CUSTOM_PREPARE_TIMEOUT_MS
                )
            } catch (
                _: Exception
            ) {
                candidate
                    ?.let(
                        ::releaseMediaPlayerAsync
                    )
            }
        }
    }

    private fun releaseMediaPlayerAsync(
        player: MediaPlayer
    ) {
        customAudioHandler.post {
            runCatching {
                player.stop()
            }
            runCatching {
                player.reset()
            }
            runCatching {
                player.release()
            }
        }
    }

    private fun startGeneratedSiren(
        attributes: AudioAttributes
    ): Boolean {
        var candidate:
            AudioTrack? = null

        return runCatching {
            val samples =
                createSirenSamples()

            val track =
                AudioTrack.Builder()
                    .setAudioAttributes(
                        attributes
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(
                                SAMPLE_RATE
                            )
                            .setEncoding(
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                            .setChannelMask(
                                AudioFormat.CHANNEL_OUT_MONO
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

            candidate =
                track

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
            track.setVolume(
                1.0f
            )
            track.play()

            audioTrack =
                track

            true
        }.getOrElse {
            runCatching {
                candidate
                    ?.release()
            }
            audioTrack = null
            false
        }
    }

    private fun stopSiren() {
        customRequestToken +=
            1L

        mediaPlayer
            ?.let(
                ::releaseMediaPlayerAsync
            )
        mediaPlayer =
            null

        stopGeneratedSiren()

        audioFocusRequest?.let {
            runCatching {
                audioManager.abandonAudioFocusRequest(it)
            }
        }
        audioFocusRequest = null
    }

    private fun stopGeneratedSiren() {
        runCatching {
            audioTrack?.stop()
        }
        runCatching {
            audioTrack?.flush()
        }
        runCatching {
            audioTrack?.release()
        }
        audioTrack = null
    }

    fun release() {
        stop()
        customAudioThread
            .quitSafely()
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

    private fun startPreAlarmBeep():
        Boolean =
        runCatching {
            val tone =
                ToneGenerator(
                    AudioManager.STREAM_ALARM,
                    PRE_ALARM_TONE_VOLUME
                )

            preAlarmTone =
                tone

            tone.startTone(
                ToneGenerator.TONE_PROP_BEEP2,
                PRE_ALARM_BEEP_MS
            )
        }.getOrDefault(
            false
        )

    private fun startPreAlarmVibration():
        Boolean {
        val vibrator =
            getVibrator()
                ?: return false

        if (
            !vibrator.hasVibrator()
        ) {
            return false
        }

        val effect =
            if (
                vibrator
                    .hasAmplitudeControl()
            ) {
                VibrationEffect
                    .createWaveform(
                        longArrayOf(
                            0,
                            280,
                            180,
                            280,
                            180
                        ),
                        intArrayOf(
                            0,
                            130,
                            0,
                            130,
                            0
                        ),
                        0
                    )
            } else {
                VibrationEffect
                    .createWaveform(
                        longArrayOf(
                            0,
                            280,
                            180,
                            280,
                            180
                        ),
                        0
                    )
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.Builder()
                    .setUsage(
                        VibrationAttributes
                            .USAGE_NOTIFICATION
                    )
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(
                        AudioAttributes
                            .USAGE_NOTIFICATION
                    )
                    .build()
            )
        }

        return true
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
        const val PRE_ALARM_TONE_VOLUME = 55
        const val PRE_ALARM_BEEP_MS = 700
        const val CUSTOM_PREPARE_TIMEOUT_MS =
            1_500L
    }
}
