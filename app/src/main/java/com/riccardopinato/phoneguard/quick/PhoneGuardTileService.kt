package com.riccardopinato.phoneguard.quick

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.activation.ActivationOutcome
import com.riccardopinato.phoneguard.activation.ActivationSource
import com.riccardopinato.phoneguard.activation.ProtectionActivationCoordinator
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneGuardTileService :
    TileService() {

    private val scope =
        MainScope()

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()

        val app =
            application as PhoneGuardApp

        val active =
            AlarmStateStore.state
                .value.phase !=
                AlarmPhase.DISARMED ||
                app.container
                    .sessionStateRepository
                    .read() != null

        if (active) {
            openAppSecurely()
            return
        }

        scope.launch {
            val settings =
                app.container
                    .settingsRepository
                    .settings
                    .first()

            when (
                ProtectionActivationCoordinator
                    .requestArm(
                        context =
                            this@PhoneGuardTileService,
                        mode =
                            settings.selectedMode,
                        source =
                            ActivationSource.TILE
                    )
            ) {
                ActivationOutcome.Started ->
                    refreshTile()

                is ActivationOutcome.Blocked ->
                    openAppSecurely()
            }
        }
    }

    private fun refreshTile() {
        val tile =
            qsTile ?: return

        val app =
            application as PhoneGuardApp

        val active =
            AlarmStateStore.state
                .value.phase !=
                AlarmPhase.DISARMED ||
                app.container
                    .sessionStateRepository
                    .read() != null

        tile.state =
            if (active) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }

        tile.label =
            getString(
                R.string.tile_label
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            tile.subtitle =
                getString(
                    if (active) {
                        R.string
                            .tile_subtitle_active
                    } else {
                        R.string
                            .tile_subtitle_ready
                    }
                )
        }

        tile.updateTile()
    }

    private fun openAppSecurely() {
        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }

        val launch =
            Runnable {
                if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES
                    .UPSIDE_DOWN_CAKE
            ) {
                val pending =
                    PendingIntent.getActivity(
                        this,
                        710,
                        intent,
                        PendingIntent
                            .FLAG_UPDATE_CURRENT or
                            PendingIntent
                                .FLAG_IMMUTABLE
                    )

                startActivityAndCollapse(
                    pending
                )
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(
                        intent
                    )
                }
            }

        if (
            isLocked &&
            isSecure
        ) {
            unlockAndRun(
                launch
            )
        } else {
            launch.run()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
