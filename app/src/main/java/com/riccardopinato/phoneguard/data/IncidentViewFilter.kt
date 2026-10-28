package com.riccardopinato.phoneguard.data

import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType

/** Read-only presentation filter; never mutates or deletes local evidence. */
object IncidentViewFilter {
    fun sessions(
        source: List<IncidentSession>,
        alarmsOnly: Boolean
    ): List<IncidentSession> =
        if (alarmsOnly) source.filter { it.triggered } else source

    fun events(
        source: List<AlarmEvent>,
        alarmsOnly: Boolean
    ): List<AlarmEvent> =
        if (alarmsOnly) {
            source.filter {
                it.type == AlarmEventType.TRIGGERED ||
                    it.type == AlarmEventType.PRE_ALARM_STARTED
            }
        } else source

    fun triggerCount(source: List<AlarmEvent>): Int =
        source.count { it.type == AlarmEventType.TRIGGERED }
}
