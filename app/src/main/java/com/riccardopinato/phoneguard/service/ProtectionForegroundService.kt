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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.data.RecoveryAction
import com.riccardopinato.phoneguard.data.SessionRecoveryPolicy
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.model.ProtectionMode
import com.riccardopinato.phoneguard.pocket.model.CalibrationProfile
import com.riccardopinato.phoneguard.pocket.model.DetectionDecision
import com.riccardopinato.phoneguard.pocket.model.GuardMode
import com.riccardopinato.phoneguard.pocket.model.GuardSettings
import com.riccardopinato.phoneguard.pocket.model.GuardState
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

    private val container by lazy {
        (application as PhoneGuardApp)
            .container
    }

    private var armingJob: Job? = null
    private var ownerCheckJob: Job? = null
    private var journeyJob: Job? = null

    private var tableDetector:
        MotionDetector? = null

    private var wakeLock:
        PowerManager.WakeLock? = null

    private var activeSettings:
        AppSettings? = null

    private var activeMode:
        ProtectionMode = ProtectionMode.TABLE

    private var activeGuardMode:
        GuardMode? = null

    private var activeJourneyEndAt:
        Long? = null

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
                    "Preparazione ${mode.label}…"
            )
        )

        container.sessionStateRepository.saveState(
            mode = mode,
            phase = AlarmPhase.ARMING
        )

        startAsForeground(
            phase = AlarmPhase.ARMING,
            title = "Preparazione ${mode.label}",
            text = armingInstruction(mode)
        )

        acquireWakeLock()

        armingJob?.cancel()

        armingJob =
            serviceScope.launch {
                val settings =
                    container
                        .settingsRepository
                        .settings
                        .first()

                activeSettings = settings

                outputController.prepare(
                    settings
                )

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
                                "Attivazione ${mode.label} in ${remaining}s"
                        )
                    )

                    updateNotification(
                        AlarmPhase.ARMING,
                        "Preparazione ${mode.label}",
                        "Attivazione in ${remaining}s"
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
            "Calibrazione ${mode.label}",
            calibrationMessage(mode)
        )

        if (mode == ProtectionMode.TABLE) {
            startTableMonitoring(
                settings = settings
            )
            return
        }

        val requiredGuardMode =
            guardMode ?: run {
                failAndStop(
                    "Profilo Pocket/Bag non valido."
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

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.ARMED,
                            mode = ProtectionMode.TABLE,
                            message =
                                "Table protetto. Non muovere il dispositivo."
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
                        "Table protetto",
                        "Il movimento farà scattare l'allarme."
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
                "Accelerometro non disponibile."
            )
        }
    }

    private fun startPocketMonitoring(
        settings: AppSettings,
        mode: ProtectionMode,
        profile: CalibrationProfile,
        recordArmedEvent: Boolean,
        journeyEndAt: Long?,
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
                        "Profilo Pocket/Bag non valido."
                    )
                    return
                }

        activeMode = mode
        activeGuardMode = guardMode

        val guardSettings =
            GuardSettings(
                mode = guardMode,
                armDelaySeconds = 0,
                sensitivity =
                    settings.pocketSensitivity,
                journeyDuration =
                    settings.journeyDuration,
                pocketStableMs = 1_200L,
                removalWindowMs = 1_600L,
                ownerGraceMs = OWNER_GRACE_MS,
                soundEnabled = settings.soundEnabled,
                vibrationEnabled =
                    settings.vibrationEnabled,
                flashEnabled = settings.flashEnabled,
                biometricEnabled =
                    settings.biometricEnabled,
                enhancedReliability =
                    mode == ProtectionMode.JOURNEY
            )

        if (
            !pocketController.start(
                settings = guardSettings,
                profile = profile
            )
        ) {
            failAndStop(
                "Sensori insufficienti per ${guardMode.name}."
            )
            return
        }

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

        activeJourneyEndAt = endAt

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
            journeyEndsAt = endAt
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
            "${mode.label} protetto",
            waitingMessage(
                mode,
                guardMode
            )
        )

        scheduleJourneyEnd(endAt)
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
                            "${activeMode.label} attivo: dispositivo in posizione protetta."

                        GuardState.REMOVAL_SUSPECTED ->
                            "Possibile estrazione rilevata…"

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
                        "${activeMode.label} protetto",
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
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        journeyJob = null

        val deadline =
            System.currentTimeMillis() +
                OWNER_GRACE_MS

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.OWNER_CHECK,
                mode = activeMode,
                message =
                    "Verifica proprietario: sblocca il telefono o autenticati.",
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

        container.sessionStateRepository.saveState(
            mode = activeMode,
            phase = AlarmPhase.OWNER_CHECK,
            pocketGuardMode = activeGuardMode,
            journeyEndsAt = activeJourneyEndAt
        )

        startAsForeground(
            phase = AlarmPhase.OWNER_CHECK,
            title = "Verifica proprietario",
            text =
                "Autenticati per evitare l'allarme."
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

        val current =
            AlarmStateStore.state
                .value

        AlarmStateStore.update(
            current.copy(
                message =
                    "Telefono sbloccato. Conferma PIN o biometria per evitare l'allarme.",
                ownerCheckDeadline =
                    System.currentTimeMillis() +
                        OWNER_GRACE_MS
            )
        )

        scheduleOwnerCheckExpiry()
    }

    private fun scheduleOwnerCheckExpiry() {
        ownerCheckJob?.cancel()

        val deadline =
            System.currentTimeMillis() +
                OWNER_GRACE_MS

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
                delay(OWNER_GRACE_MS)

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

        val decision =
            pocketController
                .ownerVerified()
                ?: return

        container.eventRepository.add(
            AlarmEventType.OWNER_VERIFIED,
            "Mode=${activeMode.name}; source=$source"
        )

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMED,
                mode = activeMode,
                message =
                    "Proprietario verificato. Protezione ancora attiva.",
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
            journeyEndsAt = activeJourneyEndAt
        )

        updateNotification(
            AlarmPhase.ARMED,
            "${activeMode.label} protetto",
            "Proprietario verificato. Protezione ancora attiva."
        )

        if (activeMode == ProtectionMode.JOURNEY) {
            scheduleJourneyEnd(
                activeJourneyEndAt
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

        triggerAlarm(
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
            confidence = 100
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
            confidence = confidence
        )
    }

    private fun triggerAlarm(
        detail: String,
        lastMotionDelta: Float,
        confidence: Int
    ) {
        val settings =
            activeSettings ?: return

        ownerCheckJob?.cancel()
        ownerCheckJob = null

        journeyJob?.cancel()
        journeyJob = null

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.TRIGGERED,
                mode = activeMode,
                message =
                    "Protezione attivata!",
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
            journeyEndsAt = activeJourneyEndAt
        )

        container
            .eventRepository
            .add(
                AlarmEventType.TRIGGERED,
                detail
            )

        startAsForeground(
            phase = AlarmPhase.TRIGGERED,
            title = "⚠ ${activeMode.label}: allarme",
            text =
                "Apri l'app e autenticati per fermare l'allarme."
        )

        outputController.start(settings)
    }

    private fun scheduleJourneyEnd(
        endAt: Long?
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
        container.sessionStateRepository
            .saveJourneyEnd(endAt)

        val remaining =
            ProtectionPolicy
                .remainingJourneyMs(
                    endAt,
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

        outputController.stop()
        releaseWakeLock()

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
                "Ripristino ${persisted.mode.label}",
            text =
                "Ripristino protezione in corso."
        )

        acquireWakeLock()

        serviceScope.launch {
            val settings =
                container.settingsRepository
                    .settings.first()

            activeSettings = settings

            outputController.prepare(
                settings
            )

            when (
                SessionRecoveryPolicy
                    .recoveryAction(
                        persisted,
                        System.currentTimeMillis()
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
                    releaseWakeLock()
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
                            persisted.journeyEndsAt
                    )

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.TRIGGERED,
                            mode = persisted.mode,
                            message =
                                "Allarme ripristinato dopo interruzione.",
                            confidenceScore = 100,
                            journeyEndsAt =
                                persisted.journeyEndsAt,
                            triggeredAt =
                                persisted.updatedAtWallClock
                        )
                    )

                    outputController.start(settings)
                }

                RecoveryAction.RESTORE_TABLE -> {
                    startTableMonitoring(
                        settings = settings,
                        initialCalibration =
                            persisted.tableCalibration,
                        recordArmedEvent = false
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
            "Errore protezione",
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
                "Phone Guard protection",
                NotificationManager
                    .IMPORTANCE_LOW
            ).apply {
                description =
                    "Mostrata mentre una modalità Phone Guard è attiva."
                setSound(null, null)
                enableVibration(false)
            }

        val alarm =
            NotificationChannel(
                CHANNEL_ALARM,
                "Phone Guard alerts",
                NotificationManager
                    .IMPORTANCE_HIGH
            ).apply {
                description =
                    "Owner check e allarmi antifurto."
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

    private fun acquireWakeLock() {
        if (
            wakeLock?.isHeld == true
        ) {
            return
        }

        val powerManager =
            getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        wakeLock =
            powerManager.newWakeLock(
                PowerManager
                    .PARTIAL_WAKE_LOCK,
                "PhoneGuard:ProtectionMonitoring"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                runCatching {
                    it.release()
                }
            }
        }

        wakeLock = null
    }

    private fun armingInstruction(
        mode: ProtectionMode
    ): String =
        when (mode) {
            ProtectionMode.TABLE ->
                "Appoggia il telefono e non muoverlo."

            ProtectionMode.POCKET ->
                "Prepara il telefono per la tasca."

            ProtectionMode.BAG ->
                "Prepara il telefono nella borsa o nello zaino."

            ProtectionMode.JOURNEY ->
                "Preparazione Journey Guard."
        }

    private fun calibrationMessage(
        mode: ProtectionMode
    ): String =
        when (mode) {
            ProtectionMode.TABLE ->
                "Non muovere il telefono."

            ProtectionMode.POCKET ->
                "Calibrazione ambiente Pocket…"

            ProtectionMode.BAG ->
                "Calibrazione ambiente Bag…"

            ProtectionMode.JOURNEY ->
                "Calibrazione Journey Guard…"
        }

    private fun waitingMessage(
        mode: ProtectionMode,
        guardMode: GuardMode
    ): String =
        when (mode) {
            ProtectionMode.POCKET ->
                "Inserisci il telefono in tasca: si armerà automaticamente."

            ProtectionMode.BAG ->
                "Metti il telefono nella borsa: si armerà automaticamente."

            ProtectionMode.JOURNEY ->
                if (guardMode == GuardMode.POCKET) {
                    "Journey attivo: in attesa della tasca."
                } else {
                    "Journey attivo: in attesa della borsa."
                }

            ProtectionMode.TABLE ->
                "Protezione Table attiva."
        }

    override fun onDestroy() {
        armingJob?.cancel()
        ownerCheckJob?.cancel()
        journeyJob?.cancel()
        tableDetector?.stop()
        pocketController.stop()
        outputController.stop()
        releaseWakeLock()

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

        private const val OWNER_GRACE_MS =
            8_000L

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
