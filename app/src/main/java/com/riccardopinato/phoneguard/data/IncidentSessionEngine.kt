package com.riccardopinato.phoneguard.data

import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.ProtectionMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidentSession(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val mode: ProtectionMode?,
    val activationSource: String?,
    val triggered: Boolean,
    val triggerReason: String?,
    val outputSummary: String?,
    val graceSeconds: Int?,
    val eventCount: Int,
    val events: List<AlarmEvent>
)

object IncidentSessionEngine {
    fun build(
        events: List<AlarmEvent>
    ): List<IncidentSession> {
        if (
            events.isEmpty()
        ) {
            return emptyList()
        }

        val chronological =
            events.sortedWith(
                compareBy<AlarmEvent> {
                    it.timestamp
                }.thenBy {
                    it.id
                }
            )

        val sessions =
            mutableListOf<
                IncidentSession
                >()

        var current =
            mutableListOf<
                AlarmEvent
                >()

        fun closeCurrent() {
            if (
                current.isEmpty()
            ) {
                return
            }

            sessions +=
                createSession(
                    current
                )

            current =
                mutableListOf()
        }

        chronological
            .forEach {
                event ->

                if (
                    event.type ==
                    AlarmEventType
                        .ARMING_STARTED &&
                    current.isNotEmpty()
                ) {
                    closeCurrent()
                }

                if (
                    current.isEmpty() &&
                    event.type !in
                        SESSION_START_TYPES
                ) {
                    return@forEach
                }

                current +=
                    event

                if (
                    event.type in
                    SESSION_END_TYPES
                ) {
                    closeCurrent()
                }
            }

        closeCurrent()

        return sessions
            .sortedByDescending {
                it.startedAt
            }
    }

    fun shareReport(
        session: IncidentSession
    ): String {
        val formatter =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            )

        return buildString {
            appendLine(
                "Phone Guard incident report"
            )
            appendLine(
                "Session: #${session.id}"
            )
            appendLine(
                "Started: " +
                    formatter.format(
                        Date(
                            session.startedAt
                        )
                    )
            )
            session.endedAt
                ?.let {
                    appendLine(
                        "Ended: " +
                            formatter.format(
                                Date(it)
                            )
                    )
                }
            appendLine(
                "Mode: " +
                    (
                        session.mode
                            ?.label
                            ?: "unknown"
                        )
            )
            appendLine(
                "Activation source: " +
                    (
                        session.activationSource
                            ?: "unknown"
                        )
            )
            appendLine(
                "Triggered: " +
                    if (
                        session.triggered
                    ) {
                        "yes"
                    } else {
                        "no"
                    }
            )
            session.triggerReason
                ?.let {
                    appendLine(
                        "Reason: $it"
                    )
                }
            session.graceSeconds
                ?.let {
                    appendLine(
                        "Owner grace: ${it}s"
                    )
                }
            session.outputSummary
                ?.let {
                    appendLine(
                        "Outputs: $it"
                    )
                }
            appendLine(
                "Events: ${session.eventCount}"
            )
            appendLine()
            appendLine(
                "This report was generated locally by Phone Guard. It contains no location or account data."
            )
        }.trim()
    }

    private fun createSession(
        events: List<AlarmEvent>
    ): IncidentSession {
        val trigger =
            events
                .lastOrNull {
                    it.type ==
                        AlarmEventType
                            .TRIGGERED
                }

        val terminal =
            events
                .lastOrNull {
                    it.type in
                        SESSION_END_TYPES
                }

        val firstEvidence =
            events
                .firstNotNullOfOrNull {
                    it.evidence
                }

        return IncidentSession(
            id =
                events
                    .first()
                    .id,
            startedAt =
                events
                    .first()
                    .timestamp,
            endedAt =
                terminal
                    ?.timestamp,
            mode =
                firstEvidence
                    ?.mode
                    ?: trigger
                        ?.evidence
                        ?.mode,
            activationSource =
                events
                    .firstNotNullOfOrNull {
                        it.evidence
                            ?.source
                    },
            triggered =
                trigger != null,
            triggerReason =
                trigger
                    ?.evidence
                    ?.reason
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: trigger
                        ?.detail
                        ?.takeIf {
                            it.isNotBlank()
                        },
            outputSummary =
                trigger
                    ?.evidence
                    ?.outputSummary,
            graceSeconds =
                events
                    .firstNotNullOfOrNull {
                        it.evidence
                            ?.graceSeconds
                    },
            eventCount =
                events.size,
            events =
                events.toList()
        )
    }

    private val SESSION_START_TYPES =
        setOf(
            AlarmEventType.ARMING_STARTED,
            AlarmEventType.ARMED,
            AlarmEventType.PRE_ALARM_STARTED,
            AlarmEventType.TRIGGERED
        )

    private val SESSION_END_TYPES =
        setOf(
            AlarmEventType.DISARMED,
            AlarmEventType.CANCELED,
            AlarmEventType.JOURNEY_ENDED,
            AlarmEventType.ERROR
        )
}
