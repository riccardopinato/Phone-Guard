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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import com.riccardopinato.phoneguard.protection.ProtectionCapabilities
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmRuntimeState
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
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
import kotlin.math.roundToInt

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

    val securityHealth by
        securityRepository.securityHealth.collectAsStateWithLifecycle(
            initialValue =
                SecurityHealth.NO_PIN
        )

    var showSetPin by remember { mutableStateOf(false) }
    var showPermissionInfo by remember { mutableStateOf(false) }

    var volumeDraft by
        remember(settings.alarmVolumePercent) {
            mutableFloatStateOf(
                settings.alarmVolumePercent.toFloat()
            )
        }

    var flashFrequencyDraft by
        remember(settings.flashFrequencyHz) {
            mutableFloatStateOf(settings.flashFrequencyHz)
        }

    val selectedMode =
        if (runtime.phase == AlarmPhase.DISARMED) {
            settings.selectedMode
        } else {
            runtime.mode
        }

    val modeSupported =
        ProtectionPolicy.isSupported(
            mode = selectedMode,
            journeyGuardMode =
                settings.journeyGuardMode,
            capabilities =
                ProtectionCapabilities(
                    hasAccelerometer =
                        hardware.hasAccelerometer,
                    hasProximity =
                        hardware.hasProximity,
                    hasLight =
                        hardware.hasLight,
                    hasGyroscope =
                        hardware.hasGyroscope
                )
        )

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestMultiplePermissions()
        ) {
            ProtectionForegroundService.arm(
                context,
                settings.selectedMode
            )
        }

    fun missingPermissions(): List<String> =
        buildList {
            if (
                settings.flashEnabled &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(
                    Manifest.permission.CAMERA
                )
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

    fun requestArm() {
        if (!hasPin) {
            showSetPin = true
            return
        }

        if (
            securityHealth !=
            SecurityHealth.READY
        ) {
            return
        }

        if (!modeSupported) {
            return
        }

        val missing =
            missingPermissions()

        if (missing.isEmpty()) {
            ProtectionForegroundService.arm(
                context,
                settings.selectedMode
            )
        } else {
            showPermissionInfo = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 18.dp,
                vertical = 14.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "PROTEZIONE",
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                fontWeight =
                    FontWeight.Bold,
                color = OmniAccent
            )

            Text(
                text = "Phone Guard",
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge
            )
        }

        UtilitySectionHeader(
            title = "MODALITÀ",
            trailing = selectedMode.label
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ProtectionMode.TABLE,
                ProtectionMode.POCKET
            ).forEach { mode ->
                ModeChoice(
                    mode = mode,
                    selected =
                        settings.selectedMode == mode,
                    enabled =
                        runtime.phase ==
                            AlarmPhase.DISARMED,
                    onClick = {
                        scope.launch {
                            settingsRepository
                                .setSelectedMode(mode)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ProtectionMode.BAG,
                ProtectionMode.JOURNEY
            ).forEach { mode ->
                ModeChoice(
                    mode = mode,
                    selected =
                        settings.selectedMode == mode,
                    enabled =
                        runtime.phase ==
                            AlarmPhase.DISARMED,
                    onClick = {
                        scope.launch {
                            settingsRepository
                                .setSelectedMode(mode)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = selectedMode.description,
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        AlarmStatusCard(
            runtime = runtime,
            countdownTotal =
                settings.countdownSeconds,
            hardware = hardware,
            selectedMode = selectedMode,
            requiresProximity =
                selectedMode == ProtectionMode.POCKET ||
                    (
                        selectedMode == ProtectionMode.JOURNEY &&
                            settings.journeyGuardMode == GuardMode.POCKET
                        )
        )

        if (!modeSupported) {
            UtilityPanel(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .errorContainer
            ) {
                Text(
                    text =
                        unsupportedMessage(
                            selectedMode,
                            settings.journeyGuardMode
                        ),
                    modifier =
                        Modifier.padding(16.dp),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onErrorContainer
                )
            }
        }

        if (
            hasPin &&
            securityHealth !=
                SecurityHealth.READY
        ) {
            UtilityPanel(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .errorContainer
            ) {
                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text =
                            "Sicurezza locale non pronta",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )

                    Text(
                        text =
                            when (securityHealth) {
                                SecurityHealth.KEY_UNAVAILABLE ->
                                    "La chiave Android Keystore del PIN non è disponibile. Reimposta il PIN dal Profilo prima di armare."

                                SecurityHealth.CORRUPTED_VERIFIER ->
                                    "I dati locali del PIN non sono validi. Reimposta il PIN dal Profilo prima di armare."

                                else ->
                                    "Configura un PIN valido prima di armare."
                            },
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )
                }
            }
        }

        if (!hasPin) {
            UtilityPanel(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .errorContainer
            ) {
                Column(
                    modifier =
                        Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement
                            .spacedBy(8.dp)
                ) {
                    Text(
                        text = "PIN richiesto",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )

                    Text(
                        text =
                            "Imposta un PIN prima di armare la protezione.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onErrorContainer
                    )

                    UtilityPrimaryAction(
                        label = "Imposta PIN",
                        enabled = true,
                        onClick = {
                            showSetPin = true
                        }
                    )
                }
            }
        }

        UtilityPrimaryAction(
            label =
                actionLabel(
                    runtime,
                    selectedMode
                ),
            enabled =
                if (
                    runtime.phase ==
                    AlarmPhase.DISARMED
                ) {
                    modeSupported &&
                        (
                            securityHealth ==
                                SecurityHealth.READY ||
                                securityHealth ==
                                SecurityHealth.NO_PIN
                            )
                } else {
                    true
                },
            destructive =
                runtime.phase ==
                    AlarmPhase.TRIGGERED,
            onClick = {
                when (runtime.phase) {
                    AlarmPhase.DISARMED ->
                        requestArm()

                    AlarmPhase.ARMING,
                    AlarmPhase.CALIBRATING,
                    AlarmPhase.ERROR ->
                        ProtectionForegroundService
                            .disarm(context)

                    AlarmPhase.ARMED,
                    AlarmPhase.OWNER_CHECK,
                    AlarmPhase.TRIGGERED ->
                        onRequestDisarm()
                }
            }
        )

        if (
            selectedMode ==
            ProtectionMode.JOURNEY
        ) {
            JourneyOptions(
                settings = settings,
                enabled =
                    runtime.phase ==
                        AlarmPhase.DISARMED,
                onGuardMode = {
                    scope.launch {
                        settingsRepository
                            .setJourneyGuardMode(it)
                    }
                },
                onDuration = {
                    scope.launch {
                        settingsRepository
                            .setJourneyDuration(it)
                    }
                }
            )
        }

        UtilitySectionHeader(
            title = "COUNTDOWN",
            trailing =
                "${settings.countdownSeconds}s"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                3,
                5,
                10,
                15
            ).forEach { seconds ->
                UtilityChoice(
                    label = "${seconds}s",
                    selected =
                        settings
                            .countdownSeconds ==
                            seconds,
                    enabled =
                        runtime.phase ==
                            AlarmPhase.DISARMED,
                    onClick = {
                        scope.launch {
                            settingsRepository
                                .setCountdown(
                                    seconds
                                )
                        }
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        if (
            selectedMode ==
            ProtectionMode.TABLE
        ) {
            TableSensitivity(
                settings = settings,
                enabled =
                    runtime.phase ==
                        AlarmPhase.DISARMED,
                onChange = {
                    scope.launch {
                        settingsRepository
                            .setSensitivity(it)
                    }
                }
            )
        } else {
            PocketSensitivityOptions(
                settings = settings,
                enabled =
                    runtime.phase ==
                        AlarmPhase.DISARMED,
                onChange = {
                    scope.launch {
                        settingsRepository
                            .setPocketSensitivity(it)
                    }
                }
            )
        }

        AlarmOutputOptions(
            settings = settings,
            hardware = hardware,
            runtime = runtime,
            volumeDraft = volumeDraft,
            onVolumeDraft = {
                volumeDraft = it
            },
            onVolumeCommit = {
                scope.launch {
                    settingsRepository
                        .setAlarmVolumePercent(
                            volumeDraft
                                .roundToInt()
                        )
                }
            },
            flashFrequencyDraft =
                flashFrequencyDraft,
            onFlashFrequencyDraft = {
                flashFrequencyDraft = it
            },
            onFlashFrequencyCommit = {
                scope.launch {
                    settingsRepository
                        .setFlashFrequencyHz(
                            flashFrequencyDraft
                        )
                }
            },
            setSound = {
                scope.launch {
                    settingsRepository
                        .setSoundEnabled(it)
                }
            },
            setVibration = {
                scope.launch {
                    settingsRepository
                        .setVibrationEnabled(it)
                }
            },
            setFlash = {
                scope.launch {
                    settingsRepository
                        .setFlashEnabled(it)
                }
            },
            setWarningScreen = {
                scope.launch {
                    settingsRepository
                        .setWarningScreenEnabled(it)
                }
            }
        )

        Spacer(
            Modifier.height(76.dp)
        )
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = {
                showSetPin = false
            },
            onSave = { pin ->
                scope.launch {
                    securityRepository
                        .setPin(pin)
                    showSetPin = false
                }
            }
        )
    }

    if (showPermissionInfo) {
        val missing =
            missingPermissions()

        PermissionInfoDialog(
            needsCamera =
                missing.contains(
                    Manifest.permission.CAMERA
                ),
            needsNotifications =
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES
                        .TIRAMISU &&
                    missing.contains(
                        Manifest.permission
                            .POST_NOTIFICATIONS
                    ),
            onContinue = {
                showPermissionInfo = false
                permissionLauncher.launch(
                    missing.toTypedArray()
                )
            },
            onDismiss = {
                showPermissionInfo = false
            }
        )
    }
}

@Composable
private fun ModeChoice(
    mode: ProtectionMode,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    UtilityChoice(
        label = mode.label,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TableSensitivity(
    settings: AppSettings,
    enabled: Boolean,
    onChange: (Sensitivity) -> Unit
) {
    UtilitySectionHeader(
        title = "SENSIBILITÀ TABLE",
        trailing =
            settings.sensitivity.label
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Sensitivity.entries
            .forEach { sensitivity ->
                UtilityChoice(
                    label =
                        sensitivity.label,
                    selected =
                        settings
                            .sensitivity ==
                            sensitivity,
                    enabled = enabled,
                    onClick = {
                        onChange(
                            sensitivity
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
    }
}

@Composable
private fun PocketSensitivityOptions(
    settings: AppSettings,
    enabled: Boolean,
    onChange: (PocketSensitivity) -> Unit
) {
    UtilitySectionHeader(
        title = "SENSIBILITÀ POCKET/BAG",
        trailing =
            settings
                .pocketSensitivity
                .label()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        PocketSensitivity.entries
            .forEach { sensitivity ->
                UtilityChoice(
                    label =
                        sensitivity.label(),
                    selected =
                        settings
                            .pocketSensitivity ==
                            sensitivity,
                    enabled = enabled,
                    onClick = {
                        onChange(
                            sensitivity
                        )
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
    }
}

@Composable
private fun JourneyOptions(
    settings: AppSettings,
    enabled: Boolean,
    onGuardMode: (GuardMode) -> Unit,
    onDuration: (JourneyDuration) -> Unit
) {
    UtilitySectionHeader(
        title = "JOURNEY PROFILE",
        trailing =
            settings.journeyGuardMode.name
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        GuardMode.entries.forEach { guard ->
            UtilityChoice(
                label =
                    if (
                        guard ==
                        GuardMode.POCKET
                    ) {
                        "Pocket"
                    } else {
                        "Bag"
                    },
                selected =
                    settings
                        .journeyGuardMode ==
                        guard,
                enabled = enabled,
                onClick = {
                    onGuardMode(guard)
                },
                modifier =
                    Modifier.weight(1f)
            )
        }
    }

    UtilitySectionHeader(
        title = "DURATA JOURNEY",
        trailing =
            settings
                .journeyDuration
                .label()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        JourneyDuration.entries
            .forEach { duration ->
                UtilityChoice(
                    label =
                        duration.shortLabel(),
                    selected =
                        settings
                            .journeyDuration ==
                            duration,
                    enabled = enabled,
                    onClick = {
                        onDuration(duration)
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
    }
}

@Composable
private fun AlarmOutputOptions(
    settings: AppSettings,
    hardware: HardwareSnapshot,
    runtime: AlarmRuntimeState,
    volumeDraft: Float,
    onVolumeDraft: (Float) -> Unit,
    onVolumeCommit: () -> Unit,
    flashFrequencyDraft: Float,
    onFlashFrequencyDraft: (Float) -> Unit,
    onFlashFrequencyCommit: () -> Unit,
    setSound: (Boolean) -> Unit,
    setVibration: (Boolean) -> Unit,
    setFlash: (Boolean) -> Unit,
    setWarningScreen: (Boolean) -> Unit
) {
    val editable =
        runtime.phase ==
            AlarmPhase.DISARMED

    UtilitySectionHeader(
        title = "ESCALATION",
        trailing = "Allarme"
    )

    UtilityPanel {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            SettingSwitchRow(
                title = "Sirena",
                subtitle =
                    "Tono di allarme locale",
                checked =
                    settings.soundEnabled,
                enabled = editable,
                onCheckedChange =
                    setSound
            )

            Text(
                text =
                    "Volume target: " +
                        "${volumeDraft.roundToInt()}%",
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Slider(
                value = volumeDraft,
                onValueChange =
                    onVolumeDraft,
                onValueChangeFinished =
                    onVolumeCommit,
                valueRange = 0f..100f,
                steps = 19,
                enabled =
                    editable &&
                        settings.soundEnabled
            )

            SettingSwitchRow(
                title = "Vibrazione",
                subtitle =
                    "Pattern continuo durante l'allarme",
                checked =
                    settings
                        .vibrationEnabled,
                enabled =
                    editable &&
                        hardware.hasVibrator,
                onCheckedChange =
                    setVibration
            )

            SettingSwitchRow(
                title = "Flash LED",
                subtitle =
                    if (
                        hardware.hasFlash
                    ) {
                        "Strobo configurabile fino a 3 Hz durante l'allarme"
                    } else {
                        "Non disponibile su questo dispositivo"
                    },
                checked =
                    settings.flashEnabled &&
                        hardware.hasFlash,
                enabled =
                    editable &&
                        hardware.hasFlash,
                onCheckedChange =
                    setFlash
            )

            if (
                hardware.hasFlash &&
                settings.flashEnabled
            ) {
                Text(
                    text =
                        "Frequenza flash: " +
                            String.format(
                                "%.1f Hz",
                                flashFrequencyDraft
                            ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                Slider(
                    value =
                        flashFrequencyDraft,
                    onValueChange =
                        onFlashFrequencyDraft,
                    onValueChangeFinished =
                        onFlashFrequencyCommit,
                    valueRange =
                        0.5f..3f,
                    steps = 4,
                    enabled = editable
                )
            }

            SettingSwitchRow(
                title = "Warning screen",
                subtitle =
                    "Schermata rossa ad alta visibilità quando scatta l'allarme",
                checked =
                    settings
                        .warningScreenEnabled,
                enabled = editable,
                onCheckedChange =
                    setWarningScreen
            )
        }
    }
}

private fun actionLabel(
    runtime: AlarmRuntimeState,
    selectedMode: ProtectionMode
): String =
    when (runtime.phase) {
        AlarmPhase.DISARMED ->
            "ATTIVA ${selectedMode.label.uppercase()}"

        AlarmPhase.ARMING ->
            "ANNULLA · ${runtime.countdownSeconds}s"

        AlarmPhase.CALIBRATING ->
            "ANNULLA CALIBRAZIONE"

        AlarmPhase.ARMED ->
            "DISATTIVA ${runtime.mode.label.uppercase()}"

        AlarmPhase.OWNER_CHECK ->
            "VERIFICA PROPRIETARIO"

        AlarmPhase.TRIGGERED ->
            "SBLOCCA E DISATTIVA"

        AlarmPhase.ERROR ->
            "CHIUDI"
    }

private fun unsupportedMessage(
    mode: ProtectionMode,
    journeyGuardMode: GuardMode
): String =
    when (
        ProtectionPolicy
            .effectiveGuardMode(
                mode,
                journeyGuardMode
            )
    ) {
        null ->
            "Questa modalità richiede l'accelerometro."

        GuardMode.POCKET ->
            "Pocket richiede accelerometro e sensore di prossimità."

        GuardMode.BAG ->
            "Bag richiede accelerometro e almeno un sensore tra luce e giroscopio."
    }

private fun PocketSensitivity.label():
    String =
    when (this) {
        PocketSensitivity.LOW ->
            "Bassa"

        PocketSensitivity.NORMAL ->
            "Normale"

        PocketSensitivity.HIGH ->
            "Alta"
    }

private fun JourneyDuration.label():
    String =
    when (this) {
        JourneyDuration.MINUTES_30 ->
            "30 minuti"

        JourneyDuration.HOUR_1 ->
            "1 ora"

        JourneyDuration.HOURS_2 ->
            "2 ore"

        JourneyDuration.UNTIL_STOPPED ->
            "Fino allo stop"
    }

private fun JourneyDuration.shortLabel():
    String =
    when (this) {
        JourneyDuration.MINUTES_30 ->
            "30m"

        JourneyDuration.HOUR_1 ->
            "1h"

        JourneyDuration.HOURS_2 ->
            "2h"

        JourneyDuration.UNTIL_STOPPED ->
            "∞"
    }
