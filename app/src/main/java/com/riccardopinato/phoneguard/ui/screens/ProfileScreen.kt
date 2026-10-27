package com.riccardopinato.phoneguard.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.BuildConfig
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.auth.BiometricAuthManager
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.hardware.BatteryDiagnostics
import com.riccardopinato.phoneguard.hardware.HardwareDiagnostics
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.ui.components.SetPinDialog
import com.riccardopinato.phoneguard.ui.components.SettingSwitchRow
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.components.UtilitySecondaryAction
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniDanger
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    settingsRepository: SettingsRepository,
    securityRepository: SecurityRepository,
    biometricAuthManager: BiometricAuthManager,
    batteryDiagnostics: BatteryDiagnostics,
    onOpenAboutPrivacy: () -> Unit,
    diagnostics: HardwareDiagnostics,
    hardware: HardwareSnapshot,
    alarmRunning: Boolean
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = context.resources

    val settings by
        settingsRepository.settings.collectAsStateWithLifecycle(
            initialValue = AppSettings()
        )

    val hasPin by
        securityRepository.hasPin.collectAsStateWithLifecycle(
            initialValue = false
        )

    val securityHealth by
        securityRepository.securityHealth.collectAsStateWithLifecycle(
            initialValue = SecurityHealth.NO_PIN
        )

    var showSetPin by remember {
        mutableStateOf(false)
    }

    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    val alarmEnvironment =
        remember {
            diagnostics
                .alarmEnvironmentSnapshot()
        }

    val flashPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                scope.launch {
                    statusMessage =
                        if (diagnostics.testFlash()) {
                            resources.getString(R.string.flash_ok)
                        } else {
                            resources.getString(R.string.flash_failed)
                        }
                }
            } else {
                statusMessage = resources.getString(R.string.camera_permission_denied)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_kicker),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OmniAccent
            )

            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineLarge
            )
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTitle(stringResource(R.string.security_section))

                StatusLine(
                    label = "PIN",
                    ready = securityHealth == SecurityHealth.READY,
                    readyText = stringResource(R.string.pin_configured),
                    missingText =
                        if (hasPin) {
                            stringResource(R.string.pin_repair)
                        } else {
                            stringResource(R.string.pin_missing)
                        }
                )

                UtilitySecondaryAction(
                    label =
                        if (hasPin) {
                            stringResource(R.string.pin_change)
                        } else {
                            stringResource(R.string.pin_set)
                        },
                    enabled = !alarmRunning,
                    onClick = { showSetPin = true }
                )

                SettingSwitchRow(
                    title = stringResource(R.string.biometric_title),
                    subtitle = stringResource(R.string.biometric_subtitle),
                    checked = settings.biometricEnabled,
                    enabled =
                        hasPin &&
                            biometricAuthManager.isAvailable() &&
                            !alarmRunning,
                    onCheckedChange = {
                        scope.launch {
                            settingsRepository.setBiometricEnabled(it)
                        }
                    }
                )

                if (!biometricAuthManager.isAvailable()) {
                    Text(
                        text = stringResource(R.string.biometric_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                SectionTitle(stringResource(R.string.diagnostics_title))

                HardwareLine(stringResource(R.string.status_accelerometer), hardware.hasAccelerometer)
                HardwareLine(stringResource(R.string.status_gyroscope), hardware.hasGyroscope)
                HardwareLine(stringResource(R.string.status_proximity), hardware.hasProximity)
                HardwareLine(stringResource(R.string.sensor_light), hardware.hasLight)
                HardwareLine(stringResource(R.string.status_flash), hardware.hasFlash)
                HardwareLine(stringResource(R.string.status_vibration), hardware.hasVibrator)

                StatusLine(
                    label = stringResource(R.string.alarm_volume),
                    ready =
                        alarmEnvironment
                            .alarmVolumePercent > 0,
                    readyText =
                        "${alarmEnvironment.alarmVolumePercent}%",
                    missingText = "0%"
                )

                StatusLine(
                    label = stringResource(R.string.dnd_label),
                    ready =
                        !alarmEnvironment
                            .doNotDisturbActive,
                    readyText = stringResource(R.string.dnd_inactive),
                    missingText =
                        if (
                            alarmEnvironment
                                .notificationPolicyAccess
                        ) {
                            stringResource(R.string.dnd_active_access)
                        } else {
                            stringResource(R.string.dnd_active_no_override)
                        }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UtilitySecondaryAction(
                        label = stringResource(R.string.test_audio),
                        enabled = !alarmRunning,
                        onClick = {
                            diagnostics.testSound()
                            statusMessage = resources.getString(R.string.audio_test_started)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    UtilitySecondaryAction(
                        label = stringResource(R.string.test_vibrate),
                        enabled = !alarmRunning && hardware.hasVibrator,
                        onClick = {
                            statusMessage =
                                if (diagnostics.testVibration()) {
                                    resources.getString(R.string.vibration_ok)
                                } else {
                                    resources.getString(R.string.vibration_unavailable)
                                }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    UtilitySecondaryAction(
                        label = stringResource(R.string.test_flash),
                        enabled = !alarmRunning && hardware.hasFlash,
                        onClick = {
                            flashPermissionLauncher.launch(
                                Manifest.permission.CAMERA
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTitle(stringResource(R.string.background_title))

                val unrestricted =
                    batteryDiagnostics.isIgnoringBatteryOptimizations()

                StatusLine(
                    label = stringResource(R.string.battery_optimization),
                    ready = unrestricted,
                    readyText = stringResource(R.string.battery_excluded),
                    missingText = stringResource(R.string.battery_standard)
                )

                Text(
                    text = stringResource(R.string.background_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                UtilitySecondaryAction(
                    label = stringResource(R.string.battery_settings),
                    enabled = !alarmRunning,
                    onClick = {
                        val opened =
                            batteryDiagnostics.openBatteryOptimizationSettings()

                        statusMessage =
                            if (opened) {
                                resources.getString(R.string.battery_settings_opened)
                            } else {
                                resources.getString(R.string.battery_settings_failed)
                            }
                    }
                )
            }
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTitle(stringResource(R.string.data_info_title))

                Text(
                    text = stringResource(R.string.data_info_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                UtilitySecondaryAction(
                    label = stringResource(R.string.info_privacy),
                    onClick = onOpenAboutPrivacy
                )
            }
        }

        statusMessage?.let { message ->
            UtilityPanel(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "OK",
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { statusMessage = null }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = OmniAccent
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onSave = { pin ->
                scope.launch {
                    securityRepository.setPin(pin)
                    showSetPin = false
                    statusMessage = resources.getString(R.string.pin_updated)
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun StatusLine(
    label: String,
    ready: Boolean,
    readyText: String,
    missingText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (ready) OmniAccent else OmniDanger)
            )

            Text(
                text = if (ready) readyText else missingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HardwareLine(
    label: String,
    available: Boolean
) {
    StatusLine(
        label = label,
        ready = available,
        readyText = stringResource(R.string.status_ok),
        missingText = stringResource(R.string.hardware_missing)
    )
}
