package com.riccardopinato.phoneguard.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.diagnostics.ProtectionCheckEngine
import com.riccardopinato.phoneguard.diagnostics.ProtectionCheckId
import com.riccardopinato.phoneguard.diagnostics.ProtectionCheckItem
import com.riccardopinato.phoneguard.diagnostics.ProtectionCheckReport
import com.riccardopinato.phoneguard.hardware.HardwareDiagnostics
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.components.UtilitySecondaryAction
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import com.riccardopinato.phoneguard.ui.theme.OmniDanger
import kotlinx.coroutines.launch

@Composable
fun ProtectionCheckScreen(
    settingsRepository: SettingsRepository,
    diagnostics: HardwareDiagnostics,
    alarmRunning: Boolean,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val resources =
        context.resources

    val scope =
        rememberCoroutineScope()

    val engine =
        remember {
            ProtectionCheckEngine(
                context
            )
        }

    val settings by
        settingsRepository.settings
            .collectAsStateWithLifecycle(
                initialValue =
                    AppSettings()
            )

    var report by
        remember {
            mutableStateOf<
                ProtectionCheckReport?
                >(null)
        }

    var statusMessage by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    fun refresh() {
        scope.launch {
            report =
                engine.run(
                    settings.selectedMode
                )
        }
    }

    LaunchedEffect(
        settings.selectedMode
    ) {
        report =
            engine.run(
                settings.selectedMode
            )
    }

    val flashPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) { granted ->
            if (!granted) {
                statusMessage =
                    resources.getString(
                        R.string
                            .camera_permission_denied
                    )
            } else {
                scope.launch {
                    statusMessage =
                        if (
                            diagnostics
                                .testFlash()
                        ) {
                            resources.getString(
                                R.string
                                    .flash_ok
                            )
                        } else {
                            resources.getString(
                                R.string
                                    .flash_failed
                            )
                        }

                    refresh()
                }
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
            Arrangement.spacedBy(
                12.dp
            )
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                stringResource(
                    R.string.common_back
                )
            )
        }

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    2.dp
                )
        ) {
            Text(
                text =
                    stringResource(
                        R.string
                            .protection_check_kicker
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
                            .protection_check_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge
            )
        }

        report?.let {
            current ->

            UtilityPanel {
                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .protection_check_score,
                                current
                                    .readyCount,
                                current
                                    .totalCount
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            stringResource(
                                if (
                                    current
                                        .hasBlockingIssue
                                ) {
                                    R.string
                                        .protection_check_blocked
                                } else {
                                    R.string
                                        .protection_check_ready
                                }
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            if (
                                current
                                    .hasBlockingIssue
                            ) {
                                OmniDanger
                            } else {
                                OmniAccent
                            }
                    )
                }
            }

            current.items
                .forEach {
                    item ->
                    CheckRow(
                        item
                    )
                }
        }

        UtilityPanel {
            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                Text(
                    text =
                        stringResource(
                            R.string
                                .protection_check_manual
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    UtilitySecondaryAction(
                        label =
                            stringResource(
                                R.string
                                    .test_audio
                            ),
                        enabled =
                            !alarmRunning,
                        onClick = {
                            diagnostics
                                .testSound()
                            statusMessage =
                                resources
                                    .getString(
                                        R.string
                                            .audio_test_started
                                    )
                        },
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    UtilitySecondaryAction(
                        label =
                            stringResource(
                                R.string
                                    .test_vibrate
                            ),
                        enabled =
                            !alarmRunning,
                        onClick = {
                            statusMessage =
                                if (
                                    diagnostics
                                        .testVibration()
                                ) {
                                    resources
                                        .getString(
                                            R.string
                                                .vibration_ok
                                        )
                                } else {
                                    resources
                                        .getString(
                                            R.string
                                                .vibration_unavailable
                                        )
                                }
                        },
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    UtilitySecondaryAction(
                        label =
                            stringResource(
                                R.string
                                    .test_flash
                            ),
                        enabled =
                            !alarmRunning,
                        onClick = {
                            flashPermissionLauncher
                                .launch(
                                    Manifest
                                        .permission
                                        .CAMERA
                                )
                        },
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

                UtilitySecondaryAction(
                    label =
                        stringResource(
                            R.string
                                .protection_check_refresh
                        ),
                    enabled =
                        !alarmRunning,
                    onClick = ::refresh
                )
            }
        }

        statusMessage?.let {
            message ->
            UtilityPanel(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {
                Text(
                    text =
                        message,
                    modifier =
                        Modifier.padding(
                            14.dp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun CheckRow(
    item: ProtectionCheckItem
) {
    UtilityPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(
                        9.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        if (
                            item.ready
                        ) {
                            OmniAccent
                        } else {
                            OmniDanger
                        }
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        2.dp
                    )
            ) {
                Text(
                    text =
                        checkLabel(
                            item.id
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
                        item.detail,
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

            Text(
                text =
                    stringResource(
                        if (
                            item.ready
                        ) {
                            R.string
                                .status_ok
                        } else if (
                            item.blocking
                        ) {
                            R.string
                                .protection_check_required
                        } else {
                            R.string
                                .status_optional
                        }
                    ),
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    if (
                        item.ready
                    ) {
                        OmniAccent
                    } else {
                        OmniDanger
                    }
            )
        }
    }
}

@Composable
private fun checkLabel(
    id: ProtectionCheckId
): String =
    stringResource(
        when (id) {
            ProtectionCheckId.SECURITY ->
                R.string
                    .protection_check_security
            ProtectionCheckId.SELECTED_MODE ->
                R.string
                    .protection_check_mode
            ProtectionCheckId.ACCELEROMETER ->
                R.string
                    .status_accelerometer
            ProtectionCheckId.PROXIMITY ->
                R.string
                    .status_proximity
            ProtectionCheckId.AUXILIARY_SENSOR ->
                R.string
                    .protection_check_aux
            ProtectionCheckId.FLASH ->
                R.string
                    .status_flash
            ProtectionCheckId.VIBRATION ->
                R.string
                    .status_vibration
            ProtectionCheckId.NOTIFICATIONS ->
                R.string
                    .protection_check_notifications
            ProtectionCheckId.ALARM_VOLUME ->
                R.string
                    .alarm_volume
            ProtectionCheckId.DO_NOT_DISTURB ->
                R.string
                    .dnd_label
            ProtectionCheckId.BATTERY_OPTIMIZATION ->
                R.string
                    .battery_optimization
            ProtectionCheckId.POWER_CONNECTION ->
                R.string
                    .protection_check_power
        }
    )
