package com.riccardopinato.phoneguard.alarm

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

/**
 * Applies a user-selected alarm-stream target for the whole protection session.
 * The original raw stream level can be persisted by the session owner and
 * re-adopted after process recovery before restoring it on disarm.
 *
 * OEM / DND policy can still limit effective audibility.
 */
class AlarmVolumeController(context: Context) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE)
            as AudioManager

    private var previousAlarmVolume: Int? = null

    fun applyTarget(percent: Int): Int? {
        val safePercent = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return previousAlarmVolume

        if (previousAlarmVolume == null) {
            previousAlarmVolume =
                audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        }

        val target =
            (max * (safePercent / 100f))
                .roundToInt()
                .coerceIn(0, max)

        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                target,
                0
            )
        }

        return previousAlarmVolume
    }

    fun adoptPreviousVolume(level: Int?) {
        if (previousAlarmVolume != null || level == null) return

        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return

        previousAlarmVolume =
            level.coerceIn(0, max)
    }

    fun previousVolume(): Int? =
        previousAlarmVolume

    fun currentPercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return 0

        val current =
            audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        return ((current.toFloat() / max.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    fun restore() {
        val previous = previousAlarmVolume ?: return

        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                previous,
                0
            )
        }

        previousAlarmVolume = null
    }
}
