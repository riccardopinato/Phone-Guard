package com.riccardopinato.phoneguard.data

import android.content.Context
import android.os.SystemClock
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.service.AlarmPhase
import kotlin.math.abs

data class PersistedProtectionSession(
    val phase: AlarmPhase,
    val mode: ProtectionMode,
    val pocketGuardMode: GuardMode?,
    val updatedAtWallClock: Long,
    val journeyEndsAt: Long?,
    val tableCalibration: MotionCalibration?,
    val pocketCalibration: CalibrationProfile?,
    val journeyEndsAtElapsedRealtime: Long? = null
)

internal enum class RecoveryAction {
    STOP,
    RECALIBRATE,
    RESTORE_TABLE,
    RESTORE_POCKET,
    RESTORE_CHARGING,
    RESTORE_ALARM
}

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

    fun recoveryAction(
        session: PersistedProtectionSession,
        now: Long,
        nowElapsedRealtime: Long? = null
    ): RecoveryAction {
        if (
            session.phase == AlarmPhase.TRIGGERED ||
            session.phase == AlarmPhase.OWNER_CHECK
        ) {
            return RecoveryAction.RESTORE_ALARM
        }

        val journeyExpired =
            if (
                nowElapsedRealtime != null &&
                session.journeyEndsAtElapsedRealtime != null
            ) {
                session.journeyEndsAtElapsedRealtime <=
                    nowElapsedRealtime
            } else {
                session.journeyEndsAt != null &&
                    session.journeyEndsAt <= now
            }

        if (
            session.mode == ProtectionMode.JOURNEY &&
            journeyExpired
        ) {
            return RecoveryAction.STOP
        }

        return when (session.phase) {
            AlarmPhase.ARMED ->
                when (session.mode) {
                    ProtectionMode.TABLE ->
                        if (session.tableCalibration != null) {
                            RecoveryAction.RESTORE_TABLE
                        } else {
                            RecoveryAction.RECALIBRATE
                        }

                    ProtectionMode.POCKET,
                    ProtectionMode.BAG,
                    ProtectionMode.JOURNEY ->
                        if (session.pocketCalibration != null) {
                            RecoveryAction.RESTORE_POCKET
                        } else {
                            RecoveryAction.RECALIBRATE
                        }

                    ProtectionMode.CHARGING ->
                        RecoveryAction.RESTORE_CHARGING
                }

            AlarmPhase.ARMING,
            AlarmPhase.CALIBRATING ->
                RecoveryAction.RECALIBRATE

            AlarmPhase.TRIGGERED,
            AlarmPhase.OWNER_CHECK ->
                RecoveryAction.RESTORE_ALARM

            AlarmPhase.DISARMED,
            AlarmPhase.ERROR ->
                RecoveryAction.STOP
        }
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

    fun saveState(
        mode: ProtectionMode,
        phase: AlarmPhase,
        pocketGuardMode: GuardMode? = null,
        journeyEndsAt: Long? = null,
        journeyEndsAtElapsedRealtime: Long? = null
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
                .putString(KEY_MODE, mode.name)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .putLong(KEY_BOOT_MARKER, currentBootMarker())

        if (pocketGuardMode == null) {
            editor.remove(KEY_POCKET_GUARD_MODE)
        } else {
            editor.putString(
                KEY_POCKET_GUARD_MODE,
                pocketGuardMode.name
            )
        }

        if (journeyEndsAt == null) {
            editor.remove(KEY_JOURNEY_ENDS_AT)
        } else {
            editor.putLong(KEY_JOURNEY_ENDS_AT, journeyEndsAt)
        }

        if (journeyEndsAtElapsedRealtime == null) {
            editor.remove(
                KEY_JOURNEY_ENDS_AT_ELAPSED
            )
        } else {
            editor.putLong(
                KEY_JOURNEY_ENDS_AT_ELAPSED,
                journeyEndsAtElapsedRealtime
            )
        }

        if (
            phase == AlarmPhase.ARMING ||
            phase == AlarmPhase.CALIBRATING
        ) {
            removeTableCalibration(editor)
            removePocketCalibration(editor)
        }

        editor.commit()
    }

    fun saveTableCalibration(
        calibration: MotionCalibration
    ) {
        prefs.edit()
            .putFloat(KEY_BASELINE_X, calibration.baselineX)
            .putFloat(KEY_BASELINE_Y, calibration.baselineY)
            .putFloat(KEY_BASELINE_Z, calibration.baselineZ)
            .putFloat(KEY_NOISE_FLOOR, calibration.noiseFloor)
            .putBoolean(KEY_HAS_TABLE_CALIBRATION, true)
            .putLong(KEY_BOOT_MARKER, currentBootMarker())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    fun savePocketCalibration(
        calibration: CalibrationProfile
    ) {
        prefs.edit()
            .putString(KEY_POCKET_PROFILE_MODE, calibration.mode.name)
            .putLong(KEY_POCKET_PROFILE_CREATED_AT, calibration.createdAt)
            .putFloat(KEY_POCKET_ENTER_LUX, calibration.enterLux)
            .putFloat(KEY_POCKET_EXIT_LUX, calibration.exitLux)
            .putFloat(KEY_POCKET_MOTION_NOISE, calibration.motionNoise)
            .putFloat(KEY_POCKET_ROTATION_NOISE, calibration.rotationNoise)
            .putInt(KEY_POCKET_SAMPLE_COUNT, calibration.sampleCount)
            .putBoolean(KEY_POCKET_HAS_LIGHT, calibration.hasLightData)
            .putBoolean(KEY_POCKET_HAS_GYRO, calibration.hasGyroscopeData)
            .putBoolean(KEY_HAS_POCKET_CALIBRATION, true)
            .putLong(KEY_BOOT_MARKER, currentBootMarker())
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .commit()
    }

    fun saveJourneyEnd(
        endAt: Long?,
        endAtElapsedRealtime: Long?
    ) {
        val editor =
            prefs.edit()
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())

        if (endAt == null) {
            editor.remove(KEY_JOURNEY_ENDS_AT)
        } else {
            editor.putLong(KEY_JOURNEY_ENDS_AT, endAt)
        }

        if (endAtElapsedRealtime == null) {
            editor.remove(
                KEY_JOURNEY_ENDS_AT_ELAPSED
            )
        } else {
            editor.putLong(
                KEY_JOURNEY_ENDS_AT_ELAPSED,
                endAtElapsedRealtime
            )
        }

        editor.commit()
    }

    fun read(): PersistedProtectionSession? {
        val phaseName =
            prefs.getString(KEY_PHASE, null)
                ?: return null

        val phase =
            runCatching {
                AlarmPhase.valueOf(phaseName)
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
            prefs.getLong(KEY_BOOT_MARKER, 0L)

        if (
            !SessionRecoveryPolicy.sameBoot(
                savedBootMarker,
                currentBootMarker()
            )
        ) {
            clear()
            return null
        }

        val mode =
            runCatching {
                ProtectionMode.valueOf(
                    prefs.getString(
                        KEY_MODE,
                        ProtectionMode.TABLE.name
                    ) ?: ProtectionMode.TABLE.name
                )
            }.getOrDefault(ProtectionMode.TABLE)

        val pocketGuardMode =
            prefs.getString(
                KEY_POCKET_GUARD_MODE,
                null
            )?.let {
                runCatching {
                    GuardMode.valueOf(it)
                }.getOrNull()
            }

        val tableCalibration =
            if (
                prefs.getBoolean(
                    KEY_HAS_TABLE_CALIBRATION,
                    false
                )
            ) {
                MotionCalibration(
                    baselineX = prefs.getFloat(KEY_BASELINE_X, 0f),
                    baselineY = prefs.getFloat(KEY_BASELINE_Y, 0f),
                    baselineZ = prefs.getFloat(KEY_BASELINE_Z, 0f),
                    noiseFloor = prefs.getFloat(KEY_NOISE_FLOOR, 0f)
                )
            } else {
                null
            }

        val pocketCalibration =
            if (
                prefs.getBoolean(
                    KEY_HAS_POCKET_CALIBRATION,
                    false
                )
            ) {
                val profileMode =
                    runCatching {
                        GuardMode.valueOf(
                            prefs.getString(
                                KEY_POCKET_PROFILE_MODE,
                                GuardMode.POCKET.name
                            ) ?: GuardMode.POCKET.name
                        )
                    }.getOrDefault(
                        pocketGuardMode ?: GuardMode.POCKET
                    )

                CalibrationProfile(
                    mode = profileMode,
                    createdAt =
                        prefs.getLong(
                            KEY_POCKET_PROFILE_CREATED_AT,
                            0L
                        ),
                    enterLux =
                        prefs.getFloat(
                            KEY_POCKET_ENTER_LUX,
                            3f
                        ),
                    exitLux =
                        prefs.getFloat(
                            KEY_POCKET_EXIT_LUX,
                            12f
                        ),
                    motionNoise =
                        prefs.getFloat(
                            KEY_POCKET_MOTION_NOISE,
                            0.03f
                        ),
                    rotationNoise =
                        prefs.getFloat(
                            KEY_POCKET_ROTATION_NOISE,
                            0.05f
                        ),
                    sampleCount =
                        prefs.getInt(
                            KEY_POCKET_SAMPLE_COUNT,
                            0
                        ),
                    hasLightData =
                        prefs.getBoolean(
                            KEY_POCKET_HAS_LIGHT,
                            false
                        ),
                    hasGyroscopeData =
                        prefs.getBoolean(
                            KEY_POCKET_HAS_GYRO,
                            false
                        )
                )
            } else {
                null
            }

        return PersistedProtectionSession(
            phase = phase,
            mode = mode,
            pocketGuardMode = pocketGuardMode,
            updatedAtWallClock =
                prefs.getLong(KEY_UPDATED_AT, 0L),
            journeyEndsAt =
                if (prefs.contains(KEY_JOURNEY_ENDS_AT)) {
                    prefs.getLong(KEY_JOURNEY_ENDS_AT, 0L)
                        .takeIf { it > 0L }
                } else {
                    null
                },
            tableCalibration = tableCalibration,
            pocketCalibration = pocketCalibration,
            journeyEndsAtElapsedRealtime =
                if (
                    prefs.contains(
                        KEY_JOURNEY_ENDS_AT_ELAPSED
                    )
                ) {
                    prefs.getLong(
                        KEY_JOURNEY_ENDS_AT_ELAPSED,
                        0L
                    ).takeIf { it > 0L }
                } else {
                    null
                }
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

    private fun removeTableCalibration(
        editor: android.content.SharedPreferences.Editor
    ) {
        editor
            .remove(KEY_BASELINE_X)
            .remove(KEY_BASELINE_Y)
            .remove(KEY_BASELINE_Z)
            .remove(KEY_NOISE_FLOOR)
            .putBoolean(KEY_HAS_TABLE_CALIBRATION, false)
    }

    private fun removePocketCalibration(
        editor: android.content.SharedPreferences.Editor
    ) {
        editor
            .remove(KEY_POCKET_PROFILE_MODE)
            .remove(KEY_POCKET_PROFILE_CREATED_AT)
            .remove(KEY_POCKET_ENTER_LUX)
            .remove(KEY_POCKET_EXIT_LUX)
            .remove(KEY_POCKET_MOTION_NOISE)
            .remove(KEY_POCKET_ROTATION_NOISE)
            .remove(KEY_POCKET_SAMPLE_COUNT)
            .remove(KEY_POCKET_HAS_LIGHT)
            .remove(KEY_POCKET_HAS_GYRO)
            .putBoolean(KEY_HAS_POCKET_CALIBRATION, false)
    }

    private companion object {
        const val PREFS_NAME = "protection_runtime_session"
        const val KEY_PHASE = "phase"
        const val KEY_MODE = "mode"
        const val KEY_POCKET_GUARD_MODE = "pocket_guard_mode"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_BOOT_MARKER = "boot_marker"
        const val KEY_JOURNEY_ENDS_AT = "journey_ends_at"
        const val KEY_JOURNEY_ENDS_AT_ELAPSED =
            "journey_ends_at_elapsed"

        const val KEY_HAS_TABLE_CALIBRATION = "has_table_calibration"
        const val KEY_BASELINE_X = "baseline_x"
        const val KEY_BASELINE_Y = "baseline_y"
        const val KEY_BASELINE_Z = "baseline_z"
        const val KEY_NOISE_FLOOR = "noise_floor"

        const val KEY_HAS_POCKET_CALIBRATION = "has_pocket_calibration"
        const val KEY_POCKET_PROFILE_MODE = "pocket_profile_mode"
        const val KEY_POCKET_PROFILE_CREATED_AT = "pocket_profile_created_at"
        const val KEY_POCKET_ENTER_LUX = "pocket_enter_lux"
        const val KEY_POCKET_EXIT_LUX = "pocket_exit_lux"
        const val KEY_POCKET_MOTION_NOISE = "pocket_motion_noise"
        const val KEY_POCKET_ROTATION_NOISE = "pocket_rotation_noise"
        const val KEY_POCKET_SAMPLE_COUNT = "pocket_sample_count"
        const val KEY_POCKET_HAS_LIGHT = "pocket_has_light"
        const val KEY_POCKET_HAS_GYRO = "pocket_has_gyro"
    }
}
