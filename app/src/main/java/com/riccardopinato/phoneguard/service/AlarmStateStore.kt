package com.riccardopinato.phoneguard.service

import com.riccardopinato.phoneguard.model.ProtectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlarmPhase {
    DISARMED,
    ARMING,
    CALIBRATING,
    ARMED,
    OWNER_CHECK,
    TRIGGERED,
    ERROR
}

data class AlarmRuntimeState(
    val phase: AlarmPhase = AlarmPhase.DISARMED,
    val mode: ProtectionMode = ProtectionMode.TABLE,
    val countdownSeconds: Int = 0,
    val message: String = "Pronto",
    val lastMotionDelta: Float = 0f,
    val confidenceScore: Int = 0,
    val detectorState: String? = null,
    val ownerCheckDeadline: Long? = null,
    val journeyEndsAt: Long? = null,
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
