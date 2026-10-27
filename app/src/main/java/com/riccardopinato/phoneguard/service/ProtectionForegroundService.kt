package com.riccardopinato.phoneguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.activation.ActivationOutcome
import com.riccardopinato.phoneguard.activation.ActivationSource
import com.riccardopinato.phoneguard.activation.ProtectionActivationCoordinator
import com.riccardopinato.phoneguard.accessory.AccessoryStateMonitor
import com.riccardopinato.phoneguard.charging.ChargingStateMonitor
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.RecoveryAction
import com.riccardopinato.phoneguard.data.SessionRecoveryPolicy
import com.riccardopinato.phoneguard.model.AlarmEventEvidence
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.DetectionDecision
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
import com.riccardopinato.phoneguard.power.ProtectionWakeLock
import com.riccardopinato.phoneguard.quick.PhoneGuardTileService
import com.riccardopinato.phoneguard.power.ProtectionWakePolicy
import com.riccardopinato.phoneguard.protection.PendingTriggerOwnerAction
import com.riccardopinato.phoneguard.protection.PocketProtectionController
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.sensor.MotionDetector
import com.riccardopinato.phoneguard.sensor.MotionTrigger
import com.riccardopinato.phoneguard.widget.PhoneGuardWidgetProvider
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class PendingAlarmTrigger(
    val detail: String,
    val lastMotionDelta: Float,
    val confidence: Int,
    val detectorState: String,
    val sensorSummary: String? = null
)

