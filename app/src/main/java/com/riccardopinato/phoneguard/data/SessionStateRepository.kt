package com.riccardopinato.phoneguard.data

import android.content.Context
import android.os.SystemClock
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.service.AlarmPhase
import kotlin.math.abs

data class PersistedAlarmSession(
    val phase: AlarmPhase,
    val updatedAtWallClock: Long,
    val calibration: MotionCalibration?
)

internal object SessionRecoveryPolicy {
    private const val BOOT_MARKER_TOLERANCE_MS = 120_000L

    fun sameBoot(
        savedBootMarker: Long,
        currentBootMarker: Long
    ): Boolean {
        if (savedBootMarker <= 0L || currentBootMarker <= 0L) {
            return false
        }

        return abs(savedBootMarker - currentBootMarker) <=
            BOOT_MARKER_TOLERANCE_MS
    }
}

class SessionStateRepository(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun savePhase(
        phase: AlarmPhase
    ) {
        if (
            phase == AlarmPhase.DISARMED ||
            phase == AlarmPhase.ERROR
        ) {
            clear()
            return
        }

        val editor =
            prefs.edit()
                .putString(KEY_PHASE, phase.name)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .putLong(KEY_BOOT_MARKER, currentBootMarker())

        if (
            phase == AlarmPhase.ARMING ||
            phase == AlarmPhase.CALIBRATING
        ) {
            removeCalibration(editor)
        }

        editor.commit()
    }

    fun saveCalibration(
        calibration: MotionCalibration
    ) {
        prefs.edit()
            .putFloat(KEY_BASELINE_X, calibration.baselineX)
            .putFloat(KEY_BASELINE_Y, calibration.baselineY)
            .putFloat(KEY_BASELINE_Z, calibration.baselineZ)
            .putFloat(KEY_NOISE_FLOOR, calibration.noiseFloor)
            .putBoolean(KEY_HAS_CALIBRATION, true)
            .putLong(KEY_BOOT_MARKER, currentBootMarker())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    fun read():
        PersistedAlarmSession? {

        val phaseName =
            prefs.getString(
                KEY_PHASE,
                null
            ) ?: return null

        val phase =
            runCatching {
                AlarmPhase.valueOf(
                    phaseName
                )
            }.getOrNull()
                ?: run {
                    clear()
                    return null
                }

        if (
            phase == AlarmPhase.DISARMED ||
            phase == AlarmPhase.ERROR
        ) {
            clear()
            return null
        }

        val savedBootMarker =
            prefs.getLong(
                KEY_BOOT_MARKER,
                0L
            )

        if (
            !SessionRecoveryPolicy.sameBoot(
                savedBootMarker,
                currentBootMarker()
            )
        ) {
            clear()
            return null
        }

        val calibration =
            if (
                prefs.getBoolean(
                    KEY_HAS_CALIBRATION,
                    false
                )
            ) {
                MotionCalibration(
                    baselineX =
                        prefs.getFloat(
                            KEY_BASELINE_X,
                            0f
                        ),
                    baselineY =
                        prefs.getFloat(
                            KEY_BASELINE_Y,
                            0f
                        ),
                    baselineZ =
                        prefs.getFloat(
                            KEY_BASELINE_Z,
                            0f
                        ),
                    noiseFloor =
                        prefs.getFloat(
                            KEY_NOISE_FLOOR,
                            0f
                        )
                )
            } else {
                null
            }

        return PersistedAlarmSession(
            phase = phase,
            updatedAtWallClock =
                prefs.getLong(
                    KEY_UPDATED_AT,
                    0L
                ),
            calibration = calibration
        )
    }

    fun clear() {
        prefs.edit()
            .clear()
            .commit()
    }

    private fun currentBootMarker(): Long =
        System.currentTimeMillis() -
            SystemClock.elapsedRealtime()

    private fun removeCalibration(
        editor: android.content.SharedPreferences.Editor
    ) {
        editor
            .remove(KEY_BASELINE_X)
            .remove(KEY_BASELINE_Y)
            .remove(KEY_BASELINE_Z)
            .remove(KEY_NOISE_FLOOR)
            .putBoolean(KEY_HAS_CALIBRATION, false)
    }

    private companion object {
        const val PREFS_NAME =
            "alarm_runtime_session"

        const val KEY_PHASE =
            "phase"

        const val KEY_UPDATED_AT =
            "updated_at"

        const val KEY_BOOT_MARKER =
            "boot_marker"

        const val KEY_HAS_CALIBRATION =
            "has_calibration"

        const val KEY_BASELINE_X =
            "baseline_x"

        const val KEY_BASELINE_Y =
            "baseline_y"

        const val KEY_BASELINE_Z =
            "baseline_z"

        const val KEY_NOISE_FLOOR =
            "noise_floor"
    }
}
