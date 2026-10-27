package com.riccardopinato.phoneguard.alarm

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

internal data class AlarmVolumeLease(
    val originalLevel: Int,
    val targetLevel: Int,
    val targetPercent: Int,
    val acquiredAt: Long
)

internal object AlarmVolumeLeasePolicy {
    fun shouldRestore(
        currentLevel: Int,
        lease: AlarmVolumeLease
    ): Boolean =
        currentLevel ==
            lease.targetLevel
}

class AlarmVolumeLeaseManager(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val audioManager =
        appContext.getSystemService(
            Context.AUDIO_SERVICE
        ) as AudioManager

    private val prefs =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    @Synchronized
    fun acquire(
        targetPercent: Int
    ): Boolean {
        val max =
            audioManager.getStreamMaxVolume(
                AudioManager.STREAM_ALARM
            )

        if (max <= 0) {
            return false
        }

        val safePercent =
            targetPercent.coerceIn(
                0,
                100
            )

        val current =
            audioManager.getStreamVolume(
                AudioManager.STREAM_ALARM
            )

        val target =
            (
                max *
                    (
                        safePercent /
                            100f
                        )
                )
                .roundToInt()
                .coerceIn(
                    0,
                    max
                )

        val existing =
            readLease()

        val lease =
            AlarmVolumeLease(
                originalLevel =
                    existing
                        ?.originalLevel
                        ?: current,
                targetLevel = target,
                targetPercent =
                    safePercent,
                acquiredAt =
                    existing
                        ?.acquiredAt
                        ?: System
                            .currentTimeMillis()
            )

        if (!writeLease(lease)) {
            return false
        }

        val applied =
            runCatching {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    target,
                    0
                )
                true
            }.getOrDefault(false)

        if (
            !applied &&
            existing == null
        ) {
            clearLease()
        }

        return applied
    }

    @Synchronized
    fun reapplyTarget(): Boolean {
        val lease =
            readLease()
                ?: return false

        return runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                lease.targetLevel,
                0
            )
            true
        }.getOrDefault(false)
    }

    @Synchronized
    fun restoreIfOwned(): Boolean {
        val lease =
            readLease()
                ?: return true

        val current =
            audioManager.getStreamVolume(
                AudioManager.STREAM_ALARM
            )

        if (
            !AlarmVolumeLeasePolicy
                .shouldRestore(
                    current,
                    lease
                )
        ) {
            clearLease()
            return true
        }

        val restored =
            runCatching {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    lease.originalLevel,
                    0
                )
                true
            }.getOrDefault(false)

        if (restored) {
            clearLease()
        }

        return restored
    }

    fun hasLease(): Boolean =
        prefs.getBoolean(
            KEY_ACTIVE,
            false
        )

    fun currentPercent(): Int {
        val max =
            audioManager.getStreamMaxVolume(
                AudioManager.STREAM_ALARM
            )

        if (max <= 0) {
            return 0
        }

        val current =
            audioManager.getStreamVolume(
                AudioManager.STREAM_ALARM
            )

        return (
            current.toFloat() /
                max.toFloat() *
                100f
            )
            .roundToInt()
            .coerceIn(
                0,
                100
            )
    }

    internal fun readLease():
        AlarmVolumeLease? {
        if (
            !prefs.getBoolean(
                KEY_ACTIVE,
                false
            )
        ) {
            return null
        }

        val original =
            prefs.getInt(
                KEY_ORIGINAL,
                -1
            )

        val target =
            prefs.getInt(
                KEY_TARGET,
                -1
            )

        if (
            original < 0 ||
            target < 0
        ) {
            clearLease()
            return null
        }

        return AlarmVolumeLease(
            originalLevel = original,
            targetLevel = target,
            targetPercent =
                prefs.getInt(
                    KEY_TARGET_PERCENT,
                    100
                ).coerceIn(0, 100),
            acquiredAt =
                prefs.getLong(
                    KEY_ACQUIRED_AT,
                    0L
                )
        )
    }

    private fun writeLease(
        lease: AlarmVolumeLease
    ): Boolean =
        prefs.edit()
            .putBoolean(
                KEY_ACTIVE,
                true
            )
            .putInt(
                KEY_ORIGINAL,
                lease.originalLevel
            )
            .putInt(
                KEY_TARGET,
                lease.targetLevel
            )
            .putInt(
                KEY_TARGET_PERCENT,
                lease.targetPercent
            )
            .putLong(
                KEY_ACQUIRED_AT,
                lease.acquiredAt
            )
            .commit()

    private fun clearLease() {
        prefs.edit()
            .clear()
            .commit()
    }

    private companion object {
        const val PREFS_NAME =
            "phone_guard_volume_lease"

        const val KEY_ACTIVE =
            "active"

        const val KEY_ORIGINAL =
            "original_level"

        const val KEY_TARGET =
            "target_level"

        const val KEY_TARGET_PERCENT =
            "target_percent"

        const val KEY_ACQUIRED_AT =
            "acquired_at"
    }
}
