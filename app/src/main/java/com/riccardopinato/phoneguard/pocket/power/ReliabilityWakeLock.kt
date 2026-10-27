package com.riccardopinato.phoneguard.pocket.power

import android.content.Context
import android.os.PowerManager

class ReliabilityWakeLock(
    context: Context
) {
    private val wakeLock =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PocketGuard:ProtectedSession"
            )
            .apply {
                setReferenceCounted(false)
            }

    fun acquire() {
        if (!wakeLock.isHeld) wakeLock.acquire()
    }

    fun release() {
        if (wakeLock.isHeld) wakeLock.release()
    }
}
