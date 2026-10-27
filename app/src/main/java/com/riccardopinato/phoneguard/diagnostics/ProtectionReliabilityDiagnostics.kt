package com.riccardopinato.phoneguard.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

data class ProtectionReliabilitySnapshot(
    val manufacturer: String,
    val manufacturerAdvice: String,
    val lastExitReason: String?,
    val lastExitTimestamp: Long?,
    val systemTheftProtectionGuidance: String
)

private data class ExitSnapshot(
    val reason: String,
    val timestamp: Long
)

class ProtectionReliabilityDiagnostics(
    context: Context
) {
    private val appContext =
        context.applicationContext

    fun snapshot():
        ProtectionReliabilitySnapshot {
        val manufacturer =
            Build.MANUFACTURER
                .orEmpty()
                .ifBlank {
                    "Android"
                }

        val exitSnapshot =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {
                readExitSnapshot()
            } else {
                null
            }

        return ProtectionReliabilitySnapshot(
            manufacturer =
                manufacturer,
            manufacturerAdvice =
                manufacturerAdvice(
                    manufacturer
                ),
            lastExitReason =
                exitSnapshot
                    ?.reason,
            lastExitTimestamp =
                exitSnapshot
                    ?.timestamp,
            systemTheftProtectionGuidance =
                "Review Android Theft Protection, Remote Lock and Find Hub in system settings. These complement Phone Guard and are not controlled by this app."
        )
    }

    @RequiresApi(
        Build.VERSION_CODES.R
    )
    private fun readExitSnapshot():
        ExitSnapshot? =
        appContext
            .getSystemService(
                ActivityManager::class.java
            )
            ?.getHistoricalProcessExitReasons(
                appContext.packageName,
                0,
                1
            )
            ?.firstOrNull()
            ?.let {
                info ->
                ExitSnapshot(
                    reason =
                        exitReasonLabel(
                            info
                        ),
                    timestamp =
                        info.timestamp
                )
            }

    private fun manufacturerAdvice(
        manufacturer: String
    ): String {
        val normalized =
            manufacturer
                .lowercase(
                    Locale.ROOT
                )

        return when {
            normalized.contains(
                "xiaomi"
            ) ||
                normalized.contains(
                    "redmi"
                ) ||
                normalized.contains(
                    "poco"
                ) ->
                "Xiaomi/Redmi/Poco: verify battery optimization and background/autostart settings. OEM policy may stop foreground work."

            normalized.contains(
                "samsung"
            ) ->
                "Samsung: verify Battery > Background usage limits and exclude Phone Guard from sleeping apps when available."

            normalized.contains(
                "oneplus"
            ) ||
                normalized.contains(
                    "oppo"
                ) ||
                normalized.contains(
                    "realme"
                ) ->
                "OnePlus/Oppo/Realme: verify battery optimization and background activity controls."

            normalized.contains(
                "huawei"
            ) ||
                normalized.contains(
                    "honor"
                ) ->
                "Huawei/Honor: verify App launch/background execution controls and battery optimization."

            normalized.contains(
                "vivo"
            ) ->
                "Vivo: verify background power consumption and autostart controls."

            else ->
                "Android: keep notifications enabled and review battery optimization. OEM-specific behavior can still vary."
        }
    }

    @RequiresApi(
        Build.VERSION_CODES.R
    )
    private fun exitReasonLabel(
        info: ApplicationExitInfo
    ): String =
        when (info.reason) {
            ApplicationExitInfo.REASON_EXIT_SELF ->
                "app exited normally"

            ApplicationExitInfo.REASON_SIGNALED ->
                "process signal"

            ApplicationExitInfo.REASON_LOW_MEMORY ->
                "low memory"

            ApplicationExitInfo.REASON_CRASH ->
                "crash"

            ApplicationExitInfo.REASON_CRASH_NATIVE ->
                "native crash"

            ApplicationExitInfo.REASON_ANR ->
                "ANR"

            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE ->
                "initialization failure"

            ApplicationExitInfo.REASON_PERMISSION_CHANGE ->
                "permission change"

            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ->
                "excessive resource usage"

            ApplicationExitInfo.REASON_USER_REQUESTED ->
                "user/system requested stop"

            ApplicationExitInfo.REASON_USER_STOPPED ->
                "user force-stop"

            ApplicationExitInfo.REASON_DEPENDENCY_DIED ->
                "dependency died"

            ApplicationExitInfo.REASON_OTHER ->
                "other system reason"

            else ->
                "reason=" + info.reason
        }
}
