package com.riccardopinato.phoneguard.autoguard

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/** In-memory only: no position coordinates, routes, history or raw activity traces. */
object MobilityContextStore {
    @Volatile var enabled: Boolean = false
    @Volatile private var current: MobilityType = MobilityType.UNKNOWN
    @Volatile private var observedAt: Long = 0L

    fun record(type: MobilityType, entered: Boolean, elapsedMs: Long) {
        if (!enabled) return
        if (entered) {
            current = type
            observedAt = elapsedMs
        } else if (current == type) {
            current = MobilityType.UNKNOWN
            observedAt = elapsedMs
        }
    }

    fun freshType(nowElapsedMs: Long): MobilityType =
        if (enabled && nowElapsedMs >= observedAt &&
            nowElapsedMs - observedAt <= 5 * 60_000L
        ) current else MobilityType.UNKNOWN

    fun clear() {
        enabled = false
        current = MobilityType.UNKNOWN
        observedAt = 0L
    }
}

/** Receives only explicit Google Play services transition PendingIntent deliveries. */
class MobilityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !MobilityContextStore.enabled) return
        val result = runCatching {
            ActivityTransitionResult.extractResult(intent)
        }.getOrNull() ?: return
        result.transitionEvents.forEach { event ->
            val type = when (event.activityType) {
                DetectedActivity.WALKING, DetectedActivity.ON_FOOT ->
                    MobilityType.WALKING
                DetectedActivity.RUNNING -> MobilityType.RUNNING
                DetectedActivity.ON_BICYCLE -> MobilityType.BICYCLE
                DetectedActivity.IN_VEHICLE -> MobilityType.VEHICLE
                DetectedActivity.STILL -> MobilityType.STILL
                else -> MobilityType.UNKNOWN
            }
            if (type != MobilityType.UNKNOWN) {
                MobilityContextStore.record(
                    type,
                    event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                    SystemClock.elapsedRealtime()
                )
            }
        }
    }
}

/**
 * Optional supporting evidence, never the sole reason for arming.
 * If Google Play services or the permission is unavailable, the main sensor flow works.
 */
class MobilityContextMonitor(context: Context) {
    private val appContext = context.applicationContext
    private var generation = 0
    private var subscribed = false
    private var requestAttempted = false

    private val callback: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            appContext,
            5071,
            Intent(appContext, MobilityTransitionReceiver::class.java)
                .setAction("com.riccardopinato.phoneguard.MOBILITY_TRANSITION"),
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                })
        )
    }

    private fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

    fun start(): Boolean {
        if (subscribed || requestAttempted) return true
        if (!hasActivityPermission()) return false
        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(appContext) != ConnectionResult.SUCCESS
        ) return false

        val transitions = listOf(
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.STILL
        ).flatMap { type ->
            listOf(
                ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                ActivityTransition.ACTIVITY_TRANSITION_EXIT
            ).map { transition ->
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(transition)
                    .build()
            }
        }

        val token = ++generation
        MobilityContextStore.enabled = true
        requestAttempted = true
        return runCatching {
            ActivityRecognition.getClient(appContext)
                .requestActivityTransitionUpdates(
                    ActivityTransitionRequest(transitions), callback
                )
                .addOnSuccessListener {
                    if (token == generation && MobilityContextStore.enabled) {
                        subscribed = true
                    } else if (hasActivityPermission()) {
                        try {
                            ActivityRecognition.getClient(appContext)
                                .removeActivityTransitionUpdates(callback)
                        } catch (_: SecurityException) {
                            // Permission revoked while the request was pending.
                        }
                    }
                }
                .addOnFailureListener {
                    if (token == generation) {
                        subscribed = false
                        requestAttempted = false
                        MobilityContextStore.clear()
                    }
                }
            true
        }.getOrElse {
            requestAttempted = false
            MobilityContextStore.clear()
            false
        }
    }

    fun stop() {
        generation++
        MobilityContextStore.clear()
        subscribed = false
        if (!requestAttempted) return
        requestAttempted = false
        if (!hasActivityPermission()) return
        try {
            ActivityRecognition.getClient(appContext)
                .removeActivityTransitionUpdates(callback)
        } catch (_: SecurityException) {
            // Revoked during shutdown; local sensor fallback remains unaffected.
        }
    }
}
