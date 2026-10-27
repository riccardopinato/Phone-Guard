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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.hardware.HardwareSnapshot
import com.riccardopinato.phoneguard.model.AlarmSoundProfile
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.model.Sensitivity
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.JourneyDuration
import com.riccardopinato.phoneguard.pocket.model.PocketSensitivity
import com.riccardopinato.phoneguard.protection.ProtectionCapabilities
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import com.riccardopinato.phoneguard.protection.ProtectionPreset
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
                text = stringResource(R.string.home_kicker),
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
            title = stringResource(R.string.home_modes),
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
                ProtectionMode.CHARGING
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

        ModeChoice(
            mode =
                ProtectionMode.JOURNEY,
            selected =
                settings.selectedMode ==
                    ProtectionMode.JOURNEY,
            enabled =
                runtime.phase ==
                    AlarmPhase.DISARMED,
            onClick = {
                scope.launch {
                    settingsRepository
                        .setSelectedMode(
                            ProtectionMode.JOURNEY
                        )
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text = modeDescription(selectedMode),
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        PresetOptions(
            enabled =
                runtime.phase ==
                    AlarmPhase.DISARMED,
            onPreset = {
                preset ->
                scope.launch {
                    settingsRepository
                        .applyPreset(
                            preset
                        )
                }
            }
        )

        if (
            selectedMode ==
            ProtectionMode.CHARGING
        ) {
            UtilityPanel {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                ) {
                    SettingSwitchRow(
                        title =
                            stringResource(
                                R.string
                                    .auto_arm_charge_title
                            ),
                        subtitle =
                            stringResource(
                                R.string
                                    .auto_arm_charge_subtitle
                            ),
                        checked =
                            settings
                                .chargeAutoArmEnabled,
                        enabled =
                            runtime.phase ==
                                AlarmPhase.DISARMED,
                        onCheckedChange = {
                            enabled ->
                            scope.launch {
                                settingsRepository
                                    .setChargeAutoArmEnabled(
                                        enabled
                                    )
                            }
                        }
                    )
                }
            }
        }

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
                            stringResource(R.string.security_not_ready),
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
                                    stringResource(R.string.security_key_unavailable)

                                SecurityHealth.CORRUPTED_VERIFIER ->
                                    stringResource(R.string.security_corrupted)

                                else ->
                                    stringResource(R.string.security_configure_pin)
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
                        text = stringResource(R.string.pin_required_title),
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
                            stringResource(R.string.pin_required_body),
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
                        label = stringResource(R.string.pin_set_action),
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
            title = stringResource(R.string.countdown_title),
            trailing =
                stringResource(R.string.seconds_format, settings.countdownSeconds)
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
                    label = stringResource(R.string.seconds_format, seconds),
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

        PreAlarmOptions(
            seconds =
                settings.preAlarmSeconds,
            enabled =
                runtime.phase ==
                    AlarmPhase.DISARMED,
            onChange = {
                seconds ->
                scope.launch {
                    settingsRepository
                        .setPreAlarmSeconds(
                            seconds
                        )
                }
            }
        )

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
        } else if (
            selectedMode !=
            ProtectionMode.CHARGING
        ) {
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

        SoundProfileOptions(
            selected =
                settings
                    .soundProfileFor(
                        selectedMode
                    ),
            enabled =
                runtime.phase ==
                    AlarmPhase.DISARMED,
            onChange = {
                profile ->
                scope.launch {
                    settingsRepository
                        .setAlarmSoundProfile(
                            selectedMode,
                            profile
                        )
                }
            }
        )

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
private fun PresetOptions(
    enabled: Boolean,
    onPreset: (ProtectionPreset) -> Unit
) {
    UtilitySectionHeader(
        title =
            stringResource(
                R.string.presets_title
            )
    )

    Column(
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ProtectionPreset.CAFE,
                ProtectionPreset.COMMUTE,
                ProtectionPreset.BACKPACK
            ).forEach {
                preset ->
                UtilityChoice(
                    label =
                        presetLabel(preset),
                    selected = false,
                    enabled = enabled,
                    onClick = {
                        onPreset(preset)
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                ProtectionPreset.PUBLIC_CHARGING,
                ProtectionPreset.TRIP
            ).forEach {
                preset ->
                UtilityChoice(
                    label =
                        presetLabel(preset),
                    selected = false,
                    enabled = enabled,
                    onClick = {
                        onPreset(preset)
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PreAlarmOptions(
    seconds: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    UtilitySectionHeader(
        title =
            stringResource(
                R.string.prealarm_title
            ),
        trailing =
            if (seconds == 0) {
                stringResource(
                    R.string
                        .prealarm_immediate
                )
            } else {
                stringResource(
                    R.string.seconds_format,
                    seconds
                )
            }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            0,
            2,
            5,
            8
        ).forEach {
            value ->
            UtilityChoice(
                label =
                    if (value == 0) {
                        stringResource(
                            R.string
                                .prealarm_immediate
                        )
                    } else {
                        stringResource(
                            R.string
                                .seconds_format,
                            value
                        )
                    },
                selected =
                    seconds == value,
                enabled = enabled,
                onClick = {
                    onChange(value)
                },
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SoundProfileOptions(
    selected: AlarmSoundProfile,
    enabled: Boolean,
    onChange: (AlarmSoundProfile) -> Unit
) {
    UtilitySectionHeader(
        title =
            stringResource(
                R.string
                    .sound_profile_title
            ),
        trailing =
            soundProfileLabel(
                selected
            )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        AlarmSoundProfile.entries
            .forEach {
                profile ->
                UtilityChoice(
                    label =
                        soundProfileLabel(
                            profile
                        ),
                    selected =
                        selected ==
                            profile,
                    enabled = enabled,
                    onClick = {
                        onChange(profile)
                    },
                    modifier =
                        Modifier.weight(1f)
                )
            }
    }
}

@Composable
private fun TableSensitivity(
    settings: AppSettings,
    enabled: Boolean,
    onChange: (Sensitivity) -> Unit
) {
    UtilitySectionHeader(
        title = stringResource(R.string.table_sensitivity_title),
        trailing =
            sensitivityLabel(settings.sensitivity)
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
                        sensitivityLabel(sensitivity),
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
        title = stringResource(R.string.pocket_sensitivity_title),
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
        title = stringResource(R.string.journey_profile_title),
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
        title = stringResource(R.string.journey_duration_title),
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
        title = stringResource(R.string.escalation_title),
        trailing = stringResource(R.string.alarm_label)
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
                title = stringResource(R.string.siren_title),
                subtitle =
                    stringResource(R.string.siren_subtitle),
                checked =
                    settings.soundEnabled,
                enabled = editable,
                onCheckedChange =
                    setSound
            )

            Text(
                text =
                    stringResource(R.string.volume_target_format, volumeDraft.roundToInt()),
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
                title = stringResource(R.string.vibration_title),
                subtitle =
                    stringResource(R.string.vibration_subtitle),
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
                title = stringResource(R.string.flash_led_title),
                subtitle =
                    if (
                        hardware.hasFlash
                    ) {
                        stringResource(R.string.flash_available_subtitle)
                    } else {
                        stringResource(R.string.unavailable_device)
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
                        stringResource(R.string.flash_frequency_format, flashFrequencyDraft),
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
                title = stringResource(R.string.warning_screen_title),
                subtitle =
                    stringResource(R.string.warning_screen_subtitle),
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

@Composable
private fun actionLabel(
    runtime: AlarmRuntimeState,
    selectedMode: ProtectionMode
): String =
    when (runtime.phase) {
        AlarmPhase.DISARMED ->
            stringResource(
                R.string.action_arm,
                selectedMode.label.uppercase()
            )

        AlarmPhase.ARMING ->
            stringResource(
                R.string.action_cancel_countdown,
                runtime.countdownSeconds
            )

        AlarmPhase.CALIBRATING ->
            stringResource(
                R.string.action_cancel_calibration
            )

        AlarmPhase.ARMED ->
            stringResource(
                R.string.action_disarm,
                runtime.mode.label.uppercase()
            )

        AlarmPhase.OWNER_CHECK ->
            stringResource(
                R.string.action_verify_owner
            )

        AlarmPhase.TRIGGERED ->
            stringResource(
                R.string.action_unlock_disarm
            )

        AlarmPhase.ERROR ->
            stringResource(
                R.string.action_close
            )
    }

@Composable
private fun unsupportedMessage(
    mode: ProtectionMode,
    journeyGuardMode: GuardMode
): String =
    stringResource(
        when (
            ProtectionPolicy
                .effectiveGuardMode(
                    mode,
                    journeyGuardMode
                )
        ) {
            null ->
                R.string.unsupported_accel

            GuardMode.POCKET ->
                R.string.unsupported_pocket

            GuardMode.BAG ->
                R.string.unsupported_bag
        }
    )

@Composable
private fun PocketSensitivity.label():
    String =
    sensitivityLabel(
        when (this) {
            PocketSensitivity.LOW ->
                Sensitivity.LOW

            PocketSensitivity.NORMAL ->
                Sensitivity.MEDIUM

            PocketSensitivity.HIGH ->
                Sensitivity.HIGH
        }
    )

@Composable
private fun sensitivityLabel(
    value: Sensitivity
): String =
    stringResource(
        when (value) {
            Sensitivity.LOW ->
                R.string.sensitivity_low

            Sensitivity.MEDIUM ->
                R.string.sensitivity_normal

            Sensitivity.HIGH ->
                R.string.sensitivity_high
        }
    )

@Composable
private fun JourneyDuration.label():
    String =
    stringResource(
        when (this) {
            JourneyDuration.MINUTES_30 ->
                R.string.duration_30m

            JourneyDuration.HOUR_1 ->
                R.string.duration_1h

            JourneyDuration.HOURS_2 ->
                R.string.duration_2h

            JourneyDuration.UNTIL_STOPPED ->
                R.string.duration_until_stop
        }
    )

@Composable
private fun modeDescription(
    mode: ProtectionMode
): String =
    stringResource(
        when (mode) {
            ProtectionMode.TABLE ->
                R.string.mode_table_description

            ProtectionMode.POCKET ->
                R.string.mode_pocket_description

            ProtectionMode.BAG ->
                R.string.mode_bag_description

            ProtectionMode.CHARGING ->
                R.string.mode_charging_description

            ProtectionMode.JOURNEY ->
                R.string.mode_journey_description
        }
    )

@Composable
private fun presetLabel(
    preset: ProtectionPreset
): String =
    stringResource(
        when (preset) {
            ProtectionPreset.CAFE ->
                R.string.preset_cafe

            ProtectionPreset.COMMUTE ->
                R.string.preset_commute

            ProtectionPreset.BACKPACK ->
                R.string.preset_backpack

            ProtectionPreset.PUBLIC_CHARGING ->
                R.string.preset_public_charging

            ProtectionPreset.TRIP ->
                R.string.preset_trip
        }
    )

@Composable
private fun soundProfileLabel(
    profile: AlarmSoundProfile
): String =
    stringResource(
        when (profile) {
            AlarmSoundProfile.SIREN ->
                R.string.sound_profile_siren

            AlarmSoundProfile.WARNING ->
                R.string.sound_profile_warning

            AlarmSoundProfile.BEEP ->
                R.string.sound_profile_beep
        }
    )

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
