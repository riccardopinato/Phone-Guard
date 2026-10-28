package com.riccardopinato.phoneguard.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.autoguard.AutoGuardRuntimePhase
import com.riccardopinato.phoneguard.autoguard.AutoGuardRuntimeStore
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.data.SecurityRepository
import com.riccardopinato.phoneguard.data.SettingsRepository
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmRuntimeState
import com.riccardopinato.phoneguard.service.ProtectionForegroundService
import com.riccardopinato.phoneguard.ui.components.SettingSwitchRow
import com.riccardopinato.phoneguard.ui.components.UtilityChoice
import com.riccardopinato.phoneguard.ui.components.UtilityPanel
import com.riccardopinato.phoneguard.ui.theme.OmniAccent
import kotlinx.coroutines.launch

/**
 * Explicit, opt-in standalone controls. Never confuses foreground standby with
 * an armed protection session, and disabling standby never disarms the owner.
 */
@Composable
fun AutoGuardScreen(
    settingsRepository: SettingsRepository,
    securityRepository: SecurityRepository,
    runtime: AlarmRuntimeState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )
    val securityHealth by securityRepository.securityHealth.collectAsStateWithLifecycle(
        initialValue = SecurityHealth.NO_PIN
    )
    val autoPhase by AutoGuardRuntimeStore.state.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<Int?>(null) }
    val canConfigure = runtime.phase == AlarmPhase.DISARMED

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            settingsRepository.setAdaptiveAutoGuardEnabled(enabled)
            val started = runCatching {
                ProtectionForegroundService.setAdaptiveAutoGuard(context, enabled)
            }.isSuccess
            if (enabled && !started) {
                settingsRepository.setAdaptiveAutoGuardEnabled(false)
                message = R.string.auto_guard_start_failed
            } else {
                message = null
            }
        }
    }

    val notificationsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) setEnabled(true)
        else message = R.string.activation_block_notifications
    }

    val mobilityPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            settingsRepository.setAdaptiveAutoGuardMobility(granted)
            if (settings.adaptiveAutoGuardEnabled) {
                ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
            }
            if (!granted) message = R.string.auto_guard_mobility_permission_denied
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.common_back))
        }
        Text(
            text = stringResource(R.string.auto_guard_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        UtilityPanel {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(
                        when {
                            runtime.phase in setOf(
                                AlarmPhase.ARMED, AlarmPhase.OWNER_CHECK, AlarmPhase.TRIGGERED
                            ) -> R.string.auto_guard_state_protection
                            !canConfigure -> R.string.auto_guard_state_activating
                            !settings.adaptiveAutoGuardEnabled -> R.string.auto_guard_state_off
                            autoPhase == AutoGuardRuntimePhase.CHECKING ->
                                R.string.auto_guard_state_checking
                            autoPhase == AutoGuardRuntimePhase.WAITING ->
                                R.string.auto_guard_state_waiting
                            autoPhase == AutoGuardRuntimePhase.PAUSED ->
                                R.string.auto_guard_state_paused
                            else -> R.string.auto_guard_state_unavailable
                        }
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = OmniAccent
                )
                Text(
                    text = stringResource(R.string.auto_guard_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.auto_guard_standby_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingSwitchRow(
                    title = stringResource(R.string.auto_guard_toggle),
                    subtitle = stringResource(R.string.auto_guard_consent),
                    checked = settings.adaptiveAutoGuardEnabled,
                    enabled = settings.adaptiveAutoGuardEnabled ||
                        (canConfigure && securityHealth == SecurityHealth.READY),
                    onCheckedChange = { enabled ->
                        if (enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            setEnabled(enabled)
                        }
                    }
                )
            }
        }

        if (settings.adaptiveAutoGuardEnabled) {
            UtilityPanel {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auto_guard_delay),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 3, 5, 10, 15).forEach { minutes ->
                            UtilityChoice(
                                label = "${minutes}m",
                                selected = settings.adaptiveAutoGuardIdleMinutes == minutes,
                                enabled = canConfigure,
                                onClick = {
                                    scope.launch {
                                        settingsRepository.setAdaptiveAutoGuardIdleMinutes(minutes)
                                        ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SettingSwitchRow(
                        title = stringResource(R.string.auto_guard_pocket),
                        subtitle = stringResource(R.string.auto_guard_pocket_body),
                        checked = settings.adaptiveAutoGuardPocket,
                        enabled = canConfigure,
                        onCheckedChange = {
                            scope.launch {
                                settingsRepository.setAdaptiveAutoGuardPocket(it)
                                ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
                            }
                        }
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.auto_guard_table),
                        subtitle = stringResource(R.string.auto_guard_table_body),
                        checked = settings.adaptiveAutoGuardTable,
                        enabled = canConfigure,
                        onCheckedChange = {
                            scope.launch {
                                settingsRepository.setAdaptiveAutoGuardTable(it)
                                ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
                            }
                        }
                    )
                    SettingSwitchRow(
                        title = stringResource(R.string.auto_guard_mobility),
                        subtitle = stringResource(R.string.auto_guard_mobility_body),
                        checked = settings.adaptiveAutoGuardMobility,
                        enabled = canConfigure,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACTIVITY_RECOGNITION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                mobilityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                scope.launch {
                                    settingsRepository.setAdaptiveAutoGuardMobility(enabled)
                                    ProtectionForegroundService.setAdaptiveAutoGuard(context, true)
                                }
                            }
                        }
                    )
                }
            }
        }
        message?.let { resource ->
            Text(
                text = stringResource(resource),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
