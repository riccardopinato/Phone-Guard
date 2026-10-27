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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmRuntimeState
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniDanger
import com.riccardopinato.phoneguard.ui.theme.OmniHero
import com.riccardopinato.phoneguard.ui.theme.OmniHeroMuted
import com.riccardopinato.phoneguard.ui.theme.OmniHeroText
import com.riccardopinato.phoneguard.ui.theme.OmniWarning
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun AlarmStatusCard(
    runtime: AlarmRuntimeState,
    countdownTotal: Int,
    hardware: HardwareSnapshot,
    selectedMode: ProtectionMode,
    requiresProximity: Boolean
) {
    if (runtime.phase == AlarmPhase.DISARMED) {
        HardwarePanel(
            hardware = hardware,
            selectedMode = selectedMode,
            requiresProximity = requiresProximity
        )
    } else {
        ActiveAlarmPanel(
            runtime = runtime,
            countdownTotal = countdownTotal
        )
    }
}

@Composable
private fun HardwarePanel(
    hardware: HardwareSnapshot,
    selectedMode: ProtectionMode,
    requiresProximity: Boolean
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
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(11.dp)
        ) {
            Text(
                text =
                    stringResource(R.string.status_hardware_mode, selectedMode.label),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniHeroText
            )

            HardwareRow(
                label =
                    stringResource(
                        R.string.status_accelerometer
                    ),
                ready =
                    hardware.hasAccelerometer,
                optional =
                    selectedMode ==
                        ProtectionMode.CHARGING ||
                        selectedMode ==
                            ProtectionMode.ACCESSORY
            )

            HardwareRow(
                label = stringResource(R.string.status_gyroscope),
                ready = hardware.hasGyroscope,
                optional = true
            )

            HardwareRow(
                label = stringResource(R.string.status_proximity),
                ready = hardware.hasProximity,
                optional = !requiresProximity
            )

            HardwareRow(
                label = stringResource(R.string.status_light),
                ready = hardware.hasLight,
                optional = true
            )

            HardwareRow(
                label = stringResource(R.string.status_flash),
                ready = hardware.hasFlash,
                optional = true
            )

            HardwareRow(
                label = stringResource(R.string.status_vibration),
                ready = hardware.hasVibrator,
                optional = true
            )
        }
    }
}

@Composable
private fun HardwareRow(
    label: String,
    ready: Boolean,
    optional: Boolean = false
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
                        if (ready) {
                            OmniAccent
                        } else if (optional) {
                            OmniWarning
                        } else {
                            OmniDanger
                        }
                    )
            )

            Text(
                text =
                    when {
                        ready -> stringResource(R.string.status_ok)
                        optional -> stringResource(R.string.status_optional)
                        else -> stringResource(R.string.status_required)
                    },
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
    val locale =
        LocalConfiguration.current.locales[0]

    val isTriggered =
        runtime.phase == AlarmPhase.TRIGGERED

    val accent =
        when (runtime.phase) {
            AlarmPhase.ARMED ->
                OmniAccent

            AlarmPhase.ARMING,
            AlarmPhase.CALIBRATING,
            AlarmPhase.OWNER_CHECK ->
                OmniWarning

            AlarmPhase.TRIGGERED,
            AlarmPhase.ERROR ->
                OmniDanger

            AlarmPhase.DISARMED ->
                OmniAccent
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color =
            if (isTriggered) {
                Color(0xFF38191D)
            } else {
                OmniHero
            },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
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

            Text(
                text = runtime.mode.label,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )

            Text(
                text = runtime.message,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniHeroMuted
            )

            if (
                runtime.phase == AlarmPhase.ARMING &&
                countdownTotal > 0
            ) {
                val progress =
                    1f -
                        (
                            runtime.countdownSeconds.toFloat() /
                                countdownTotal.toFloat()
                            )
                            .coerceIn(
                                0f,
                                1f
                            )

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                    trackColor =
                        Color.White.copy(alpha = 0.10f)
                )
            }

            runtime.detectorState
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Text(
                        text = stringResource(R.string.status_detector, it),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniHeroMuted
                    )
                }

            if (
                runtime.confidenceScore > 0 &&
                runtime.mode != ProtectionMode.TABLE
            ) {
                Text(
                    text =
                        stringResource(R.string.status_confidence, runtime.confidenceScore),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniHeroMuted
                )
            }

            runtime.journeyEndsAt
                ?.let { endAt ->
                    Text(
                        text =
                            stringResource(
                                R.string.status_journey_until,
                                SimpleDateFormat(
                                    "HH:mm",
                                    locale
                                ).format(
                                    Date(endAt)
                                )
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniHeroMuted
                    )
                }
        }
    }
}

@Composable
private fun phaseTitle(
    phase: AlarmPhase
): String =
    stringResource(
        when (phase) {
            AlarmPhase.DISARMED ->
                R.string.phase_ready

            AlarmPhase.ARMING ->
                R.string.phase_preparing

            AlarmPhase.CALIBRATING ->
                R.string.phase_calibrating

            AlarmPhase.ARMED ->
                R.string.phase_active

            AlarmPhase.OWNER_CHECK ->
                R.string.phase_owner_check

            AlarmPhase.TRIGGERED ->
                R.string.phase_alarm

            AlarmPhase.ERROR ->
                R.string.phase_error
        }
    )
