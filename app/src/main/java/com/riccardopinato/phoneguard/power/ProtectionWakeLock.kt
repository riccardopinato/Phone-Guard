package com.riccardopinato.phoneguard.power

import android.content.Context
import android.os.PowerManager
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object ProtectionWakePolicy {

    fun requiresCpuLease(
        mode: ProtectionMode,
        guardMode: GuardMode?,
        capabilities: SensorCapabilities
    ): Boolean =
        when (mode) {
            ProtectionMode.TABLE ->
                !capabilities.accelerometerWakeUp

            ProtectionMode.POCKET ->
                !pocketCanWake(
                    capabilities
                )

            ProtectionMode.BAG ->
                !bagCanWake(
                    capabilities
                )

            ProtectionMode.JOURNEY ->
                when (
                    guardMode
                        ?: GuardMode.POCKET
                ) {
                    GuardMode.POCKET ->
                        !pocketCanWake(
                            capabilities
                        )

                    GuardMode.BAG ->
                        !bagCanWake(
                            capabilities
                        )
                }
        }

    private fun pocketCanWake(
        capabilities: SensorCapabilities
    ): Boolean =
        capabilities.hasAccelerometer &&
            capabilities.accelerometerWakeUp &&
            capabilities.hasProximity &&
            capabilities.proximityWakeUp

    private fun bagCanWake(
        capabilities: SensorCapabilities
    ): Boolean {
        val wakeAuxiliary =
            (
                capabilities.hasLight &&
                    capabilities.lightWakeUp
                ) ||
                (
                    capabilities.hasGyroscope &&
                        capabilities.gyroscopeWakeUp
                    )

        return capabilities.hasAccelerometer &&
            capabilities.accelerometerWakeUp &&
            wakeAuxiliary
    }
}

class ProtectionWakeLock(
    context: Context,
    private val scope: CoroutineScope
) : AutoCloseable {

    private val wakeLock =
        (
            context.applicationContext
                .getSystemService(
                    Context.POWER_SERVICE
                ) as PowerManager
            )
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PhoneGuard:ProtectionMonitoring"
            )
            .apply {
                setReferenceCounted(false)
            }

    private var required = false
    private var refreshJob: Job? = null
    private var closed = false

    @Synchronized
    fun forceRequired() {
        setRequired(true)
    }

    @Synchronized
    fun setRequired(value: Boolean) {
        if (closed) return

        required = value

        if (!value) {
            stopRefresh()
            releaseInternal()
            return
        }

        acquireBounded()

        if (
            refreshJob?.isActive !=
            true
        ) {
            refreshJob =
                scope.launch {
                    while (isActive) {
                        delay(
                            REFRESH_INTERVAL_MS
                        )

                        synchronized(
                            this@ProtectionWakeLock
                        ) {
                            if (
                                required &&
                                !closed
                            ) {
                                acquireBounded()
                            }
                        }
                    }
                }
        }
    }

    @Synchronized
    fun release() {
        required = false
        stopRefresh()
        releaseInternal()
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        required = false
        stopRefresh()
        releaseInternal()
    }

    private fun acquireBounded() {
        if (wakeLock.isHeld) {
            runCatching {
                wakeLock.release()
            }
        }

        runCatching {
            wakeLock.acquire(
                LEASE_TIMEOUT_MS
            )
        }
    }

    private fun releaseInternal() {
        if (wakeLock.isHeld) {
            runCatching {
                wakeLock.release()
            }
        }
    }

    private fun stopRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private companion object {
        const val LEASE_TIMEOUT_MS =
            15L * 60L * 1_000L

        const val REFRESH_INTERVAL_MS =
            10L * 60L * 1_000L
    }
}
