package com.riccardopinato.phoneguard.accessory

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessoryStateMonitorPolicyTest {

    @Test
    fun recoveryProtectsOnlyPersistedIdentities() {
        val original =
            ProtectedAccessory(
                id = 10,
                type = 4,
                name = "Train Buds"
            )

        val later =
            ProtectedAccessory(
                id = 11,
                type = 4,
                name = "Guest Buds"
            )

        val selected =
            AccessoryStateMonitor
                .selectProtectedAccessories(
                    connected =
                        listOf(
                            original,
                            later
                        ),
                    protectedFingerprints =
                        setOf(
                            original
                                .stableFingerprint()
                        )
                )

        assertEquals(
            listOf(
                original
            ),
            selected
        )
    }

    @Test
    fun newArmProtectsAllCurrentlyConnectedSupportedAccessories() {
        val first =
            ProtectedAccessory(
                id = 10,
                type = 4,
                name = "A"
            )

        val second =
            ProtectedAccessory(
                id = 11,
                type = 4,
                name = "B"
            )

        assertEquals(
            listOf(
                first,
                second
            ),
            AccessoryStateMonitor
                .selectProtectedAccessories(
                    connected =
                        listOf(
                            first,
                            second
                        ),
                    protectedFingerprints =
                        null
                )
        )
    }
}
