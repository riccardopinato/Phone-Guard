package com.riccardopinato.phoneguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        eventRepository.events.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "CRONOLOGIA",
                    style = MaterialTheme.typography.labelMedium,
                    color = OmniAccent,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Eventi",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            if (events.isNotEmpty()) {
                Text(
                    text = "Cancella",
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = eventRepository::clear)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = OmniDanger
                )
            }
        }

        if (events.isEmpty()) {
            UtilityPanel {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nessun evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Le attivazioni e i rilevamenti compariranno qui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(
                    items = events,
                    key = { it.id }
                ) { event ->
                    EventPanel(event)
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(eventAccent(event.type))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = event.type.toItalianLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = formatEventTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (event.detail.isNotBlank()) {
                    Text(
                        text = event.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun eventAccent(
    type: AlarmEventType
): Color =
    when (type) {
        AlarmEventType.ARMING_STARTED -> OmniWarning
        AlarmEventType.ARMED -> OmniAccent
        AlarmEventType.TRIGGERED -> OmniDanger
        AlarmEventType.DISARMED -> OmniAccent
        AlarmEventType.CANCELED -> OmniWarning
        AlarmEventType.ERROR -> OmniDanger
    }

private fun AlarmEventType.toItalianLabel(): String =
    when (this) {
        AlarmEventType.ARMING_STARTED -> "Armamento avviato"
        AlarmEventType.ARMED -> "Allarme armato"
        AlarmEventType.TRIGGERED -> "Movimento rilevato"
        AlarmEventType.DISARMED -> "Allarme disattivato"
        AlarmEventType.CANCELED -> "Armamento annullato"
        AlarmEventType.ERROR -> "Errore"
    }

private fun formatEventTimestamp(
    timestamp: Long
): String =
    SimpleDateFormat(
        "dd/MM/yyyy HH:mm:ss",
        Locale.getDefault()
    ).format(Date(timestamp))
