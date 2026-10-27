package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.activation.ActivationBlockReason
import com.riccardopinato.phoneguard.activation.ActivationOutcome
import com.riccardopinato.phoneguard.activation.ProtectionActivationCoordinator
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.service.AlarmPhase
import com.riccardopinato.phoneguard.service.AlarmStateStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtectionActivationCoordinatorInstrumentedTest {

    @Test
    fun persistedSessionBlocksReplacementActivationBeforeRecovery() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            val app =
                context as PhoneGuardApp

            AlarmStateStore.reset()

            app.container
                .sessionStateRepository
                .saveState(
                    mode =
                        ProtectionMode.TABLE,
                    phase =
                        AlarmPhase.ARMED
                )

            try {
                val result =
                    ProtectionActivationCoordinator
                        .evaluate(
                            context,
                            ProtectionMode.TABLE
                        )

                assertTrue(
                    result is
                        ActivationOutcome.Blocked
                )

                assertEquals(
                    ActivationBlockReason.RECOVERY_PENDING,
                    (
                        result as
                            ActivationOutcome.Blocked
                        ).reason
                )
            } finally {
                app.container
                    .sessionStateRepository
                    .clear()
                AlarmStateStore.reset()
            }
        }
}
