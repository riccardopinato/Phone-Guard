package com.riccardopinato.phoneguard.data

import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.service.AlarmPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRecoveryPolicyTest {

    @Test
    fun sameBoot_acceptsSmallClockDrift() {
        assertTrue(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 1_000_000L,
                currentBootMarker = 1_060_000L
            )
        )
    }

    @Test
    fun sameBoot_rejectsDifferentBoot() {
        assertFalse(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 1_000_000L,
                currentBootMarker = 1_300_001L
            )
        )
    }

    @Test
    fun sameBoot_rejectsMissingMarker() {
        assertFalse(
            SessionRecoveryPolicy.sameBoot(
                savedBootMarker = 0L,
                currentBootMarker = 1_000_000L
            )
        )
    }

    @Test
    fun armedTableWithCalibrationRestoresTable() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.TABLE,
                pocketGuardMode = null,
                updatedAtWallClock = 1L,
                journeyEndsAt = null,
                tableCalibration =
                    MotionCalibration(
                        baselineX = 0f,
                        baselineY = 0f,
                        baselineZ = 9.8f,
                        noiseFloor = 0.03f
                    ),
                pocketCalibration = null
            )

        assertEquals(
            RecoveryAction.RESTORE_TABLE,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10L
            )
        )
    }

    @Test
    fun armedPocketWithProfileRestoresPocket() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.POCKET,
                pocketGuardMode = GuardMode.POCKET,
                updatedAtWallClock = 1L,
                journeyEndsAt = null,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.POCKET
                    )
            )

        assertEquals(
            RecoveryAction.RESTORE_POCKET,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10L
            )
        )
    }

    @Test
    fun ownerCheckRecoveryFailsClosedToAlarm() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.OWNER_CHECK,
                mode = ProtectionMode.POCKET,
                pocketGuardMode = GuardMode.POCKET,
                updatedAtWallClock = 1L,
                journeyEndsAt = null,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.POCKET
                    )
            )

        assertEquals(
            RecoveryAction.RESTORE_ALARM,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10L
            )
        )
    }

    @Test
    fun expiredJourneyStopsInsteadOfRearming() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.JOURNEY,
                pocketGuardMode = GuardMode.BAG,
                updatedAtWallClock = 1L,
                journeyEndsAt = 9_000L,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.BAG
                    )
            )

        assertEquals(
            RecoveryAction.STOP,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10_000L
            )
        )
    }


    @Test
    fun expiredJourneyOwnerCheckStillRestoresAlarm() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.OWNER_CHECK,
                mode = ProtectionMode.JOURNEY,
                pocketGuardMode = GuardMode.POCKET,
                updatedAtWallClock = 1L,
                journeyEndsAt = 9_000L,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.POCKET
                    )
            )

        assertEquals(
            RecoveryAction.RESTORE_ALARM,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10_000L
            )
        )
    }

    @Test
    fun expiredJourneyTriggeredStillRestoresAlarm() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.TRIGGERED,
                mode = ProtectionMode.JOURNEY,
                pocketGuardMode = GuardMode.BAG,
                updatedAtWallClock = 1L,
                journeyEndsAt = 9_000L,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.BAG
                    )
            )

        assertEquals(
            RecoveryAction.RESTORE_ALARM,
            SessionRecoveryPolicy.recoveryAction(
                session,
                now = 10_000L
            )
        )
    }

    @Test
    fun journeyRecoveryUsesMonotonicDeadlineWhenAvailable() {
        val session =
            PersistedProtectionSession(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.JOURNEY,
                pocketGuardMode = GuardMode.POCKET,
                updatedAtWallClock = 1L,
                journeyEndsAt = 1_000_000L,
                tableCalibration = null,
                pocketCalibration =
                    pocketProfile(
                        GuardMode.POCKET
                    ),
                journeyEndsAtElapsedRealtime =
                    9_000L
            )

        assertEquals(
            RecoveryAction.STOP,
            SessionRecoveryPolicy
                .recoveryAction(
                    session = session,
                    now = 100L,
                    nowElapsedRealtime =
                        10_000L
                )
        )
    }

    private fun pocketProfile(
        mode: GuardMode
    ): CalibrationProfile =
        CalibrationProfile(
            mode = mode,
            createdAt = 1L,
            enterLux = 5f,
            exitLux = 12f,
            motionNoise = 0.03f,
            rotationNoise = 0.05f,
            sampleCount = 30,
            hasLightData = true,
            hasGyroscopeData = true
        )
}
