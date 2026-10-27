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
                                .recoveryIdentity()
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
    fun recoveryDistinguishesIdenticalTypeAndProductNames() {
        val first =
            ProtectedAccessory(
                id = 10,
                type = 4,
                name = "Twin Buds"
            )

        val second =
            ProtectedAccessory(
                id = 11,
                type = 4,
                name = "Twin Buds"
            )

        val expected =
            setOf(
                first.recoveryIdentity(),
                second.recoveryIdentity()
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
                        expected
                )
        )

        assertEquals(
            listOf(
                first
            ),
            AccessoryStateMonitor
                .selectProtectedAccessories(
                    connected =
                        listOf(
                            first
                        ),
                    protectedFingerprints =
                        expected
                )
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
