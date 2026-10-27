package com.riccardopinato.phoneguard.pocket.model

enum class GuardState {
    DISARMED,
    ARMING,
    CALIBRATING,
    WAITING_FOR_POCKET,
    POCKET_CONFIRMED,
    REMOVAL_SUSPECTED,
    OWNER_CHECK,
    ALARM
}
