package com.riccardopinato.phoneguard.pocket.model

enum class JourneyDuration(val durationMs: Long?) {
    MINUTES_30(30L * 60L * 1000L),
    HOUR_1(60L * 60L * 1000L),
    HOURS_2(2L * 60L * 60L * 1000L),
    UNTIL_STOPPED(null)
}
