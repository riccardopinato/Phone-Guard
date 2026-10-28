package com.riccardopinato.phoneguard.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.EventRepository
import com.riccardopinato.phoneguard.data.IncidentSession
import com.riccardopinato.phoneguard.data.IncidentSessionEngine
import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.components.UtilityChoice
import com.riccardopinato.phoneguard.ui.components.UtilitySectionHeader
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniDanger
import com.riccardopinato.phoneguard.ui.theme.OmniWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsScreen(
    eventRepository: EventRepository
) {
    val context =
        LocalContext.current

    val resources =
        LocalResources.current

    val events by
        eventRepository.events
            .collectAsStateWithLifecycle()

    val sessions = remember(events) { IncidentSessionEngine.build(events) }
    var onlyAlarms by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val visibleSessions = if (onlyAlarms) sessions.filter { it.triggered } else sessions
    val visibleEvents = if (onlyAlarms) events.filter {
        it.type == AlarmEventType.TRIGGERED ||
            it.type == AlarmEventType.PRE_ALARM_STARTED
    } else events
    val alarmCount = events.count { it.type == AlarmEventType.TRIGGERED }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 2.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        1.dp
                    )
            ) {
                Text(
                    text =
                        stringResource(
                            R.string
                                .events_kicker
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        OmniAccent,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        stringResource(
                            R.string
                                .events_title
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .headlineLarge
                )
            }

            if (
                events.isNotEmpty()
            ) {
                TextButton(
                    onClick =
                        { confirmClear = true }
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .events_clear
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                        color =
                            OmniDanger
                    )
                }
            }
        }

        if (events.isNotEmpty()) {
            UtilityPanel {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.events_alarm_summary, alarmCount, events.size
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UtilityChoice(
                            label = stringResource(R.string.events_filter_all),
                            selected = !onlyAlarms,
                            enabled = true,
                            onClick = { onlyAlarms = false },
                            modifier = Modifier.weight(1f)
                        )
                        UtilityChoice(
                            label = stringResource(R.string.events_filter_alarms),
                            selected = onlyAlarms,
                            enabled = true,
                            onClick = { onlyAlarms = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (
            events.isEmpty()
        ) {
            UtilityPanel {
                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .events_empty_title
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .events_empty_text
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        bottom = 22.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        9.dp
                    )
            ) {
                if (
                    visibleSessions.isNotEmpty()
                ) {
                    item {
                        UtilitySectionHeader(
                            title =
                                stringResource(
                                    R.string
                                        .incident_sessions_title
                                ),
                            trailing =
                                visibleSessions
                                    .size
                                    .toString()
                        )
                    }

                    items(
                        items =
                            visibleSessions,
                        key = {
                            "session_" +
                                it.id
                        }
                    ) {
                        session ->
                        IncidentPanel(
                            session =
                                session,
                            onShare = {
                                val report =
                                    IncidentSessionEngine
                                        .shareReport(
                                            session
                                        )

                                context.startActivity(
                                    Intent
                                        .createChooser(
                                            Intent(
                                                Intent.ACTION_SEND
                                            )
                                                .setType(
                                                    "text/plain"
                                                )
                                                .putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    report
                                                ),
                                            resources.getString(
                                                R.string
                                                    .incident_share_title
                                            )
                                        )
                                )
                            }
                        )
                    }
                }

                if (onlyAlarms && visibleSessions.isEmpty() &&
                    visibleEvents.isEmpty()
                ) {
                    item {
                        UtilityPanel {
                            Text(
                                text = stringResource(R.string.events_filter_empty),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    UtilitySectionHeader(
                        title =
                            stringResource(
                                R.string
                                    .event_evidence_title
                            ),
                        trailing =
                            visibleEvents
                                .size
                                .toString()
                    )
                }

                items(
                    items = visibleEvents,
                    key = {
                        "event_" +
                            it.id
                    }
                ) {
                    event ->
                    EventPanel(
                        event
                    )
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.events_clear_confirm_title)) },
            text = { Text(stringResource(R.string.events_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    eventRepository.clear()
                    confirmClear = false
                    onlyAlarms = false
                }) { Text(stringResource(R.string.events_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun IncidentPanel(
    session: IncidentSession,
    onShare: () -> Unit
) {
    UtilityPanel {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        15.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text =
                        session.mode
                            ?.label
                            ?: stringResource(
                                R.string
                                    .incident_unknown_mode
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                TextButton(
                    onClick =
                        onShare
                ) {
                    Text(
                        stringResource(
                            R.string
                                .incident_share
                        )
                    )
                }
            }

            Text(
                text =
                    formatIncidentRange(
                        session
                    ),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    if (
                        session.triggered
                    ) {
                        stringResource(
                            R.string
                                .incident_triggered,
                            session.eventCount
                        )
                    } else {
                        stringResource(
                            R.string
                                .incident_not_triggered,
                            session.eventCount
                        )
                    },
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,
                color =
                    if (
                        session.triggered
                    ) {
                        OmniDanger
                    } else {
                        OmniAccent
                    }
            )

            session.triggerReason
                ?.let {
                    reason ->
                    Text(
                        text =
                            reason,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

            session.outputSummary
                ?.let {
                    outputs ->
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .events_evidence_outputs,
                                outputs
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .labelSmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
        }
    }
}

@Composable
private fun EventPanel(
    event: AlarmEvent
) {
    UtilityPanel {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        15.dp
                    ),
            verticalAlignment =
                Alignment.Top,
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(
                            top = 5.dp
                        )
                        .size(
                            9.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            eventAccent(
                                event.type
                            )
                        )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        3.dp
                    )
            ) {
                Text(
                    text =
                        eventLabel(
                            event.type
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        formatEventTimestamp(
                            event.timestamp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                if (
                    event.detail
                        .isNotBlank()
                ) {
                    Text(
                        text =
                            event.detail,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                event.evidence
                    ?.let {
                        evidence ->
                        EvidenceDetails(
                            event =
                                event,
                            evidence =
                                evidence
                        )
                    }
            }
        }
    }
}

@Composable
private fun EvidenceDetails(
    event: AlarmEvent,
    evidence:
        com.riccardopinato.phoneguard.model.AlarmEventEvidence
) {
    val lines =
        buildList {
            evidence.mode
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_mode,
                            it.label
                        )
                    )
                }

            evidence.source
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_source,
                            it
                        )
                    )
                }

            evidence.confidenceScore
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_confidence,
                            it
                        )
                    )
                }

            evidence.detectorState
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_detector,
                            it
                        )
                    )
                }

            evidence.sensorSummary
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_sensors,
                            it
                        )
                    )
                }

            if (
                event.type ==
                    AlarmEventType.TRIGGERED
            ) {
                evidence.outputSummary
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            stringResource(
                                R.string
                                    .events_evidence_outputs,
                                it
                            )
                        )
                    }
            }

            evidence.graceSeconds
                ?.let {
                    add(
                        stringResource(
                            R.string
                                .events_evidence_grace,
                            it
                        )
                    )
                }
        }

    if (
        lines.isNotEmpty()
    ) {
        Text(
            text =
                lines.joinToString(
                    separator = "\n"
                ),
            style =
                MaterialTheme
                    .typography
                    .labelSmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

private fun eventAccent(
    type: AlarmEventType
): Color =
    when (type) {
        AlarmEventType.ARMING_STARTED,
        AlarmEventType.PRE_ALARM_STARTED,
        AlarmEventType.ALARM_OUTPUTS_TIMED_OUT,
        AlarmEventType.CANCELED ->
            OmniWarning

        AlarmEventType.ARMED,
        AlarmEventType.OWNER_VERIFIED,
        AlarmEventType.JOURNEY_ENDED,
        AlarmEventType.DISARMED ->
            OmniAccent

        AlarmEventType.TRIGGERED,
        AlarmEventType.ERROR ->
            OmniDanger
    }

@Composable
private fun eventLabel(
    type: AlarmEventType
): String =
    stringResource(
        when (type) {
            AlarmEventType.ARMING_STARTED ->
                R.string.event_arming

            AlarmEventType.ARMED ->
                R.string.event_armed

            AlarmEventType.OWNER_VERIFIED ->
                R.string.event_owner_verified

            AlarmEventType.TRIGGERED ->
                R.string.event_triggered

            AlarmEventType.PRE_ALARM_STARTED ->
                R.string.event_pre_alarm

            AlarmEventType.ALARM_OUTPUTS_TIMED_OUT ->
                R.string.event_alarm_outputs_timeout

            AlarmEventType.JOURNEY_ENDED ->
                R.string.event_journey_ended

            AlarmEventType.DISARMED ->
                R.string.event_disarmed

            AlarmEventType.CANCELED ->
                R.string.event_canceled

            AlarmEventType.ERROR ->
                R.string.event_error
        }
    )

private fun formatIncidentRange(
    session: IncidentSession
): String {
    val start =
        formatEventTimestamp(
            session.startedAt
        )

    val end =
        session.endedAt
            ?.let(
                ::formatEventTimestamp
            )

    return if (
        end == null
    ) {
        start
    } else {
        "$start → $end"
    }
}

private fun formatEventTimestamp(
    timestamp: Long
): String =
    SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale.getDefault()
    ).format(
        Date(
            timestamp
        )
    )
