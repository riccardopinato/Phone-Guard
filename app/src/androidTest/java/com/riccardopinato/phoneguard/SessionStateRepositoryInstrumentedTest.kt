package com.riccardopinato.phoneguard

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.data.SessionStateRepository
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.service.AlarmPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionStateRepositoryInstrumentedTest {

    @Test
    fun journeyPersistsWallAndMonotonicDeadlinesWithinBoot() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        val repository =
            SessionStateRepository(
                context
            )

        repository.clear()

        val wallEnd =
            System.currentTimeMillis() +
                60_000L

        val elapsedEnd =
            SystemClock.elapsedRealtime() +
                60_000L

        try {
            repository.saveState(
                mode = ProtectionMode.JOURNEY,
                phase = AlarmPhase.ARMED,
                pocketGuardMode =
                    GuardMode.POCKET,
                journeyEndsAt = wallEnd,
                journeyEndsAtElapsedRealtime =
                    elapsedEnd
            )

            val restored =
                repository.read()

            assertNotNull(
                restored
            )

            assertEquals(
                wallEnd,
                restored?.journeyEndsAt
            )

            assertEquals(
                elapsedEnd,
                restored
                    ?.journeyEndsAtElapsedRealtime
            )
        } finally {
            repository.clear()
        }
    }
}
