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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.BuildConfig
import com.riccardopinato.phoneguard.auth.BiometricAuthManager
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

    val settings by
        settingsRepository.settings.collectAsStateWithLifecycle(
            initialValue = AppSettings()
        )

    val hasPin by
        securityRepository.hasPin.collectAsStateWithLifecycle(
            initialValue = false
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
                            "Flash OK"
                        } else {
                            "Test flash non riuscito."
                        }
                }
            } else {
                statusMessage = "Permesso fotocamera non concesso."
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
                text = "IMPOSTAZIONI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OmniAccent
            )

            Text(
                text = "Profilo e sicurezza",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTitle("Sicurezza")

                StatusLine(
                    label = "PIN",
                    ready = hasPin,
                    readyText = "Configurato",
                    missingText = "Da configurare"
                )

                UtilitySecondaryAction(
                    label = if (hasPin) "Cambia PIN" else "Imposta PIN",
                    enabled = !alarmRunning,
                    onClick = { showSetPin = true }
                )

                SettingSwitchRow(
                    title = "Biometria",
                    subtitle = "Usa biometria forte per disattivare l'allarme",
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
                        text = "Biometria forte non disponibile o non configurata.",
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
                SectionTitle("Diagnostica hardware")

                HardwareLine("Accelerometro", hardware.hasAccelerometer)
                HardwareLine("Giroscopio", hardware.hasGyroscope)
                HardwareLine("Prossimità", hardware.hasProximity)
                HardwareLine("Sensore luce", hardware.hasLight)
                HardwareLine("Flash", hardware.hasFlash)
                HardwareLine("Vibrazione", hardware.hasVibrator)

                StatusLine(
                    label = "Volume allarme",
                    ready =
                        alarmEnvironment
                            .alarmVolumePercent > 0,
                    readyText =
                        "${alarmEnvironment.alarmVolumePercent}%",
                    missingText = "0%"
                )

                StatusLine(
                    label = "Non disturbare",
                    ready =
                        !alarmEnvironment
                            .doNotDisturbActive,
                    readyText = "Non attivo",
                    missingText =
                        if (
                            alarmEnvironment
                                .notificationPolicyAccess
                        ) {
                            "Attivo · accesso policy"
                        } else {
                            "Attivo · nessun override"
                        }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UtilitySecondaryAction(
                        label = "Audio",
                        enabled = !alarmRunning,
                        onClick = {
                            diagnostics.testSound()
                            statusMessage = "Test audio avviato."
                        },
                        modifier = Modifier.weight(1f)
                    )

                    UtilitySecondaryAction(
                        label = "Vibra",
                        enabled = !alarmRunning && hardware.hasVibrator,
                        onClick = {
                            statusMessage =
                                if (diagnostics.testVibration()) {
                                    "Vibrazione OK"
                                } else {
                                    "Vibrazione non disponibile."
                                }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    UtilitySecondaryAction(
                        label = "Flash",
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
                SectionTitle("Background")

                val unrestricted =
                    batteryDiagnostics.isIgnoringBatteryOptimizations()

                StatusLine(
                    label = "Ottimizzazione batteria",
                    ready = unrestricted,
                    readyText = "Esclusa",
                    missingText = "Standard"
                )

                Text(
                    text =
                        "Table, Pocket, Bag e Journey usano un unico Foreground Service visibile. " +
                            "Controlla le impostazioni solo se noti interruzioni.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                UtilitySecondaryAction(
                    label = "Impostazioni batteria",
                    enabled = !alarmRunning,
                    onClick = {
                        val opened =
                            batteryDiagnostics.openBatteryOptimizationSettings()

                        statusMessage =
                            if (opened) {
                                "Impostazioni batteria aperte."
                            } else {
                                "Impossibile aprire le impostazioni batteria."
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
                SectionTitle("Dati e informazioni")

                Text(
                    text =
                        "Le impostazioni non sensibili possono usare Android Auto Backup. " +
                            "PIN, dati di sicurezza e storico eventi restano esclusi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                UtilitySecondaryAction(
                    label = "Info e privacy",
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
            text = "Versione ${BuildConfig.VERSION_NAME}",
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
                    statusMessage = "PIN aggiornato."
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
        readyText = "OK",
        missingText = "Non presente"
    )
}
