package com.riccardopinato.phoneguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.riccardopinato.phoneguard.data.PinVerificationResult
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.data.SecurityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityRepositoryInstrumentedTest {

    private lateinit var repository:
        SecurityRepository

    @Before
    fun setUp() =
        runBlocking {
            val context =
                ApplicationProvider
                    .getApplicationContext<Context>()

            repository =
                SecurityRepository(
                    context
                )

            repository.clearPin()
        }

    @After
    fun tearDown() =
        runBlocking {
            repository.clearPin()
        }

    @Test
    fun pinRoundTripUsesRealAndroidKeystore() =
        runBlocking {
            repository.setPin(
                "4826"
            )

            assertEquals(
                SecurityHealth.READY,
                repository
                    .securityHealth
                    .first()
            )

            assertTrue(
                repository.verifyPin(
                    "4826"
                ) is
                    PinVerificationResult
                        .Success
            )
        }

    @Test
    fun wrongPinIsRejectedWithoutDestroyingCredential() =
        runBlocking {
            repository.setPin(
                "4826"
            )

            assertTrue(
                repository.verifyPin(
                    "1111"
                ) is
                    PinVerificationResult
                        .Invalid
            )

            assertEquals(
                SecurityHealth.READY,
                repository
                    .securityHealth
                    .first()
            )
        }
    @Test
    fun malformedPinAttemptsAreRateLimited() =
        runBlocking {
            repository.setPin(
                "4826"
            )

            repeat(4) {
                assertTrue(
                    repository.verifyPin(
                        "x"
                    ) is
                        PinVerificationResult
                            .Invalid
                )
            }

            assertTrue(
                repository.verifyPin(
                    "x"
                ) is
                    PinVerificationResult
                        .LockedOut
            )
        }

}