class ProtectionForegroundService : Service() {

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        )

    private lateinit var outputController:
        AlarmOutputController

    private lateinit var notificationManager:
        NotificationManager

    private lateinit var pocketController:
        PocketProtectionController

    private lateinit var chargingMonitor:
        ChargingStateMonitor

    private lateinit var accessoryMonitor:
        AccessoryStateMonitor

    private val container by lazy {
        (application as PhoneGuardApp)
            .container
    }

    private var armingJob: Job? = null
    private var ownerCheckJob: Job? = null
    private var journeyJob: Job? = null
    private var alarmOutputTimeoutJob: Job? = null

    private var pendingAlarmTrigger:
        PendingAlarmTrigger? = null

    private var tableDetector:
        MotionDetector? = null

    private lateinit var protectionWakeLock:
        ProtectionWakeLock

    private var activeSettings:
        AppSettings? = null

    private var activeActivationSource:
        ActivationSource =
        ActivationSource.APP

    private var autoArmStandbyEnabled =
        false

    private var autoArmAwaitingFreshConnection =
        false

    private var activeMode:
        ProtectionMode = ProtectionMode.TABLE

    private var activeGuardMode:
        GuardMode? = null

    private var activeJourneyEndAt:
        Long? = null

    private var activeJourneyEndAtElapsedRealtime:
        Long? = null

    private val powerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        if (
                            autoArmStandbyEnabled &&
                            AlarmStateStore.state
                                .value.phase ==
                                AlarmPhase.DISARMED
                        ) {
                            autoArmAwaitingFreshConnection =
                                false
                        }

                        if (
                            activeMode ==
                                ProtectionMode.CHARGING &&
                            AlarmStateStore.state
                                .value.phase ==
                                AlarmPhase.ARMED
                        ) {
                            requestAlarm(
                                detail =
                                    "Mode=CHARGING; external power disconnected.",
                                lastMotionDelta = 0f,
                                confidence = 100,
                                detectorState =
                                    "POWER_DISCONNECTED",
                                sensorSummary =
                                    "externalPower=false"
                            )
                        }
                    }

                    Intent.ACTION_POWER_CONNECTED -> {
                        if (
                            !autoArmStandbyEnabled ||
                            autoArmAwaitingFreshConnection ||
                            AlarmStateStore.state
                                .value.phase !=
                                AlarmPhase.DISARMED
                        ) {
                            return
                        }

                        autoArmAwaitingFreshConnection =
                            true

                        serviceScope.launch {
                            when (
                                ProtectionActivationCoordinator
                                    .evaluate(
                                        applicationContext,
                                        ProtectionMode.CHARGING
                                    )
                            ) {
                                ActivationOutcome.Started ->
                                    beginArming(
                                        ProtectionMode.CHARGING,
                                        ActivationSource.AUTO_CHARGER
                                    )

                                is ActivationOutcome.Blocked ->
                                    Unit
                            }
                        }
                    }
                }
            }
        }

    private val ownerReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action ==
                    Intent.ACTION_USER_PRESENT
                ) {
                    ownerPresenceObserved()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        outputController =
            AlarmOutputController(
                context =
                    applicationContext,
                volumeLease =
                    container
                        .alarmVolumeLeaseManager
            )

        notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        pocketController =
            PocketProtectionController(
                context = applicationContext,
                scope = serviceScope,
                onDecision = ::handlePocketDecision
            )

        chargingMonitor =
            ChargingStateMonitor(
                applicationContext
            )

        accessoryMonitor =
            AccessoryStateMonitor(
                applicationContext
            )

        protectionWakeLock =
            ProtectionWakeLock(
                context = applicationContext,
                scope = serviceScope
            )

        ContextCompat.registerReceiver(
            this,
            ownerReceiver,
            IntentFilter(
                Intent.ACTION_USER_PRESENT
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            this,
            powerReceiver,
            IntentFilter().apply {
                addAction(
                    Intent.ACTION_POWER_CONNECTED
                )
                addAction(
                    Intent.ACTION_POWER_DISCONNECTED
                )
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        createNotificationChannels()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_ARM -> {
                val mode =
                    intent.getStringExtra(
                        EXTRA_MODE
                    )?.let {
                        runCatching {
                            ProtectionMode.valueOf(it)
                        }.getOrNull()
                    } ?: ProtectionMode.TABLE

                val source =
                    intent.getStringExtra(
                        EXTRA_SOURCE
                    )?.let {
                        runCatching {
                            ActivationSource.valueOf(it)
                        }.getOrNull()
                    } ?: ActivationSource.APP

                beginArming(
                    mode,
                    source
                )
            }

            ACTION_SET_AUTO_ARM -> {
                val enabled =
                    intent.getBooleanExtra(
                        EXTRA_AUTO_ARM,
                        false
                    )

                autoArmStandbyEnabled =
                    enabled

                if (
                    AlarmStateStore.state
                        .value.phase ==
                    AlarmPhase.DISARMED
                ) {
                    if (enabled) {
                        enterChargingAutoArmStandby()
                    } else {
                        ServiceCompat.stopForeground(
                            this,
                            ServiceCompat
                                .STOP_FOREGROUND_REMOVE
                        )
                        stopSelf()
                    }
                }
            }

            ACTION_DISARM ->
                disarm()

            ACTION_OWNER_VERIFIED ->
                ownerVerified(
                    "Authenticated in app"
                )

            ACTION_RESTORE ->
                restorePersistedSession()

            null -> {
                startAsForeground(
                    phase = AlarmPhase.DISARMED,
                    title =
                        getString(
                            R.string.svc_auto_arm_title
                        ),
                    text =
                        getString(
                            R.string.svc_auto_arm_checking
                        )
                )
                restorePersistedSession()
            }
        }

        return START_STICKY
    }

    private fun beginArming(
        mode: ProtectionMode,
        source: ActivationSource =
            ActivationSource.APP
    ) {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return
        }

        activeMode = mode
        activeActivationSource =
            source

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMING,
                mode = mode,
                message =
                    getString(R.string.svc_preparing, mode.label)
            )
        )

        container.sessionStateRepository.saveState(
            mode = mode,
            phase = AlarmPhase.ARMING
        )

        refreshQuickSurfaces()

        startAsForeground(
            phase = AlarmPhase.ARMING,
            title = getString(R.string.svc_preparing_title, mode.label),
            text = armingInstruction(mode)
        )

        protectionWakeLock.forceRequired()

        armingJob?.cancel()

        armingJob =
            serviceScope.launch {
                val settings =
                    container
                        .settingsRepository
                        .settings
                        .first()

                activeSettings = settings
                autoArmStandbyEnabled =
                    settings.autoArmCharging

                container.eventRepository.add(
                    AlarmEventType.ARMING_STARTED,
                    "Mode=${mode.name}; countdown=" +
                        "${settings.countdownSeconds}s",
                    evidence =
                        AlarmEventEvidence(
                            mode = mode,
                            source = source.name,
                            graceSeconds =
                                settings
                                    .gracePeriodSeconds
                        )
                )

                for (
                    remaining in
                    settings.countdownSeconds
                        downTo 1
                ) {
                    if (
                        AlarmStateStore.state
                            .value.phase !=
                        AlarmPhase.ARMING
                    ) {
                        return@launch
                    }

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.ARMING,
                            mode = mode,
                            countdownSeconds = remaining,
                            message =
                                getString(R.string.svc_activating, mode.label, remaining)
                        )
                    )

                    updateNotification(
                        AlarmPhase.ARMING,
                        getString(R.string.svc_preparing_title, mode.label),
                        getString(R.string.svc_activating_short, remaining)
                    )

                    delay(1_000)
                }

                if (
                    AlarmStateStore.state
                        .value.phase !=
                    AlarmPhase.ARMING
                ) {
                    return@launch
                }

                beginCalibration(
                    settings = settings,
                    mode = mode
                )
            }
    }

    private fun beginCalibration(
        settings: AppSettings,
        mode: ProtectionMode
    ) {
        activeMode = mode

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.CALIBRATING,
                mode = mode,
                message =
                    calibrationMessage(mode)
            )
        )

        refreshQuickSurfaces()

        val guardMode =
            ProtectionPolicy.effectiveGuardMode(
                mode,
                settings.journeyGuardMode
            )

        activeGuardMode = guardMode

        container.sessionStateRepository.saveState(
            mode = mode,
            phase = AlarmPhase.CALIBRATING,
            pocketGuardMode = guardMode
        )

        updateNotification(
            AlarmPhase.CALIBRATING,
            getString(R.string.svc_calibrating_title, mode.label),
            calibrationMessage(mode)
        )

        if (mode == ProtectionMode.TABLE) {
            startTableMonitoring(
                settings = settings
            )
            return
        }

        if (
            mode ==
            ProtectionMode.CHARGING
        ) {
            startChargingMonitoring(
                settings = settings,
                recordArmedEvent = true
            )
            return
        }

        if (
            mode ==
            ProtectionMode.ACCESSORY
        ) {
            startAccessoryMonitoring(
                settings = settings,
                recordArmedEvent = true
            )
            return
        }

        val requiredGuardMode =
            guardMode ?: run {
                failAndStop(
                    getString(R.string.svc_invalid_profile)
                )
                return
            }

        pocketController.calibrate(
            mode = requiredGuardMode,
            onComplete = { profile ->
                container
                    .sessionStateRepository
                    .savePocketCalibration(
                        profile
                    )

                startPocketMonitoring(
                    settings = settings,
                    mode = mode,
                    profile = profile,
                    recordArmedEvent = true,
                    journeyEndAt = null,
                    guardModeOverride = requiredGuardMode
                )
            },
            onError = ::failAndStop
        )
    }

    private fun startChargingMonitoring(
        settings: AppSettings,
        recordArmedEvent: Boolean
    ) {
        tableDetector?.stop()
        tableDetector = null
        pocketController.stop()

        activeMode =
            ProtectionMode.CHARGING
        activeGuardMode = null
        activeJourneyEndAt = null
        activeJourneyEndAtElapsedRealtime = null

        if (
            !chargingMonitor
                .isPowerConnected()
        ) {
            if (!recordArmedEvent) {
                triggerAlarm(
                    detail =
                        "Mode=CHARGING; power was removed while protection was being recovered.",
                    lastMotionDelta = 0f,
                    confidence = 100,
                    detectorState =
                        "POWER_DISCONNECTED_RECOVERY",
                    sensorSummary =
                        "externalPower=false"
                )
            } else {
                failAndStop(
                    getString(
                        R.string
                            .svc_charging_requires_power
                    )
                )
            }
            return
        }

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.CHARGING,
                message =
                    getString(
                        R.string
                            .svc_charging_protected
                    ),
                detectorState =
                    "POWER_CONNECTED"
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository
            .saveState(
                mode =
                    ProtectionMode.CHARGING,
                phase = AlarmPhase.ARMED
            )

        if (recordArmedEvent) {
            container.eventRepository.add(
                AlarmEventType.ARMED,
                "Mode=CHARGING; external power connected.",
                evidence =
                    AlarmEventEvidence(
                        mode =
                            ProtectionMode.CHARGING,
                        source =
                            activeActivationSource.name,
                        detectorState =
                            "POWER_CONNECTED",
                        sensorSummary =
                            "externalPower=true",
                        graceSeconds =
                            settings
                                .gracePeriodSeconds
                    )
            )
        }

        updateNotification(
            AlarmPhase.ARMED,
            getString(
                R.string
                    .svc_charging_title
            ),
            getString(
                R.string
                    .svc_charging_alarm_hint
            )
        )

        protectionWakeLock
            .setRequired(false)
    }

    private fun startAccessoryMonitoring(
        settings: AppSettings,
        recordArmedEvent: Boolean,
        expectedFingerprints:
            Set<String>? = null
    ) {
        tableDetector?.stop()
        tableDetector = null
        pocketController.stop()
        accessoryMonitor.stop()

        activeMode =
            ProtectionMode.ACCESSORY
        activeGuardMode = null
        activeJourneyEndAt = null
        activeJourneyEndAtElapsedRealtime = null

        val protected =
            accessoryMonitor.start {
                removed ->
                if (
                    activeMode ==
                        ProtectionMode.ACCESSORY &&
                    AlarmStateStore.state
                        .value.phase ==
                        AlarmPhase.ARMED
                ) {
                    val summary =
                        removed
                            .joinToString(
                                separator = ", "
                            ) {
                                it.name
                            }

                    requestAlarm(
                        detail =
                            "Mode=ACCESSORY; protected audio accessory disconnected: " +
                                summary,
                        lastMotionDelta = 0f,
                        confidence = 100,
                        detectorState =
                            "ACCESSORY_DISCONNECTED",
                        sensorSummary =
                            "removed=" +
                                summary
                    )
                }
            }

        if (
            protected.isEmpty()
        ) {
            accessoryMonitor.stop()

            if (
                !recordArmedEvent
            ) {
                triggerAlarm(
                    detail =
                        "Mode=ACCESSORY; protected accessory was absent while protection was being recovered.",
                    lastMotionDelta = 0f,
                    confidence = 100,
                    detectorState =
                        "ACCESSORY_DISCONNECTED_RECOVERY",
                    sensorSummary =
                        "accessoryConnected=false"
                )
            } else {
                failAndStop(
                    getString(
                        R.string
                            .svc_accessory_requires_device
                    )
                )
            }
            return
        }

        val currentFingerprints =
            protected
                .map {
                    it.stableFingerprint()
                }
                .toSet()

        if (
            expectedFingerprints !=
                null &&
            expectedFingerprints
                .isNotEmpty() &&
            !currentFingerprints
                .containsAll(
                    expectedFingerprints
                )
        ) {
            accessoryMonitor.stop()

            triggerAlarm(
                detail =
                    "Mode=ACCESSORY; original protected accessory set is no longer present during recovery.",
                lastMotionDelta = 0f,
                confidence = 100,
                detectorState =
                    "ACCESSORY_IDENTITY_MISMATCH_RECOVERY",
                sensorSummary =
                    "expected=" +
                        expectedFingerprints
                            .sorted()
                            .joinToString() +
                        "; current=" +
                        currentFingerprints
                            .sorted()
                            .joinToString()
            )
            return
        }

        val summary =
            protected
                .joinToString(
                    separator = ", "
                ) {
                    it.name
                }

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode =
                    ProtectionMode.ACCESSORY,
                message =
                    getString(
                        R.string
                            .svc_accessory_protected
                    ),
                detectorState =
                    "ACCESSORY_CONNECTED"
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository
            .saveState(
                mode =
                    ProtectionMode.ACCESSORY,
                phase =
                    AlarmPhase.ARMED
            )

        container.sessionStateRepository
            .saveAccessoryFingerprints(
                currentFingerprints
            )

        if (
            recordArmedEvent
        ) {
            container.eventRepository.add(
                AlarmEventType.ARMED,
                "Mode=ACCESSORY; protected=$summary",
                evidence =
                    AlarmEventEvidence(
                        mode =
                            ProtectionMode.ACCESSORY,
                        source =
                            activeActivationSource.name,
                        detectorState =
                            "ACCESSORY_CONNECTED",
                        sensorSummary =
                            "protected=" +
                                summary,
                        graceSeconds =
                            settings
                                .gracePeriodSeconds
                    )
            )
        }

        updateNotification(
            AlarmPhase.ARMED,
            getString(
                R.string
                    .svc_accessory_title
            ),
            getString(
                R.string
                    .svc_accessory_alarm_hint
            )
        )

        protectionWakeLock
            .setRequired(false)
    }

    private fun startTableMonitoring(
        settings: AppSettings,
        initialCalibration:
            MotionCalibration? = null,
        recordArmedEvent:
            Boolean = true
    ) {
        tableDetector?.stop()
        pocketController.stop()

        val detector =
            MotionDetector(
                context =
                    applicationContext,
                sensitivity =
                    settings.sensitivity,
                initialCalibration =
                    initialCalibration,
                onCalibrated = {
                    calibration ->

                    activeMode =
                        ProtectionMode.TABLE
                    activeGuardMode = null
                    activeJourneyEndAt = null
                    activeJourneyEndAtElapsedRealtime = null

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.ARMED,
                            mode = ProtectionMode.TABLE,
                            message =
                                getString(R.string.svc_table_protected)
                        )
                    )

                    refreshQuickSurfaces()

                    container.sessionStateRepository.saveState(
                        mode = ProtectionMode.TABLE,
                        phase = AlarmPhase.ARMED
                    )

                    container.sessionStateRepository
                        .saveTableCalibration(
                            calibration
                        )

                    if (recordArmedEvent) {
                        container
                            .eventRepository
                            .add(
                                AlarmEventType.ARMED,
                                "Mode=TABLE; noise floor " +
                                    String.format(
                                        Locale.US,
                                        "%.3f m/s²",
                                        calibration.noiseFloor
                                    ),
                                evidence =
                                    AlarmEventEvidence(
                                        mode =
                                            ProtectionMode.TABLE,
                                        source =
                                            activeActivationSource.name,
                                        detectorState =
                                            "TABLE_MONITORING",
                                        sensorSummary =
                                            "noiseFloor=" +
                                                String.format(
                                                    Locale.US,
                                                    "%.3f",
                                                    calibration.noiseFloor
                                                ),
                                        graceSeconds =
                                            settings
                                                .gracePeriodSeconds
                                    )
                            )
                    }

                    updateNotification(
                        AlarmPhase.ARMED,
                        getString(R.string.svc_table_title),
                        getString(R.string.svc_table_alarm_hint)
                    )

                    updateMonitoringWakeLock(
                        mode = ProtectionMode.TABLE,
                        guardMode = null
                    )
                },
                onMovement = {
                    trigger ->
                    triggerTableAlarm(trigger)
                }
            )

        tableDetector = detector

        if (!detector.start()) {
            failAndStop(
                getString(R.string.svc_accel_unavailable)
            )
        }
    }

    private fun startPocketMonitoring(
        settings: AppSettings,
        mode: ProtectionMode,
        profile: CalibrationProfile,
        recordArmedEvent: Boolean,
        journeyEndAt: Long?,
        journeyEndAtElapsedRealtime: Long? = null,
        guardModeOverride: GuardMode? = null
    ) {
        tableDetector?.stop()
        tableDetector = null

        val guardMode =
            guardModeOverride
                ?: ProtectionPolicy.effectiveGuardMode(
                    mode,
                    settings.journeyGuardMode
                )
                ?: run {
                    failAndStop(
                        getString(R.string.svc_invalid_profile)
                    )
                    return
                }

        activeMode = mode
        activeGuardMode = guardMode

        val guardSettings =
            GuardSettings(
                mode = guardMode,
                sensitivity =
                    settings.pocketSensitivity,
                pocketStableMs = 1_200L,
                removalWindowMs = 1_600L
            )

        if (
            !pocketController.start(
                settings = guardSettings,
                profile = profile
            )
        ) {
            failAndStop(
                getString(R.string.svc_sensors_insufficient, guardMode.name)
            )
            return
        }

        updateMonitoringWakeLock(
            mode = mode,
            guardMode = guardMode
        )

        val endAt =
            if (mode == ProtectionMode.JOURNEY) {
                journeyEndAt
                    ?: ProtectionPolicy.journeyEndAt(
                        System.currentTimeMillis(),
                        settings.journeyDuration
                    )
            } else {
                null
            }

        val endAtElapsedRealtime =
            if (mode == ProtectionMode.JOURNEY) {
                journeyEndAtElapsedRealtime
                    ?: ProtectionPolicy.journeyEndAt(
                        SystemClock.elapsedRealtime(),
                        settings.journeyDuration
                    )
            } else {
                null
            }

        activeJourneyEndAt = endAt
        activeJourneyEndAtElapsedRealtime =
            endAtElapsedRealtime

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = mode,
                message =
                    waitingMessage(
                        mode,
                        guardMode
                    ),
                detectorState =
                    GuardState
                        .WAITING_FOR_POCKET
                        .name,
                journeyEndsAt = endAt
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository.saveState(
            mode = mode,
            phase = AlarmPhase.ARMED,
            pocketGuardMode = guardMode,
            journeyEndsAt = endAt,
            journeyEndsAtElapsedRealtime =
                endAtElapsedRealtime
        )

        container.sessionStateRepository
            .savePocketCalibration(
                profile
            )

        if (recordArmedEvent) {
            container.eventRepository.add(
                AlarmEventType.ARMED,
                "Mode=${mode.name}; guard=${guardMode.name}; " +
                    "samples=${profile.sampleCount}",
                evidence =
                    AlarmEventEvidence(
                        mode = mode,
                        source =
                            activeActivationSource.name,
                        detectorState =
                            "WAITING_FOR_${guardMode.name}",
                        sensorSummary =
                            "samples=${profile.sampleCount}; " +
                                "light=${profile.hasLightData}; " +
                                "gyro=${profile.hasGyroscopeData}",
                        graceSeconds =
                            settings
                                .gracePeriodSeconds
                    )
            )
        }

        updateNotification(
            AlarmPhase.ARMED,
            getString(R.string.svc_mode_protected_title, mode.label),
            waitingMessage(
                mode,
                guardMode
            )
        )

        scheduleJourneyEnd(
            endAt = endAt,
            endAtElapsedRealtime =
                endAtElapsedRealtime
        )
    }

    private fun handlePocketDecision(
        decision: DetectionDecision
    ) {
        val guardMode =
            activeGuardMode ?: return

        when (decision.state) {
            GuardState.WAITING_FOR_POCKET,
            GuardState.POCKET_CONFIRMED,
            GuardState.REMOVAL_SUSPECTED -> {
                val message =
                    when (decision.state) {
                        GuardState.WAITING_FOR_POCKET ->
                            waitingMessage(
                                activeMode,
                                guardMode
                            )

                        GuardState.POCKET_CONFIRMED ->
                            getString(R.string.svc_position_protected, activeMode.label)

                        GuardState.REMOVAL_SUSPECTED ->
                            getString(R.string.svc_possible_removal)

                        else -> ""
                    }

                AlarmStateStore.update(
                    AlarmRuntimeState(
                        phase = AlarmPhase.ARMED,
                        mode = activeMode,
                        message = message,
                        confidenceScore =
                            decision.confidenceScore,
                        detectorState =
                            decision.state.name,
                        journeyEndsAt =
                            activeJourneyEndAt
                    )
                )

                if (decision.changed) {
                    updateNotification(
                        AlarmPhase.ARMED,
                        getString(R.string.svc_mode_protected_title, activeMode.label),
                        message
                    )
                }
            }

            GuardState.OWNER_CHECK -> {
                beginOwnerCheck(
                    decision
                )
            }

            GuardState.ALARM -> {
                triggerPocketAlarm(
                    reason = decision.reason,
                    confidence =
                        decision.confidenceScore
                )
            }

            GuardState.DISARMED,
            GuardState.ARMING,
            GuardState.CALIBRATING ->
                Unit
        }
    }

    private fun beginOwnerCheck(
        decision: DetectionDecision
    ) {
        pendingAlarmTrigger = null
        protectionWakeLock.forceRequired()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        journeyJob = null

        val deadline =
            System.currentTimeMillis() +
                ownerGraceMs()

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.OWNER_CHECK,
                mode = activeMode,
                message =
                    getString(R.string.svc_owner_check_message),
                confidenceScore =
                    decision.confidenceScore,
                detectorState =
                    decision.state.name,
                ownerCheckDeadline =
                    deadline,
                journeyEndsAt =
                    activeJourneyEndAt
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository.saveState(
            mode = activeMode,
            phase = AlarmPhase.OWNER_CHECK,
            pocketGuardMode = activeGuardMode,
            journeyEndsAt = activeJourneyEndAt,
            journeyEndsAtElapsedRealtime =
                activeJourneyEndAtElapsedRealtime
        )

        startAsForeground(
            phase = AlarmPhase.OWNER_CHECK,
            title = getString(R.string.svc_owner_check_title),
            text =
                getString(R.string.svc_owner_check_notification)
        )

        startConfiguredPreAlarm()
        scheduleOwnerCheckExpiry()
    }

    private fun beginOwnerCheck(
        trigger: PendingAlarmTrigger
    ) {
        pendingAlarmTrigger =
            trigger
        protectionWakeLock.forceRequired()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        journeyJob = null

        val deadline =
            System.currentTimeMillis() +
                ownerGraceMs()

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.OWNER_CHECK,
                mode = activeMode,
                message =
                    getString(
                        R.string
                            .svc_owner_check_message
                    ),
                confidenceScore =
                    trigger.confidence,
                detectorState =
                    trigger.detectorState,
                ownerCheckDeadline =
                    deadline,
                journeyEndsAt =
                    activeJourneyEndAt
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository
            .saveState(
                mode = activeMode,
                phase =
                    AlarmPhase.OWNER_CHECK,
                pocketGuardMode =
                    activeGuardMode,
                journeyEndsAt =
                    activeJourneyEndAt,
                journeyEndsAtElapsedRealtime =
                    activeJourneyEndAtElapsedRealtime
            )

        startAsForeground(
            phase = AlarmPhase.OWNER_CHECK,
            title =
                getString(
                    R.string
                        .svc_owner_check_title
                ),
            text =
                getString(
                    R.string
                        .svc_owner_check_notification
                )
        )

        startConfiguredPreAlarm()
        scheduleOwnerCheckExpiry()
    }

    private fun ownerGraceMs(): Long =
        (
            activeSettings
                ?.gracePeriodSeconds
                ?: 5
            )
            .toLong() *
            1_000L

    private fun startConfiguredPreAlarm() {
        val settings =
            activeSettings
                ?: return

        val profile =
            settings.alarmProfile(
                activeMode
            )

        outputController.startPreAlarm(
            profile.preAlarmStyle
        )

        container.eventRepository.add(
            AlarmEventType.PRE_ALARM_STARTED,
            "Mode=${activeMode.name}; style=${profile.preAlarmStyle.name}",
            evidence =
                AlarmEventEvidence(
                    mode = activeMode,
                    source =
                        activeActivationSource.name,
                    detectorState =
                        "OWNER_CHECK",
                    graceSeconds =
                        settings
                            .gracePeriodSeconds
                )
        )
    }


    private fun ownerPresenceObserved() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.OWNER_CHECK
        ) {
            return
        }

        val current =
            AlarmStateStore.state
                .value

        AlarmStateStore.update(
            current.copy(
                message =
                    getString(R.string.svc_unlocked_message),
                ownerCheckDeadline =
                    System.currentTimeMillis() +
                        ownerGraceMs()
            )
        )

        scheduleOwnerCheckExpiry()
    }

    private fun scheduleOwnerCheckExpiry() {
        ownerCheckJob?.cancel()

        val deadline =
            System.currentTimeMillis() +
                ownerGraceMs()

        val current =
            AlarmStateStore.state
                .value

        if (
            current.phase ==
            AlarmPhase.OWNER_CHECK
        ) {
            AlarmStateStore.update(
                current.copy(
                    ownerCheckDeadline =
                        deadline
                )
            )
        }

        ownerCheckJob =
            serviceScope.launch {
                delay(
                    ownerGraceMs()
                )

                val pending =
                    pendingAlarmTrigger

                if (pending != null) {
                    pendingAlarmTrigger = null

                    triggerAlarm(
                        detail =
                            pending.detail,
                        lastMotionDelta =
                            pending
                                .lastMotionDelta,
                        confidence =
                            pending.confidence,
                        detectorState =
                            pending
                                .detectorState,
                        sensorSummary =
                            pending
                                .sensorSummary
                    )
                    return@launch
                }

                val expired =
                    pocketController
                        .ownerCheckExpired()

                if (
                    expired?.state ==
                    GuardState.ALARM
                ) {
                    triggerPocketAlarm(
                        reason =
                            expired.reason,
                        confidence = 100
                    )
                }
            }
    }

    private fun ownerVerified(
        source: String
    ) {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.OWNER_CHECK
        ) {
            return
        }

        ownerCheckJob?.cancel()
        ownerCheckJob = null

        outputController
            .stopPreAlarm()

        val pending =
            pendingAlarmTrigger

        pendingAlarmTrigger = null

        val detectorState =
            if (pending != null) {
                "OWNER_VERIFIED"
            } else {
                pocketController
                    .ownerVerified()
                    ?.state
                    ?.name
                    ?: return
            }

        container.eventRepository.add(
            AlarmEventType.OWNER_VERIFIED,
            "Mode=${activeMode.name}; source=$source",
            evidence =
                AlarmEventEvidence(
                    mode = activeMode,
                    source = source,
                    detectorState =
                        detectorState
                )
        )

        if (pending != null) {
            when (
                ProtectionPolicy
                    .pendingTriggerOwnerAction(
                        activeMode
                    )
            ) {
                PendingTriggerOwnerAction
                    .END_CHARGING_SESSION -> {
                    disarm(
                        addDisarmEvent = false
                    )
                    return
                }

                PendingTriggerOwnerAction
                    .END_ACCESSORY_SESSION -> {
                    disarm(
                        addDisarmEvent = false
                    )
                    return
                }

                PendingTriggerOwnerAction
                    .RESTART_TABLE -> {
                    val settings =
                        activeSettings
                            ?: return

                    val calibration =
                        container
                            .sessionStateRepository
                            .read()
                            ?.tableCalibration

                    if (calibration != null) {
                        startTableMonitoring(
                            settings = settings,
                            initialCalibration =
                                calibration,
                            recordArmedEvent =
                                false
                        )
                    } else {
                        beginCalibration(
                            settings = settings,
                            mode =
                                ProtectionMode.TABLE
                        )
                    }
                    return
                }

                PendingTriggerOwnerAction
                    .RESUME_ARMED ->
                    Unit
            }
        }

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = activeMode,
                message =
                    getString(R.string.svc_owner_verified),
                confidenceScore = 0,
                detectorState =
                    detectorState,
                journeyEndsAt =
                    activeJourneyEndAt
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository.saveState(
            mode = activeMode,
            phase = AlarmPhase.ARMED,
            pocketGuardMode = activeGuardMode,
            journeyEndsAt = activeJourneyEndAt,
            journeyEndsAtElapsedRealtime =
                activeJourneyEndAtElapsedRealtime
        )

        updateNotification(
            AlarmPhase.ARMED,
            getString(
                R.string.svc_mode_protected_title,
                activeMode.label
            ),
            getString(R.string.svc_owner_verified)
        )

        if (activeMode == ProtectionMode.JOURNEY) {
            scheduleJourneyEnd(
                endAt = activeJourneyEndAt,
                endAtElapsedRealtime =
                    activeJourneyEndAtElapsedRealtime
            )
        }
    }

    private fun triggerTableAlarm(
        trigger: MotionTrigger
    ) {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.ARMED
        ) {
            return
        }

        requestAlarm(
            detail =
                "Mode=TABLE; Accel=" +
                    String.format(
                        Locale.US,
                        "%.2f",
                        trigger
                            .accelerationDelta
                    ) +
                    " m/s²; Gyro=" +
                    String.format(
                        Locale.US,
                        "%.2f",
                        trigger
                            .gyroscopeMagnitude
                    ) +
                    " rad/s",
            lastMotionDelta =
                trigger.accelerationDelta,
            confidence = 100,
            detectorState =
                "TABLE_TRIGGER",
            sensorSummary =
                "accel=" +
                    String.format(
                        Locale.US,
                        "%.2f",
                        trigger
                            .accelerationDelta
                    ) +
                    "; gyro=" +
                    String.format(
                        Locale.US,
                        "%.2f",
                        trigger
                            .gyroscopeMagnitude
                    )
        )
    }

    private fun requestAlarm(
        detail: String,
        lastMotionDelta: Float,
        confidence: Int,
        detectorState: String,
        sensorSummary: String? = null
    ) {
        if (ownerGraceMs() <= 0L) {
            triggerAlarm(
                detail = detail,
                lastMotionDelta =
                    lastMotionDelta,
                confidence = confidence,
                detectorState =
                    detectorState,
                sensorSummary =
                    sensorSummary
            )
            return
        }

        beginOwnerCheck(
            PendingAlarmTrigger(
                detail = detail,
                lastMotionDelta =
                    lastMotionDelta,
                confidence = confidence,
                detectorState =
                    detectorState,
                sensorSummary =
                    sensorSummary
            )
        )
    }

    private fun triggerPocketAlarm(
        reason: String,
        confidence: Int
    ) {
        triggerAlarm(
            detail =
                "Mode=${activeMode.name}; " +
                    "guard=${activeGuardMode?.name}; " +
                    "confidence=$confidence; " +
                    reason,
            lastMotionDelta = 0f,
            confidence = confidence,
            detectorState =
                GuardState.ALARM.name,
            sensorSummary =
                reason
        )
    }

    private fun triggerAlarm(
        detail: String,
        lastMotionDelta: Float,
        confidence: Int,
        detectorState: String,
        sensorSummary: String? = null
    ) {
        protectionWakeLock.forceRequired()
        val settings =
            activeSettings ?: return

        ownerCheckJob?.cancel()
        ownerCheckJob = null
        pendingAlarmTrigger = null

        journeyJob?.cancel()
        journeyJob = null

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.TRIGGERED,
                mode = activeMode,
                message =
                    getString(R.string.svc_alarm_triggered),
                lastMotionDelta =
                    lastMotionDelta,
                confidenceScore =
                    confidence,
                detectorState =
                    detectorState,
                journeyEndsAt =
                    activeJourneyEndAt,
                triggeredAt =
                    System.currentTimeMillis()
            )
        )

        refreshQuickSurfaces()

        container.sessionStateRepository.saveState(
            mode = activeMode,
            phase = AlarmPhase.TRIGGERED,
            pocketGuardMode = activeGuardMode,
            journeyEndsAt = activeJourneyEndAt,
            journeyEndsAtElapsedRealtime =
                activeJourneyEndAtElapsedRealtime
        )

        startAsForeground(
            phase = AlarmPhase.TRIGGERED,
            title = getString(R.string.svc_alarm_title, activeMode.label),
            text =
                getString(R.string.svc_alarm_body)
        )

        val profile =
            settings.alarmProfile(
                activeMode
            )

        val outputHealth =
            outputController.start(
                profile
            )

        scheduleAlarmOutputTimeout(
            profile.alarmDuration.durationMs,
            AlarmStateStore.state
                .value.triggeredAt
                ?: System.currentTimeMillis()
        )

        container
            .eventRepository
            .add(
                AlarmEventType.TRIGGERED,
                detail,
                evidence =
                    AlarmEventEvidence(
                        mode = activeMode,
                        source =
                            activeActivationSource.name,
                        reason = detail,
                        confidenceScore =
                            confidence,
                        detectorState =
                            detectorState,
                        sensorSummary =
                            sensorSummary,
                        outputSummary =
                            outputHealth
                                .eventSummary(),
                        graceSeconds =
                            settings
                                .gracePeriodSeconds
                    )
            )
    }

    private fun scheduleAlarmOutputTimeout(
        durationMs: Long?,
        triggeredAt: Long
    ) {
        alarmOutputTimeoutJob
            ?.cancel()
        alarmOutputTimeoutJob =
            null

        val configuredDuration =
            durationMs
                ?: return

        val elapsed =
            (
                System.currentTimeMillis() -
                    triggeredAt
                )
                .coerceAtLeast(
                    0L
                )

        val remaining =
            (
                configuredDuration -
                    elapsed
                )
                .coerceAtLeast(
                    0L
                )

        alarmOutputTimeoutJob =
            serviceScope.launch {
                if (
                    remaining > 0L
                ) {
                    delay(
                        remaining
                    )
                }

                if (
                    AlarmStateStore.state
                        .value.phase ==
                        AlarmPhase.TRIGGERED
                ) {
                    outputController.stop()

                    container.eventRepository.add(
                        AlarmEventType.ALARM_OUTPUTS_TIMED_OUT,
                        "Mode=${activeMode.name}; alarm outputs stopped after configured duration.",
                        evidence =
                            AlarmEventEvidence(
                                mode = activeMode,
                                source =
                                    activeActivationSource.name,
                                detectorState =
                                    "ALARM_OUTPUTS_TIMED_OUT"
                            )
                    )

                    updateNotification(
                        AlarmPhase.TRIGGERED,
                        getString(
                            R.string
                                .svc_alarm_title,
                            activeMode.label
                        ),
                        getString(
                            R.string
                                .svc_alarm_outputs_stopped
                        )
                    )
                }
            }
    }

    private fun scheduleJourneyEnd(
        endAt: Long?,
        endAtElapsedRealtime: Long?
    ) {
        journeyJob?.cancel()
        journeyJob = null

        if (
            activeMode !=
            ProtectionMode.JOURNEY
        ) {
            return
        }

        val phase =
            AlarmStateStore.state
                .value.phase

        if (
            phase == AlarmPhase.OWNER_CHECK ||
            phase == AlarmPhase.TRIGGERED
        ) {
            return
        }

        activeJourneyEndAt = endAt
        activeJourneyEndAtElapsedRealtime =
            endAtElapsedRealtime

        container.sessionStateRepository
            .saveJourneyEnd(
                endAt = endAt,
                endAtElapsedRealtime =
                    endAtElapsedRealtime
            )

        val remaining =
            ProtectionPolicy
                .remainingJourneyMs(
                    endAtElapsedRealtime =
                        endAtElapsedRealtime,
                    nowElapsedRealtime =
                        SystemClock.elapsedRealtime(),
                    endAtWallClock = endAt,
                    nowWallClock =
                        System.currentTimeMillis()
                )
                ?: return

        if (remaining <= 0L) {
            finishJourney()
            return
        }

        journeyJob =
            serviceScope.launch {
                delay(remaining)
                finishJourney()
            }
    }

    private fun finishJourney() {
        journeyJob = null

        val phase =
            AlarmStateStore.state
                .value.phase

        if (
            phase == AlarmPhase.OWNER_CHECK ||
            phase == AlarmPhase.TRIGGERED
        ) {
            return
        }

        container.eventRepository.add(
            AlarmEventType.JOURNEY_ENDED,
            "Journey session completed."
        )

        disarm(
            addDisarmEvent = false
        )
    }

    private fun disarm(
        addDisarmEvent: Boolean = true
    ) {
        val previous =
            AlarmStateStore.state
                .value.phase

        armingJob?.cancel()
        armingJob = null

        ownerCheckJob?.cancel()
        ownerCheckJob = null
        pendingAlarmTrigger = null

        journeyJob?.cancel()
        journeyJob = null

        alarmOutputTimeoutJob?.cancel()
        alarmOutputTimeoutJob = null

        tableDetector?.stop()
        tableDetector = null

        pocketController.stop()
        accessoryMonitor.stop()

        outputController.stop()
        protectionWakeLock.release()

        when {
            previous == AlarmPhase.ARMING ||
                previous == AlarmPhase.CALIBRATING -> {
                container.eventRepository.add(
                    AlarmEventType.CANCELED,
                    "Mode=${activeMode.name}; armamento annullato."
                )
            }

            addDisarmEvent &&
                previous != AlarmPhase.DISARMED -> {
                container.eventRepository.add(
                    AlarmEventType.DISARMED,
                    "Mode=${activeMode.name}; protezione disattivata."
                )
            }
        }

        container.sessionStateRepository.clear()
        activeSettings = null
        activeMode = ProtectionMode.TABLE
        activeGuardMode = null
        activeJourneyEndAt = null
        activeJourneyEndAtElapsedRealtime = null
        AlarmStateStore.reset()
        refreshQuickSurfaces()

        if (autoArmStandbyEnabled) {
            enterChargingAutoArmStandby()
        } else {
            ServiceCompat.stopForeground(
                this,
                ServiceCompat
                    .STOP_FOREGROUND_REMOVE
            )

            stopSelf()
        }
    }

    private fun enterChargingAutoArmStandby() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return
        }

        protectionWakeLock.release()
        autoArmAwaitingFreshConnection =
            chargingMonitor
                .isPowerConnected()

        startAsForeground(
            phase = AlarmPhase.DISARMED,
            title =
                getString(
                    R.string
                        .svc_auto_arm_title
                ),
            text =
                getString(
                    if (
                        autoArmAwaitingFreshConnection
                    ) {
                        R.string
                            .svc_auto_arm_wait_reconnect
                    } else {
                        R.string
                            .svc_auto_arm_wait_connect
                    }
                )
        )
    }

    private fun restorePersistedSession() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return
        }

        val persisted =
            container
                .sessionStateRepository
                .read()
                ?: run {
                    serviceScope.launch {
                        val settings =
                            container
                                .settingsRepository
                                .settings
                                .first()

                        autoArmStandbyEnabled =
                            settings
                                .autoArmCharging

                        if (
                            autoArmStandbyEnabled
                        ) {
                            enterChargingAutoArmStandby()
                        } else {
                            outputController.stop()
                            ServiceCompat.stopForeground(
                                this@ProtectionForegroundService,
                                ServiceCompat
                                    .STOP_FOREGROUND_REMOVE
                            )
                            stopSelf()
                        }
                    }
                    return
                }

        activeMode = persisted.mode
        activeGuardMode =
            persisted.pocketGuardMode
        activeJourneyEndAt =
            persisted.journeyEndsAt
        activeJourneyEndAtElapsedRealtime =
            persisted.journeyEndsAtElapsedRealtime

        startAsForeground(
            phase =
                if (
                    persisted.phase ==
                    AlarmPhase.TRIGGERED ||
                    persisted.phase ==
                    AlarmPhase.OWNER_CHECK
                ) {
                    AlarmPhase.TRIGGERED
                } else {
                    AlarmPhase.CALIBRATING
                },
            title =
                getString(R.string.svc_restore_title, persisted.mode.label),
            text =
                getString(R.string.svc_restore_body)
        )

        protectionWakeLock.forceRequired()

        serviceScope.launch {
            val settings =
                container.settingsRepository
                    .settings.first()

            activeSettings = settings
            autoArmStandbyEnabled =
                settings.autoArmCharging

            when (
                SessionRecoveryPolicy
                    .recoveryAction(
                        session = persisted,
                        now =
                            System.currentTimeMillis(),
                        nowElapsedRealtime =
                            SystemClock.elapsedRealtime()
                    )
            ) {
                RecoveryAction.STOP -> {
                    if (
                        persisted.mode ==
                        ProtectionMode.JOURNEY
                    ) {
                        container.eventRepository.add(
                            AlarmEventType.JOURNEY_ENDED,
                            "Journey expired before recovery."
                        )
                    }

                    outputController.stop()
                    container.sessionStateRepository.clear()
                    protectionWakeLock.release()
                    ServiceCompat.stopForeground(
                        this@ProtectionForegroundService,
                        ServiceCompat
                            .STOP_FOREGROUND_REMOVE
                    )
                    stopSelf()
                }

                RecoveryAction.RESTORE_ALARM -> {
                    container.sessionStateRepository.saveState(
                        mode = persisted.mode,
                        phase = AlarmPhase.TRIGGERED,
                        pocketGuardMode =
                            persisted.pocketGuardMode,
                        journeyEndsAt =
                            persisted.journeyEndsAt,
                        journeyEndsAtElapsedRealtime =
                            persisted
                                .journeyEndsAtElapsedRealtime
                    )

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.TRIGGERED,
                            mode = persisted.mode,
                            message =
                                getString(R.string.svc_alarm_restored),
                            confidenceScore = 100,
                            journeyEndsAt =
                                persisted.journeyEndsAt,
                            triggeredAt =
                                persisted.updatedAtWallClock
                        )
                    )

                    refreshQuickSurfaces()

                    val profile =
                        settings.alarmProfile(
                            persisted.mode
                        )

                    outputController.start(
                        profile
                    )

                    scheduleAlarmOutputTimeout(
                        profile
                            .alarmDuration
                            .durationMs,
                        persisted
                            .updatedAtWallClock
                    )
                }

                RecoveryAction.RESTORE_TABLE -> {
                    startTableMonitoring(
                        settings = settings,
                        initialCalibration =
                            persisted.tableCalibration,
                        recordArmedEvent = false
                    )
                }

                RecoveryAction.RESTORE_CHARGING -> {
                    startChargingMonitoring(
                        settings = settings,
                        recordArmedEvent =
                            false
                    )
                }

                RecoveryAction.RESTORE_ACCESSORY -> {
                    startAccessoryMonitoring(
                        settings = settings,
                        recordArmedEvent =
                            false,
                        expectedFingerprints =
                            persisted
                                .accessoryFingerprints
                    )
                }

                RecoveryAction.RESTORE_POCKET -> {
                    val profile =
                        persisted.pocketCalibration

                    if (profile == null) {
                        beginCalibration(
                            settings,
                            persisted.mode
                        )
                    } else {
                        startPocketMonitoring(
                            settings = settings,
                            mode = persisted.mode,
                            profile = profile,
                            recordArmedEvent = false,
                            journeyEndAt =
                                persisted.journeyEndsAt,
                            journeyEndAtElapsedRealtime =
                                persisted
                                    .journeyEndsAtElapsedRealtime,
                            guardModeOverride =
                                persisted.pocketGuardMode
                        )
                    }
                }

                RecoveryAction.RECALIBRATE -> {
                    beginCalibration(
                        settings = settings,
                        mode = persisted.mode
                    )
                }
            }
        }
    }

    private fun failAndStop(
        message: String
    ) {
        container.eventRepository.add(
            AlarmEventType.ERROR,
            "Mode=${activeMode.name}; $message"
        )

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ERROR,
                mode = activeMode,
                message = message
            )
        )

        refreshQuickSurfaces()

        updateNotification(
            AlarmPhase.ERROR,
            getString(R.string.svc_error_title),
            message
        )

        serviceScope.launch {
            delay(1_500)
            disarm(
                addDisarmEvent = false
            )
        }
    }

    private fun startAsForeground(
        phase: AlarmPhase,
        title: String,
        text: String
    ) {
        val notification =
            buildNotification(
                phase,
                title,
                text
            )

        val serviceType =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES
                    .UPSIDE_DOWN_CAKE
            ) {
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            serviceType
        )
    }

    private fun updateNotification(
        phase: AlarmPhase,
        title: String,
        text: String
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(
                phase,
                title,
                text
            )
        )
    }

    private fun buildNotification(
        phase: AlarmPhase,
        title: String,
        text: String
    ): Notification {
        val openAppIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                flags =
                    Intent
                        .FLAG_ACTIVITY_SINGLE_TOP or
                    Intent
                        .FLAG_ACTIVITY_CLEAR_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                100,
                openAppIntent,
                PendingIntent
                    .FLAG_UPDATE_CURRENT or
                    PendingIntent
                        .FLAG_IMMUTABLE
            )

        val urgent =
            phase == AlarmPhase.TRIGGERED ||
                phase == AlarmPhase.OWNER_CHECK

        return NotificationCompat
            .Builder(
                this,
                if (urgent) {
                    CHANNEL_ALARM
                } else {
                    CHANNEL_MONITOR
                }
            )
            .setSmallIcon(
                R.drawable.ic_stat_shield
            )
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(
                pendingIntent
            )
            .setOngoing(
                phase != AlarmPhase.ERROR
            )
            .setOnlyAlertOnce(
                !urgent
            )
            .setCategory(
                if (urgent) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_SERVICE
                }
            )
            .setPriority(
                if (urgent) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_LOW
                }
            )
            .build()
    }

    private fun createNotificationChannels() {
        val monitor =
            NotificationChannel(
                CHANNEL_MONITOR,
                getString(R.string.svc_channel_monitor_name),
                NotificationManager
                    .IMPORTANCE_LOW
            ).apply {
                description =
                    getString(R.string.svc_channel_monitor_desc)
                setSound(null, null)
                enableVibration(false)
            }

        val alarm =
            NotificationChannel(
                CHANNEL_ALARM,
                getString(R.string.svc_channel_alarm_name),
                NotificationManager
                    .IMPORTANCE_HIGH
            ).apply {
                description =
                    getString(R.string.svc_channel_alarm_desc)
                setSound(null, null)
                enableVibration(false)
            }

        notificationManager
            .createNotificationChannels(
                listOf(
                    monitor,
                    alarm
                )
            )
    }

    private fun updateMonitoringWakeLock(
        mode: ProtectionMode,
        guardMode: GuardMode?
    ) {
        protectionWakeLock.setRequired(
            ProtectionWakePolicy.requiresCpuLease(
                mode = mode,
                guardMode = guardMode,
                capabilities =
                    pocketController.capabilities
            )
        )
    }

    private fun armingInstruction(
        mode: ProtectionMode
    ): String =
        getString(
            when (mode) {
                ProtectionMode.TABLE ->
                    R.string.svc_instruction_table
                ProtectionMode.POCKET ->
                    R.string.svc_instruction_pocket
                ProtectionMode.BAG ->
                    R.string.svc_instruction_bag
                ProtectionMode.CHARGING ->
                    R.string.svc_instruction_charging
                ProtectionMode.ACCESSORY ->
                    R.string.svc_instruction_accessory
                ProtectionMode.JOURNEY ->
                    R.string.svc_instruction_journey
            }
        )

    private fun calibrationMessage(
        mode: ProtectionMode
    ): String =
        getString(
            when (mode) {
                ProtectionMode.TABLE ->
                    R.string.svc_calibration_table
                ProtectionMode.POCKET ->
                    R.string.svc_calibration_pocket
                ProtectionMode.BAG ->
                    R.string.svc_calibration_bag
                ProtectionMode.CHARGING ->
                    R.string.svc_calibration_charging
                ProtectionMode.ACCESSORY ->
                    R.string.svc_calibration_accessory
                ProtectionMode.JOURNEY ->
                    R.string.svc_calibration_journey
            }
        )

    private fun waitingMessage(
        mode: ProtectionMode,
        guardMode: GuardMode
    ): String =
        getString(
            when (mode) {
                ProtectionMode.POCKET ->
                    R.string.svc_wait_pocket

                ProtectionMode.BAG ->
                    R.string.svc_wait_bag

                ProtectionMode.JOURNEY ->
                    if (guardMode == GuardMode.POCKET) {
                        R.string.svc_wait_journey_pocket
                    } else {
                        R.string.svc_wait_journey_bag
                    }

                ProtectionMode.TABLE ->
                    R.string.svc_wait_table

                ProtectionMode.CHARGING ->
                    R.string.svc_wait_charging

                ProtectionMode.ACCESSORY ->
                    R.string.svc_wait_accessory
            }
        )

    private fun refreshQuickSurfaces() {
        PhoneGuardWidgetProvider.updateAll(
            applicationContext
        )

        runCatching {
            TileService.requestListeningState(
                applicationContext,
                ComponentName(
                    applicationContext,
                    PhoneGuardTileService::class.java
                )
            )
        }
    }

    private fun AlarmOutputHealth.eventSummary():
        String =
        buildList {
            if (soundRequested) {
                add(
                    "sound=" +
                        if (soundActive) {
                            "active"
                        } else {
                            "unverified"
                        }
                )
            }

            if (vibrationRequested) {
                add(
                    "vibration=" +
                        if (vibrationActive) {
                            "active"
                        } else {
                            "unavailable"
                        }
                )
            }

            if (flashRequested) {
                add(
                    "flash=" +
                        if (flashActive) {
                            "active"
                        } else {
                            "unavailable"
                        }
                )
            }

            if (soundRequested) {
                add(
                    "volume=${effectiveVolumePercent}%"
                )
            }
        }
            .joinToString(
                separator = "; "
            )

    override fun onDestroy() {
        armingJob?.cancel()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        alarmOutputTimeoutJob?.cancel()
        tableDetector?.stop()
        pocketController.stop()
        accessoryMonitor.stop()
        outputController.stop()
        protectionWakeLock.close()

        runCatching {
            unregisterReceiver(ownerReceiver)
        }

        runCatching {
            unregisterReceiver(powerReceiver)
        }

        serviceScope.cancel()

        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            AlarmStateStore.reset()
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    companion object {
        private const val ACTION_ARM =
            "com.riccardopinato.phoneguard.action.ARM"

        private const val ACTION_DISARM =
            "com.riccardopinato.phoneguard.action.DISARM"

        private const val ACTION_OWNER_VERIFIED =
            "com.riccardopinato.phoneguard.action.OWNER_VERIFIED"

        private const val ACTION_RESTORE =
            "com.riccardopinato.phoneguard.action.RESTORE"

        private const val ACTION_SET_AUTO_ARM =
            "com.riccardopinato.phoneguard.action.SET_AUTO_ARM"

        private const val EXTRA_MODE =
            "protection_mode"

        private const val EXTRA_SOURCE =
            "activation_source"

        private const val EXTRA_AUTO_ARM =
            "auto_arm_enabled"

        private const val NOTIFICATION_ID =
            4101

        private const val CHANNEL_MONITOR =
            "phone_guard_monitor"

        private const val CHANNEL_ALARM =
            "phone_guard_alarm"

        fun arm(
            context: Context,
            mode: ProtectionMode,
            source: ActivationSource =
                ActivationSource.APP
        ) {
            val intent =
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                )
                    .setAction(ACTION_ARM)
                    .putExtra(
                        EXTRA_MODE,
                        mode.name
                    )
                    .putExtra(
                        EXTRA_SOURCE,
                        source.name
                    )

            ContextCompat
                .startForegroundService(
                    context,
                    intent
                )
        }

        fun setChargingAutoArm(
            context: Context,
            enabled: Boolean
        ) {
            val intent =
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                )
                    .setAction(
                        ACTION_SET_AUTO_ARM
                    )
                    .putExtra(
                        EXTRA_AUTO_ARM,
                        enabled
                    )

            if (enabled) {
                ContextCompat
                    .startForegroundService(
                        context,
                        intent
                    )
            } else {
                runCatching {
                    context.startService(
                        intent
                    )
                }
            }
        }

        fun disarm(
            context: Context
        ) {
            val intent =
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                ).setAction(
                    ACTION_DISARM
                )

            runCatching {
                context.startService(intent)
            }
        }

        fun restore(
            context: Context
        ) {
            val intent =
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                ).setAction(
                    ACTION_RESTORE
                )

            ContextCompat
                .startForegroundService(
                    context,
                    intent
                )
        }

        fun ownerVerified(
            context: Context
        ) {
            val intent =
                Intent(
                    context,
                    ProtectionForegroundService::class.java
                ).setAction(
                    ACTION_OWNER_VERIFIED
                )

            runCatching {
                context.startService(intent)
            }
        }
    }
}
