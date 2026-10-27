package com.riccardopinato.phoneguard.autoguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local runtime truth, independent of the persisted user opt-in. */
enum class AutoGuardRuntimePhase {
    OFF,
    WAITING,
    CHECKING,
    PAUSED
}

object AutoGuardRuntimeStore {
    private val mutable = MutableStateFlow(AutoGuardRuntimePhase.OFF)
    val state: StateFlow<AutoGuardRuntimePhase> = mutable.asStateFlow()
    fun update(next: AutoGuardRuntimePhase) { mutable.value = next }
}
