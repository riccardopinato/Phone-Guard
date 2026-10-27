package com.riccardopinato.phoneguard.quickaccess

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhoneGuardTileService :
    TileService() {

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        )

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            openApp()
            return
        }

        unlockAndRun {
            scope.launch {
                val armed =
                    QuickArmCoordinator
                        .arm(
                            this@PhoneGuardTileService
                        )

                if (!armed) {
                    openApp()
                }

                updateTile()
            }
        }
    }

    private fun updateTile() {
        val active =
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED

        qsTile?.apply {
            label =
                getString(
                    R.string.app_name
                )

            state =
                if (active) {
                    Tile.STATE_ACTIVE
                } else {
                    Tile.STATE_INACTIVE
                }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                subtitle =
                    getString(
                        if (active) {
                            R.string
                                .quick_tile_active
                        } else {
                            R.string
                                .quick_tile_inactive
                        }
                    )
            }

            updateTile()
        }
    }

    private fun openApp() {
        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            val pending =
                PendingIntent.getActivity(
                    this,
                    4103,
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

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
