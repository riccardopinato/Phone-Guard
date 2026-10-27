package com.riccardopinato.phoneguard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.service.AlarmForegroundService
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmRuntimeState
import com.riccardopinato.phoneguard.ui.components.AlarmStatusCard
import com.riccardopinato.phoneguard.ui.components.PermissionInfoDialog
import com.riccardopinato.phoneguard.ui.components.SetPinDialog
import com.riccardopinato.phoneguard.ui.components.SettingSwitchRow
import com.riccardopinato.phoneguard.ui.components.UtilityChoice
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.components.UtilityPrimaryAction
import com.riccardopinato.phoneguard.ui.components.UtilitySectionHeader
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import kotlinx.coroutines.launch

@Composable
fun AlarmScreen(
    settingsRepository: SettingsRepository,
    securityRepository: SecurityRepository,
    hardware: HardwareSnapshot,
    runtime: AlarmRuntimeState,
    onRequestDisarm: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by
        settingsRepository.settings.collectAsStateWithLifecycle(
            initialValue = AppSettings()
        )

    val hasPin by
        securityRepository.hasPin.collectAsStateWithLifecycle(
            initialValue = false
        )

    var showSetPin by remember { mutableStateOf(false) }
    var showPermissionInfo by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            AlarmForegroundService.arm(context)
        }

    fun missingPermissions(): List<String> = buildList {
        if (
            settings.flashEnabled &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            add(Manifest.permission.CAMERA)
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestArm() {
        if (!hasPin) {
            showSetPin = true
            return
        }

        val missing = missingPermissions()

        if (missing.isEmpty()) {
            AlarmForegroundService.arm(context)
        } else {
            showPermissionInfo = true
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
                text = "TABLETOP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OmniAccent
            )

            Text(
                text = "Motion Alarm",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        AlarmStatusCard(
            runtime = runtime,
            countdownTotal = settings.countdownSeconds,
            hardware = hardware
        )

        if (!hasPin) {
            UtilityPanel(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PIN richiesto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Text(
                        text = "Imposta un PIN prima di armare l'allarme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    UtilityPrimaryAction(
                        label = "Imposta PIN",
                        enabled = true,
                        onClick = { showSetPin = true }
                    )
                }
            }
        }

        UtilityPrimaryAction(
            label = actionLabel(runtime),
            enabled = hardware.hasAccelerometer,
            destructive = runtime.phase == AlarmPhase.TRIGGERED,
            onClick = {
                when (runtime.phase) {
                    AlarmPhase.DISARMED -> requestArm()

                    AlarmPhase.ARMING,
                    AlarmPhase.CALIBRATING,
                    AlarmPhase.ERROR ->
                        AlarmForegroundService.disarm(context)

                    AlarmPhase.ARMED,
                    AlarmPhase.TRIGGERED ->
                        onRequestDisarm()
                }
            }
        )

        UtilitySectionHeader(
            title = "COUNTDOWN",
            trailing = "${settings.countdownSeconds}s"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 10, 15).forEach { seconds ->
                UtilityChoice(
                    label = "${seconds}s",
                    selected = settings.countdownSeconds == seconds,
                    enabled = runtime.phase == AlarmPhase.DISARMED,
                    onClick = {
                        scope.launch {
                            settingsRepository.setCountdown(seconds)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        UtilitySectionHeader(
            title = "SENSIBILITÀ",
            trailing = settings.sensitivity.label
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Sensitivity.entries.forEach { sensitivity ->
                UtilityChoice(
                    label = sensitivity.label,
                    selected = settings.sensitivity == sensitivity,
                    enabled = runtime.phase == AlarmPhase.DISARMED,
                    onClick = {
                        scope.launch {
                            settingsRepository.setSensitivity(sensitivity)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        UtilityPanel {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SettingSwitchRow(
                    title = "Sirena",
                    subtitle = "Tono di allarme locale",
                    checked = settings.soundEnabled,
                    enabled = runtime.phase == AlarmPhase.DISARMED,
                    onCheckedChange = {
                        scope.launch {
                            settingsRepository.setSoundEnabled(it)
                        }
                    }
                )

                SettingSwitchRow(
                    title = "Vibrazione",
                    subtitle = "Pattern continuo durante l'allarme",
                    checked = settings.vibrationEnabled,
                    enabled =
                        runtime.phase == AlarmPhase.DISARMED &&
                            hardware.hasVibrator,
                    onCheckedChange = {
                        scope.launch {
                            settingsRepository.setVibrationEnabled(it)
                        }
                    }
                )

                SettingSwitchRow(
                    title = "Flash LED",
                    subtitle =
                        if (hardware.hasFlash) {
                            "Lampeggio rapido durante l'allarme"
                        } else {
                            "Non disponibile su questo dispositivo"
                        },
                    checked = settings.flashEnabled && hardware.hasFlash,
                    enabled =
                        runtime.phase == AlarmPhase.DISARMED &&
                            hardware.hasFlash,
                    onCheckedChange = {
                        scope.launch {
                            settingsRepository.setFlashEnabled(it)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(76.dp))
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onSave = { pin ->
                scope.launch {
                    securityRepository.setPin(pin)
                    showSetPin = false
                }
            }
        )
    }

    if (showPermissionInfo) {
        val missing = missingPermissions()

        PermissionInfoDialog(
            needsCamera = missing.contains(Manifest.permission.CAMERA),
            needsNotifications =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    missing.contains(Manifest.permission.POST_NOTIFICATIONS),
            onContinue = {
                showPermissionInfo = false
                permissionLauncher.launch(missing.toTypedArray())
            },
            onDismiss = { showPermissionInfo = false }
        )
    }
}

private fun actionLabel(
    runtime: AlarmRuntimeState
): String =
    when (runtime.phase) {
        AlarmPhase.DISARMED -> "ATTIVA ALLARME"
        AlarmPhase.ARMING -> "ANNULLA · ${runtime.countdownSeconds}s"
        AlarmPhase.CALIBRATING -> "ANNULLA CALIBRAZIONE"
        AlarmPhase.ARMED -> "DISATTIVA"
        AlarmPhase.TRIGGERED -> "SBLOCCA E DISATTIVA"
        AlarmPhase.ERROR -> "CHIUDI"
    }
