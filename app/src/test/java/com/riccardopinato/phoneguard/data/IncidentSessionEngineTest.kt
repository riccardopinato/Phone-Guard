package com.riccardopinato.phoneguard.data

import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventEvidence
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentSessionEngineTest {

    @Test
    fun groupsLifecycleIntoOneIncident() {
        val events =
            listOf(
                event(
                    id = 4,
                    timestamp = 4_000,
                    type =
                        AlarmEventType.DISARMED
                ),
                event(
                    id = 3,
                    timestamp = 3_000,
                    type =
                        AlarmEventType.TRIGGERED,
                    reason =
                        "accessory disconnected",
                    outputs =
                        "sound=active"
                ),
                event(
                    id = 2,
                    timestamp = 2_000,
                    type =
                        AlarmEventType.ARMED
                ),
                event(
                    id = 1,
                    timestamp = 1_000,
                    type =
                        AlarmEventType.ARMING_STARTED
                )
            )

        val sessions =
            IncidentSessionEngine
                .build(
                    events
                )

        assertEquals(
            1,
            sessions.size
        )

        val session =
            sessions.single()

        assertTrue(
            session.triggered
        )
        assertEquals(
            ProtectionMode.ACCESSORY,
            session.mode
        )
        assertEquals(
            4,
            session.eventCount
        )
        assertEquals(
            4_000,
            session.endedAt
        )
        assertEquals(
            "accessory disconnected",
            session.triggerReason
        )
    }

    @Test
    fun startsNewSessionWhenAnotherArmingBegins() {
        val events =
            listOf(
                event(
                    1,
                    1_000,
                    AlarmEventType.ARMING_STARTED
                ),
                event(
                    2,
                    2_000,
                    AlarmEventType.CANCELED
                ),
                event(
                    3,
                    3_000,
                    AlarmEventType.ARMING_STARTED
                ),
                event(
                    4,
                    4_000,
                    AlarmEventType.ARMED
                )
            )

        val sessions =
            IncidentSessionEngine
                .build(
                    events
                )

        assertEquals(
            2,
            sessions.size
        )
        assertFalse(
            sessions.first()
                .triggered
        )
    }

    @Test
    fun shareReportStatesLocalPrivacyBoundary() {
        val session =
            IncidentSessionEngine
                .build(
                    listOf(
                        event(
                            1,
                            1_000,
                            AlarmEventType.ARMING_STARTED
                        ),
                        event(
                            2,
                            2_000,
                            AlarmEventType.TRIGGERED,
                            reason = "movement"
                        )
                    )
                )
                .single()

        val report =
            IncidentSessionEngine
                .shareReport(
                    session
                )

        assertTrue(
            report.contains(
                "no location or account data"
            )
        )
        assertTrue(
            report.contains(
                "Triggered: yes"
            )
        )
    }

    private fun event(
        id: Long,
        timestamp: Long,
        type: AlarmEventType,
        reason: String? = null,
        outputs: String? = null
    ): AlarmEvent =
        AlarmEvent(
            id = id,
            timestamp = timestamp,
            type = type,
            detail =
                reason.orEmpty(),
            evidence =
                AlarmEventEvidence(
                    mode =
                        ProtectionMode.ACCESSORY,
                    source = "APP",
                    reason = reason,
                    outputSummary = outputs,
                    graceSeconds = 5
                )
        )
}
