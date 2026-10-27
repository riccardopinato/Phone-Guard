package com.riccardopinato.phoneguard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmRuntimeState
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniDanger
import com.riccardopinato.phoneguard.ui.theme.OmniHero
import com.riccardopinato.phoneguard.ui.theme.OmniHeroMuted
import com.riccardopinato.phoneguard.ui.theme.OmniHeroSoft
import com.riccardopinato.phoneguard.ui.theme.OmniHeroText
import com.riccardopinato.phoneguard.ui.theme.OmniWarning

@Composable
fun AlarmStatusCard(
    runtime: AlarmRuntimeState,
    countdownTotal: Int,
    hardware: HardwareSnapshot
) {
    if (runtime.phase == AlarmPhase.DISARMED) {
        HardwarePanel(hardware)
    } else {
        ActiveAlarmPanel(
            runtime = runtime,
            countdownTotal = countdownTotal
        )
    }
}

@Composable
private fun HardwarePanel(
    hardware: HardwareSnapshot
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = OmniHero,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(
                text = "Hardware",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniHeroText
            )

            HardwareRow(
                label = "Accelerometro",
                ready = hardware.hasAccelerometer
            )

            HardwareRow(
                label = "Giroscopio",
                ready = hardware.hasGyroscope
            )

            HardwareRow(
                label = "Flash",
                ready = hardware.hasFlash
            )

            HardwareRow(
                label = "Vibrazione",
                ready = hardware.hasVibrator,
                readyText = "Pronta"
            )
        }
    }
}

@Composable
private fun HardwareRow(
    label: String,
    ready: Boolean,
    readyText: String = "Pronto"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniHeroMuted
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (ready) OmniAccent else OmniDanger
                    )
            )

            Text(
                text = if (ready) readyText else "Non disponibile",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OmniHeroText
            )
        }
    }
}

@Composable
private fun ActiveAlarmPanel(
    runtime: AlarmRuntimeState,
    countdownTotal: Int
) {
    val isTriggered = runtime.phase == AlarmPhase.TRIGGERED
    val accent = when (runtime.phase) {
        AlarmPhase.ARMED -> OmniAccent
        AlarmPhase.ARMING,
        AlarmPhase.CALIBRATING -> OmniWarning
        AlarmPhase.TRIGGERED,
        AlarmPhase.ERROR -> OmniDanger
        AlarmPhase.DISARMED -> OmniAccent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = if (isTriggered) Color(0xFF38191D) else OmniHero,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )

                Text(
                    text = phaseTitle(runtime.phase),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmniHeroText
                )
            }

            if (runtime.message.isNotBlank()) {
                Text(
                    text = runtime.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniHeroMuted
                )
            }

            when (runtime.phase) {
                AlarmPhase.ARMING -> {
                    val progress =
                        if (countdownTotal > 0) {
                            1f - runtime.countdownSeconds.toFloat() / countdownTotal.toFloat()
                        } else {
                            0f
                        }

                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = accent,
                        trackColor = OmniHeroSoft
                    )
                }

                AlarmPhase.CALIBRATING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = accent,
                        trackColor = OmniHeroSoft,
                        strokeWidth = 3.dp
                    )
                }

                else -> Unit
            }
        }
    }
}

private fun phaseTitle(
    phase: AlarmPhase
): String =
    when (phase) {
        AlarmPhase.DISARMED -> "Hardware"
        AlarmPhase.ARMING -> "Armamento"
        AlarmPhase.CALIBRATING -> "Calibrazione"
        AlarmPhase.ARMED -> "Protezione attiva"
        AlarmPhase.TRIGGERED -> "Movimento rilevato"
        AlarmPhase.ERROR -> "Errore"
    }
