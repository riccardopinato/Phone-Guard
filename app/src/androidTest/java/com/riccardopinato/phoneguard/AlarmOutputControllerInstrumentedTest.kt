package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import com.riccardopinato.phoneguard.model.AlarmOutputProfile
import com.riccardopinato.phoneguard.service.AlarmOutputController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmOutputControllerInstrumentedTest {

    @Test
    fun volumeLeaseStartsWithAlarmOutputNotControllerConstruction() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val lease =
            AlarmVolumeLeaseManager(
                context
            )

        lease.restoreIfOwned()

        val controller =
            AlarmOutputController(
                context = context,
                volumeLease = lease
            )

        assertFalse(
            lease.hasLease()
        )

        try {
            val health =
                controller.start(
                    AlarmOutputProfile(
                        soundEnabled = true,
                        vibrationEnabled = false,
                        flashEnabled = false,
                        alarmVolumePercent = 60
                    )
                )

            assertTrue(
                health.soundRequested
            )

            assertTrue(
                lease.hasLease()
            )
        } finally {
            controller.stop()
        }

        assertFalse(
            lease.hasLease()
        )
    }
}
