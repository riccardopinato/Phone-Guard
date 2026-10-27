package com.riccardopinato.phoneguard

import android.app.Application

class PhoneGuardApp : Application() {

    lateinit var container: AppContainer
        private set

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
    }
}
