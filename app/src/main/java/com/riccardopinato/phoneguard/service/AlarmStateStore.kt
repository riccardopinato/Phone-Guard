// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/service/AlarmStateStore.kt
package com.riccardopinato.phoneguard.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlarmPhase {
    DISARMED,
    ARMING,
    CALIBRATING,
    ARMED,
    TRIGGERED,
    ERROR
}

data class AlarmRuntimeState(
    val phase: AlarmPhase = AlarmPhase.DISARMED,
    val countdownSeconds: Int = 0,
    val message: String = "Pronto",
    val lastMotionDelta: Float = 0f,
    val triggeredAt: Long? = null
)

object AlarmStateStore {

    private val _state =
        MutableStateFlow(AlarmRuntimeState())

    val state: StateFlow<AlarmRuntimeState> =
        _state.asStateFlow()

    fun update(value: AlarmRuntimeState) {
        _state.value = value
    }

    fun reset() {
        _state.value = AlarmRuntimeState()
    }
}
