package com.riccardopinato.phoneguard.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.EventRepository
import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
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
    val events by
        eventRepository.events
            .collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
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
            modifier = Modifier
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
                    color = OmniAccent,
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

            if (events.isNotEmpty()) {
                TextButton(
                    onClick =
                        eventRepository::clear
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
                        color = OmniDanger
                    )
                }
            }
        }

        if (events.isEmpty()) {
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
                items(
                    items = events,
                    key = {
                        it.id
                    }
                ) { event ->
                    EventPanel(
                        event
                    )
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment =
                Alignment.Top,
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        top = 5.dp
                    )
                    .size(9.dp)
                    .clip(CircleShape)
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
                        text = event.detail,
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

                if (
                    event.metadata
                        .isNotEmpty()
                ) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                2.dp
                            )
                    ) {
                        event.metadata
                            .entries
                            .sortedBy {
                                it.key
                            }
                            .take(10)
                            .forEach {
                                entry ->
                                Text(
                                    text =
                                        "${metadataLabel(entry.key)}: ${entry.value}",
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
        }
    }
}

private fun eventAccent(
    type: AlarmEventType
): Color =
    when (type) {
        AlarmEventType.ARMING_STARTED ->
            OmniWarning

        AlarmEventType.ARMED,
        AlarmEventType.OWNER_VERIFIED,
        AlarmEventType.JOURNEY_ENDED,
        AlarmEventType.DISARMED ->
            OmniAccent

        AlarmEventType.TRIGGERED,
        AlarmEventType.ERROR ->
            OmniDanger

        AlarmEventType.CANCELED ->
            OmniWarning
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

private fun metadataLabel(
    key: String
): String =
    key
        .replace(
            "_",
            " "
        )
        .replaceFirstChar {
            if (
                it.isLowerCase()
            ) {
                it.titlecase(
                    Locale.getDefault()
                )
            } else {
                it.toString()
            }
        }

private fun formatEventTimestamp(
    timestamp: Long
): String =
    SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale.getDefault()
    ).format(
        Date(timestamp)
    )
