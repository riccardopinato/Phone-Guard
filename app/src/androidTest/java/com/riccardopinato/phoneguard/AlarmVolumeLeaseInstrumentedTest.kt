package com.riccardopinato.phoneguard

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.alarm.AlarmVolumeLeaseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmVolumeLeaseInstrumentedTest {

    @Test
    fun acquireAndRestoreRoundTrip() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val audio =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val original =
            audio.getStreamVolume(
                AudioManager.STREAM_ALARM
            )

        val lease =
            AlarmVolumeLeaseManager(
                context
            )

        try {
            assertTrue(
                lease.acquire(60)
            )

            assertTrue(
                lease.hasLease()
            )

            assertTrue(
                lease.restoreIfOwned()
            )

            assertEquals(
                original,
                audio.getStreamVolume(
                    AudioManager.STREAM_ALARM
                )
            )
        } finally {
            lease.restoreIfOwned()
        }
    }
}
