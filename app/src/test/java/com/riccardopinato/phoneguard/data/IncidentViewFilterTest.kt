package com.riccardopinato.phoneguard.data

import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IncidentViewFilterTest {
    private fun event(id: Long, type: AlarmEventType) =
        AlarmEvent(id = id, timestamp = id, type = type, detail = "local")

    @Test fun alarmsOnlyRetainsTriggerAndPreAlarmWithoutMutatingSource() {
        val entries = listOf(
            event(1, AlarmEventType.ARMING_STARTED),
            event(2, AlarmEventType.PRE_ALARM_STARTED),
            event(3, AlarmEventType.TRIGGERED),
            event(4, AlarmEventType.DISARMED)
        )
        val selected = IncidentViewFilter.events(entries, true)
        assertEquals(listOf(2L, 3L), selected.map { it.id })
        assertEquals(4, entries.size)
        assertEquals(1, IncidentViewFilter.triggerCount(entries))
        assertSame(entries, IncidentViewFilter.events(entries, false))
    }

    @Test fun filterUsesDerivedSessionsWithoutChangingIncidentGrouping() {
        val entries = listOf(
            event(1, AlarmEventType.ARMING_STARTED),
            event(2, AlarmEventType.DISARMED),
            event(3, AlarmEventType.ARMING_STARTED),
            event(4, AlarmEventType.TRIGGERED),
            event(5, AlarmEventType.DISARMED)
        )
        val sessions = IncidentSessionEngine.build(entries)
        val alarms = IncidentViewFilter.sessions(sessions, true)
        assertEquals(2, sessions.size)
        assertEquals(1, alarms.size)
        assertEquals(true, alarms.single().triggered)
        assertSame(sessions, IncidentViewFilter.sessions(sessions, false))
    }
}
