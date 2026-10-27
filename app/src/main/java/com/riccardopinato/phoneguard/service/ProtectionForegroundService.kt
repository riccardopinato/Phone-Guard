package com.riccardopinato.phoneguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.RecoveryAction
import com.riccardopinato.phoneguard.data.SessionRecoveryPolicy
import com.riccardopinato.phoneguard.data.SecurityHealth
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.DetectionDecision
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
import com.riccardopinato.phoneguard.power.ChargingGuardController
import com.riccardopinato.phoneguard.power.ProtectionWakeLock
import com.riccardopinato.phoneguard.power.ProtectionWakePolicy
import com.riccardopinato.phoneguard.protection.PocketProtectionController
import com.riccardopinato.phoneguard.protection.ProtectionPolicy
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.sensor.MotionDetector
import com.riccardopinato.phoneguard.sensor.MotionTrigger
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    private lateinit var chargingController:
        ChargingGuardController

    private val container by lazy {
        (application as PhoneGuardApp)
            .container
    }

    private var armingJob: Job? = null
    private var ownerCheckJob: Job? = null
    private var journeyJob: Job? = null

    private var tableDetector:
        MotionDetector? = null

    private lateinit var protectionWakeLock:
        ProtectionWakeLock

    private var activeSettings:
        AppSettings? = null

    private var activeMode:
        ProtectionMode = ProtectionMode.TABLE

    private var activeGuardMode:
        GuardMode? = null

    private var activeJourneyEndAt:
        Long? = null

    private var activeJourneyEndAtElapsedRealtime:
        Long? = null

    private var pendingTrigger:
        PendingTrigger? = null

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

        chargingController =
            ChargingGuardController(
                context = applicationContext,
                onDisconnected =
                    ::triggerChargingAlarm
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

                beginArming(mode)
            }

            ACTION_DISARM ->
                disarm()

            ACTION_OWNER_VERIFIED ->
                ownerVerified(
                    "Authenticated in app"
                )

            ACTION_RESTORE,
            null ->
                restorePersistedSession()
        }

        return START_STICKY
    }

    private fun beginArming(
        mode: ProtectionMode
    ) {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return
        }

        activeMode = mode

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

        startAsForeground(
            phase = AlarmPhase.ARMING,
            title = getString(R.string.svc_preparing_title, mode.label),
            text = armingInstruction(mode)
        )

        protectionWakeLock.forceRequired()

        armingJob?.cancel()

        armingJob =
            serviceScope.launch {
                val securityHealth =
                    container
                        .securityRepository
                        .securityHealth
                        .first()

                if (
                    securityHealth !=
                    SecurityHealth.READY
                ) {
                    failAndStop(
                        getString(
                            R.string
                                .svc_security_not_ready
                        )
                    )
                    return@launch
                }

                val settings =
                    container
                        .settingsRepository
                        .settings
                        .first()

                activeSettings = settings

                container.eventRepository.add(
                    AlarmEventType.ARMING_STARTED,
                    "Mode=${mode.name}; countdown=" +
                        "${settings.countdownSeconds}s"
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
                settings = settings
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
        recordArmedEvent: Boolean = true
    ) {
        tableDetector?.stop()
        tableDetector = null
        pocketController.stop()

        if (!chargingController.start()) {
            failAndStop(
                getString(
                    R.string.svc_charger_required
                )
            )
            return
        }

        activeMode =
            ProtectionMode.CHARGING
        activeGuardMode = null
        activeJourneyEndAt = null
        activeJourneyEndAtElapsedRealtime = null

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = ProtectionMode.CHARGING,
                message =
                    getString(
                        R.string
                            .svc_charging_protected
                    )
            )
        )

        container.sessionStateRepository
            .saveState(
                mode =
                    ProtectionMode.CHARGING,
                phase =
                    AlarmPhase.ARMED
            )

        if (recordArmedEvent) {
            container.eventRepository.add(
                AlarmEventType.ARMED,
                "Mode=CHARGING; power connected",
                mapOf(
                    "mode" to "CHARGING",
                    "source" to "power"
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
                    .svc_charging_hint
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
        chargingController.stop()

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
        chargingController.stop()

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
                    "samples=${profile.sampleCount}"
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
        val snapshot =
            pocketController
                .lastSensorSnapshot()

        val metadata =
            buildMap {
                put(
                    "mode",
                    activeMode.name
                )
                put(
                    "guard",
                    activeGuardMode
                        ?.name
                        ?: "n/a"
                )
                put(
                    "confidence",
                    decision
                        .confidenceScore
                        .toString()
                )

                snapshot?.let {
                    put(
                        "motion",
                        String.format(
                            Locale.US,
                            "%.3f",
                            it.motionScore
                        )
                    )
                    put(
                        "rotation",
                        String.format(
                            Locale.US,
                            "%.3f",
                            it.rotationScore
                        )
                    )
                    it.lux?.let {
                        lux ->
                        put(
                            "lux",
                            String.format(
                                Locale.US,
                                "%.1f",
                                lux
                            )
                        )
                    }
                    it.proximityNear
                        ?.let {
                            near ->
                            put(
                                "proximity",
                                if (near) {
                                    "near"
                                } else {
                                    "far"
                                }
                            )
                        }
                }
            }

        beginPreAlarmOrTrigger(
            PendingTrigger(
                detail =
                    "Mode=${activeMode.name}; " +
                        "guard=${activeGuardMode?.name}; " +
                        "confidence=${decision.confidenceScore}; " +
                        decision.reason,
                lastMotionDelta =
                    snapshot
                        ?.motionScore
                        ?: 0f,
                confidence =
                    decision.confidenceScore,
                detectorState =
                    decision.state.name,
                metadata =
                    metadata
            )
        )
    }

    private fun beginPreAlarmOrTrigger(
        trigger: PendingTrigger
    ) {
        val graceSeconds =
            activeSettings
                ?.preAlarmSeconds
                ?: 0

        if (graceSeconds <= 0) {
            triggerAlarm(
                detail = trigger.detail,
                lastMotionDelta =
                    trigger.lastMotionDelta,
                confidence =
                    trigger.confidence,
                metadata =
                    trigger.metadata
            )
            return
        }

        pendingTrigger = trigger
        protectionWakeLock.forceRequired()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        journeyJob = null

        val deadline =
            System.currentTimeMillis() +
                graceSeconds * 1_000L

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

        scheduleOwnerCheckExpiry()
    }

    private fun ownerPresenceObserved() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.OWNER_CHECK
        ) {
            return
        }

        val graceMs =
            ownerGraceMs()

        if (graceMs <= 0L) {
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
                        graceMs
            )
        )

        scheduleOwnerCheckExpiry()
    }

    private fun scheduleOwnerCheckExpiry() {
        ownerCheckJob?.cancel()

        val graceMs =
            ownerGraceMs()

        val deadline =
            System.currentTimeMillis() +
                graceMs

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
                delay(graceMs)

                val trigger =
                    pendingTrigger
                        ?: return@launch

                if (
                    activeGuardMode !=
                    null
                ) {
                    pocketController
                        .ownerCheckExpired()
                }

                triggerAlarm(
                    detail =
                        trigger.detail,
                    lastMotionDelta =
                        trigger
                            .lastMotionDelta,
                    confidence =
                        trigger.confidence,
                    metadata =
                        trigger.metadata
                )
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

        pendingTrigger = null

        container.eventRepository.add(
            AlarmEventType.OWNER_VERIFIED,
            "Mode=${activeMode.name}; source=$source",
            mapOf(
                "mode" to activeMode.name,
                "source" to source
            )
        )

        if (
            activeGuardMode ==
            null
        ) {
            disarm(
                addDisarmEvent = false
            )
            return
        }

        val decision =
            pocketController
                .ownerVerified()
                ?: return

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = activeMode,
                message =
                    getString(R.string.svc_owner_verified),
                confidenceScore = 0,
                detectorState =
                    decision.state.name,
                journeyEndsAt =
                    activeJourneyEndAt
            )
        )

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

        beginPreAlarmOrTrigger(
            PendingTrigger(
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
                metadata =
                    mapOf(
                        "mode" to "TABLE",
                        "trigger" to "motion",
                        "acceleration" to
                            String.format(
                                Locale.US,
                                "%.2f",
                                trigger
                                    .accelerationDelta
                            ),
                        "gyroscope" to
                            String.format(
                                Locale.US,
                                "%.2f",
                                trigger
                                    .gyroscopeMagnitude
                            )
                    )
            )
        )
    }

    private fun triggerPocketAlarm(
        reason: String,
        confidence: Int
    ) {
        val snapshot =
            pocketController
                .lastSensorSnapshot()

        triggerAlarm(
            detail =
                "Mode=${activeMode.name}; " +
                    "guard=${activeGuardMode?.name}; " +
                    "confidence=$confidence; " +
                    reason,
            lastMotionDelta =
                snapshot
                    ?.motionScore
                    ?: 0f,
            confidence = confidence,
            metadata =
                buildMap {
                    put(
                        "mode",
                        activeMode.name
                    )
                    put(
                        "guard",
                        activeGuardMode
                            ?.name
                            ?: "n/a"
                    )
                    put(
                        "confidence",
                        confidence.toString()
                    )
                    put(
                        "trigger",
                        "sensor_fusion"
                    )
                    snapshot?.let {
                        put(
                            "motion",
                            String.format(
                                Locale.US,
                                "%.3f",
                                it.motionScore
                            )
                        )
                        put(
                            "rotation",
                            String.format(
                                Locale.US,
                                "%.3f",
                                it.rotationScore
                            )
                        )
                        it.lux?.let {
                            lux ->
                            put(
                                "lux",
                                String.format(
                                    Locale.US,
                                    "%.1f",
                                    lux
                                )
                            )
                        }
                        it.proximityNear
                            ?.let {
                                near ->
                                put(
                                    "proximity",
                                    if (near) {
                                        "near"
                                    } else {
                                        "far"
                                    }
                                )
                            }
                    }
                }
        )
    }

    private fun triggerChargingAlarm() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.ARMED ||
            activeMode !=
            ProtectionMode.CHARGING
        ) {
            return
        }

        beginPreAlarmOrTrigger(
            PendingTrigger(
                detail =
                    "Mode=CHARGING; power disconnected",
                lastMotionDelta = 0f,
                confidence = 100,
                detectorState =
                    "POWER_DISCONNECTED",
                metadata =
                    mapOf(
                        "mode" to "CHARGING",
                        "trigger" to
                            "power_disconnected",
                        "confidence" to "100"
                    )
            )
        )
    }

    private fun triggerAlarm(
        detail: String,
        lastMotionDelta: Float,
        confidence: Int,
        metadata: Map<String, String> =
            emptyMap()
    ) {
        protectionWakeLock.forceRequired()
        val settings =
            activeSettings ?: return

        ownerCheckJob?.cancel()
        ownerCheckJob = null
        pendingTrigger = null

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
                    if (activeMode == ProtectionMode.TABLE) {
                        "TABLE_TRIGGER"
                    } else {
                        GuardState.ALARM.name
                    },
                journeyEndsAt =
                    activeJourneyEndAt,
                triggeredAt =
                    System.currentTimeMillis()
            )
        )

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

        val outputHealth =
            outputController.start(
                settings,
                activeMode
            )

        container
            .eventRepository
            .add(
                AlarmEventType.TRIGGERED,
                detail,
                metadata +
                    mapOf(
                        "sound_profile" to
                            settings
                                .soundProfileFor(
                                    activeMode
                                )
                                .name,
                        "sound_active" to
                            outputHealth
                                .soundActive
                                .toString(),
                        "vibration_active" to
                            outputHealth
                                .vibrationActive
                                .toString(),
                        "flash_active" to
                            outputHealth
                                .flashActive
                                .toString(),
                        "effective_volume" to
                            outputHealth
                                .effectiveVolumePercent
                                .toString()
                    )
            )
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

        journeyJob?.cancel()
        journeyJob = null

        tableDetector?.stop()
        tableDetector = null

        pocketController.stop()
        chargingController.stop()

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
        pendingTrigger = null
        AlarmStateStore.reset()

        ServiceCompat.stopForeground(
            this,
            ServiceCompat
                .STOP_FOREGROUND_REMOVE
        )

        stopSelf()
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
                    outputController.stop()
                    stopSelf()
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

                    outputController.start(
                        settings,
                        persisted.mode
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
                        settings =
                            settings,
                        recordArmedEvent =
                            false
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

                ProtectionMode.CHARGING ->
                    R.string.svc_charging_hint

                ProtectionMode.TABLE ->
                    R.string.svc_wait_table
            }
        )

    private fun ownerGraceMs(): Long =
        (
            activeSettings
                ?.preAlarmSeconds
                ?: 0
            ) *
            1_000L

    private data class PendingTrigger(
        val detail: String,
        val lastMotionDelta: Float,
        val confidence: Int,
        val detectorState: String,
        val metadata: Map<String, String>
    )

    override fun onDestroy() {
        armingJob?.cancel()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        tableDetector?.stop()
        pocketController.stop()
        chargingController.stop()
        outputController.stop()
        protectionWakeLock.close()

        runCatching {
            unregisterReceiver(ownerReceiver)
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

        private const val EXTRA_MODE =
            "protection_mode"

        private const val NOTIFICATION_ID =
            4101

        private const val CHANNEL_MONITOR =
            "phone_guard_monitor"

        private const val CHANNEL_ALARM =
            "phone_guard_alarm"

        fun arm(
            context: Context,
            mode: ProtectionMode
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

            ContextCompat
                .startForegroundService(
                    context,
                    intent
                )
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
