package com.riccardopinato.phoneguard

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.quickaccess.PowerConnectionReceiver

class PhoneGuardApp : Application() {

    lateinit var container: AppContainer
        private set

    private val powerConnectionReceiver =
        PowerConnectionReceiver()

    override fun onCreate() {
        super.onCreate()

        container =
            AppContainer(
                applicationContext
            )

        if (
            container
                .sessionStateRepository
                .read() == null
        ) {
            container
                .alarmVolumeLeaseManager
                .restoreIfOwned()
        }

        ContextCompat.registerReceiver(
            applicationContext,
            powerConnectionReceiver,
            IntentFilter(
                Intent.ACTION_POWER_CONNECTED
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
