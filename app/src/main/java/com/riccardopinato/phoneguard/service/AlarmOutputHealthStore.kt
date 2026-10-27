package com.riccardopinato.phoneguard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlarmOutputHealth(
    val soundRequested: Boolean = false,
    val soundActive: Boolean = false,
    val vibrationRequested: Boolean = false,
    val vibrationActive: Boolean = false,
    val flashRequested: Boolean = false,
    val flashActive: Boolean = false,
    val targetVolumePercent: Int = 0,
    val effectiveVolumePercent: Int = 0
)

object AlarmOutputHealthStore {
    private val mutable =
        MutableStateFlow(
            AlarmOutputHealth()
        )

    val state:
        StateFlow<AlarmOutputHealth> =
        mutable.asStateFlow()

    fun update(
        value: AlarmOutputHealth
    ) {
        mutable.value = value
    }

    fun reset() {
        mutable.value =
            AlarmOutputHealth()
    }
}
