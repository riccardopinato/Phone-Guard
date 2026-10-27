package com.riccardopinato.phoneguard.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.accessory.AccessoryStateMonitor
import com.riccardopinato.phoneguard.charging.ChargingStateMonitor
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.SensorSnapshot
import com.riccardopinato.phoneguard.pocket.sensor.SamplingMode
import com.riccardopinato.phoneguard.pocket.sensor.SensorController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

data class LiveGuardTestResult(
    val success: Boolean,
    val detail: String,
    val observedScore: Float? = null
)

class LiveProtectionTestEngine(
    context: Context
) {
    private val appContext =
        context.applicationContext

    suspend fun run(
        mode: ProtectionMode,
        journeyGuardMode: GuardMode
    ): LiveGuardTestResult =
        when (mode) {
            ProtectionMode.TABLE ->
                testTable()

            ProtectionMode.POCKET ->
                testPocket()

            ProtectionMode.BAG ->
                testBag()

            ProtectionMode.CHARGING ->
                testCharging()

            ProtectionMode.ACCESSORY ->
                testAccessory()

            ProtectionMode.JOURNEY ->
                when (
                    journeyGuardMode
                ) {
                    GuardMode.POCKET ->
                        testPocket()

                    GuardMode.BAG ->
                        testBag()
                }
        }

    private suspend fun testTable():
        LiveGuardTestResult =
        sensorTest(
            instruction =
                "Move or lift the phone now."
        ) {
            snapshot ->
            if (
                snapshot.accelerometerObserved &&
                snapshot.motionScore >=
                    TABLE_TEST_THRESHOLD
            ) {
                LiveGuardTestResult(
                    success = true,
                    detail =
                        "Movement detected. Table Guard trigger path is responsive.",
                    observedScore =
                        snapshot.motionScore
                )
            } else {
                null
            }
        }

    private suspend fun testPocket():
        LiveGuardTestResult {
        var nearObserved =
            false

        return sensorTest(
            instruction =
                "Cover the proximity sensor, then uncover it as if removing the phone from a pocket."
        ) {
            snapshot ->
            if (
                snapshot.proximityObserved &&
                snapshot.proximityNear == true
            ) {
                nearObserved =
                    true
            }

            if (
                nearObserved &&
                snapshot.proximityObserved &&
                snapshot.proximityNear == false &&
                snapshot.accelerometerObserved
            ) {
                LiveGuardTestResult(
                    success = true,
                    detail =
                        "Pocket exit transition detected.",
                    observedScore =
                        snapshot.motionScore
                )
            } else {
                null
            }
        }
    }

    private suspend fun testBag():
        LiveGuardTestResult {
        var storedEvidence =
            false

        return sensorTest(
            instruction =
                "Place the phone in a dark/enclosed position, then remove or move it."
        ) {
            snapshot ->
            val dark =
                snapshot.lightObserved &&
                    snapshot.lux != null &&
                    snapshot.lux <=
                        BAG_DARK_LUX

            if (
                dark ||
                (
                    snapshot.gyroscopeObserved &&
                        snapshot.rotationScore <=
                            BAG_STORED_ROTATION
                    )
            ) {
                storedEvidence =
                    true
            }

            val removalEvidence =
                (
                    snapshot.lightObserved &&
                        snapshot.lux != null &&
                        snapshot.lux >=
                            BAG_EXIT_LUX
                    ) ||
                    (
                        snapshot.gyroscopeObserved &&
                            snapshot.rotationScore >=
                                BAG_EXIT_ROTATION
                        ) ||
                    (
                        snapshot.accelerometerObserved &&
                            snapshot.motionScore >=
                                BAG_EXIT_MOTION
                        )

            if (
                storedEvidence &&
                removalEvidence
            ) {
                LiveGuardTestResult(
                    success = true,
                    detail =
                        "Bag removal transition detected.",
                    observedScore =
                        maxOf(
                            snapshot.motionScore,
                            snapshot.rotationScore
                        )
                )
            } else {
                null
            }
        }
    }

    private suspend fun sensorTest(
        instruction: String,
        evaluator:
            (SensorSnapshot) ->
                LiveGuardTestResult?
    ): LiveGuardTestResult {
        val completed =
            CompletableDeferred<
                LiveGuardTestResult
                >()

        val controller =
            SensorController(
                appContext
            ) {
                snapshot ->
                evaluator(
                    snapshot
                )?.let {
                    if (
                        !completed.isCompleted
                    ) {
                        completed.complete(
                            it
                        )
                    }
                }
            }

        return try {
            controller.start(
                SamplingMode.ALERT
            )

            withTimeoutOrNull(
                TEST_TIMEOUT_MS
            ) {
                completed.await()
            } ?: LiveGuardTestResult(
                success = false,
                detail =
                    "$instruction No qualifying transition was detected before timeout."
            )
        } finally {
            controller.stop()
        }
    }

    private suspend fun testCharging():
        LiveGuardTestResult {
        val monitor =
            ChargingStateMonitor(
                appContext
            )

        if (
            !monitor
                .isPowerConnected()
        ) {
            return LiveGuardTestResult(
                success = false,
                detail =
                    "Connect external power before starting the Charging Guard test."
            )
        }

        val completed =
            CompletableDeferred<
                LiveGuardTestResult
                >()

        val receiver =
            object :
                BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?
                ) {
                    if (
                        intent?.action ==
                        Intent.ACTION_POWER_DISCONNECTED &&
                        !completed.isCompleted
                    ) {
                        completed.complete(
                            LiveGuardTestResult(
                                success = true,
                                detail =
                                    "Power disconnect detected. Charging Guard trigger path is responsive."
                            )
                        )
                    }
                }
            }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(
                Intent.ACTION_POWER_DISCONNECTED
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        return try {
            withTimeoutOrNull(
                TEST_TIMEOUT_MS
            ) {
                completed.await()
            } ?: LiveGuardTestResult(
                success = false,
                detail =
                    "No charger disconnect was detected before timeout."
            )
        } finally {
            runCatching {
                appContext.unregisterReceiver(
                    receiver
                )
            }
        }
    }

    private suspend fun testAccessory():
        LiveGuardTestResult {
        val completed =
            CompletableDeferred<
                LiveGuardTestResult
                >()

        val monitor =
            AccessoryStateMonitor(
                appContext
            )

        val protected =
            monitor.start {
                removed ->
                if (
                    !completed.isCompleted
                ) {
                    completed.complete(
                        LiveGuardTestResult(
                            success = true,
                            detail =
                                "Accessory disconnect detected: " +
                                    removed
                                        .joinToString {
                                            it.name
                                        }
                        )
                    )
                }
            }

        if (
            protected.isEmpty()
        ) {
            monitor.stop()

            return LiveGuardTestResult(
                success = false,
                detail =
                    "Connect wired, USB or Bluetooth audio before starting the Accessory Guard test."
            )
        }

        return try {
            withTimeoutOrNull(
                TEST_TIMEOUT_MS
            ) {
                completed.await()
            } ?: LiveGuardTestResult(
                success = false,
                detail =
                    "No protected accessory disconnect was detected before timeout."
            )
        } finally {
            monitor.stop()
        }
    }

    private companion object {
        const val TEST_TIMEOUT_MS =
            12_000L

        const val TABLE_TEST_THRESHOLD =
            0.18f

        const val BAG_DARK_LUX =
            8f

        const val BAG_EXIT_LUX =
            18f

        const val BAG_STORED_ROTATION =
            0.08f

        const val BAG_EXIT_ROTATION =
            0.20f

        const val BAG_EXIT_MOTION =
            0.16f
    }
}
