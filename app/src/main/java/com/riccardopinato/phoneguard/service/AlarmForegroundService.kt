// TARGET: app/src/main/java/com/riccardopinato/tabletopalarm/service/AlarmForegroundService.kt
package com.riccardopinato.phoneguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.riccardopinato.phoneguard.MainActivity
import com.riccardopinato.phoneguard.R
import com.riccardopinato.phoneguard.PhoneGuardApp
import com.riccardopinato.phoneguard.model.AlarmEventType
import com.riccardopinato.phoneguard.model.AppSettings
import com.riccardopinato.phoneguard.sensor.MotionCalibration
import com.riccardopinato.phoneguard.sensor.MotionDetector
import com.riccardopinato.phoneguard.sensor.MotionTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmForegroundService : Service() {

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main.immediate
        )

    private lateinit var outputController:
        AlarmOutputController

    private lateinit var notificationManager:
        NotificationManager

    private val container by lazy {
        (application as PhoneGuardApp)
            .container
    }

    private var armingJob: Job? = null
    private var motionDetector:
        MotionDetector? = null
    private var wakeLock:
        PowerManager.WakeLock? = null
    private var activeSettings:
        AppSettings? = null

    override fun onCreate() {
        super.onCreate()

        outputController =
            AlarmOutputController(
                applicationContext
            )

        notificationManager =
            getSystemService(
                NotificationManager::class.java
            )

        createNotificationChannels()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_ARM -> beginArming()
            ACTION_DISARM -> disarm()
            null -> restorePersistedSession()
        }

        return START_STICKY
    }

    private fun beginArming() {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.DISARMED
        ) {
            return
        }

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.ARMING,
                countdownSeconds = 0,
                message = "Preparazione allarme…"
            )
        )

        container.sessionStateRepository.savePhase(AlarmPhase.ARMING)

        startAsForeground(
            phase = AlarmPhase.ARMING,
            title = "Preparazione allarme",
            text = "Appoggia il telefono e non muoverlo."
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

                container.eventRepository.add(
                    AlarmEventType
                        .ARMING_STARTED,
                    "Countdown " +
                        "${settings.countdownSeconds}s - " +
                        "sensibilità " +
                        settings.sensitivity.label
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
                            phase =
                                AlarmPhase.ARMING,
                            countdownSeconds =
                                remaining,
                            message =
                                "Appoggia il telefono. " +
                                "Attivazione in " +
                                "${remaining}s"
                        )
                    )

                    updateNotification(
                        AlarmPhase.ARMING,
                        "Preparazione allarme",
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

                AlarmStateStore.update(
                    AlarmRuntimeState(
                        phase =
                            AlarmPhase
                                .CALIBRATING,
                        message =
                            "Calibrazione sensori… " +
                            "non muovere il telefono."
                    )
                )

                container.sessionStateRepository.savePhase(AlarmPhase.CALIBRATING)

                updateNotification(
                    AlarmPhase.CALIBRATING,
                    "Calibrazione",
                    "Non muovere il telefono."
                )

                startMotionMonitoring(
                    settings
                )
            }
    }

    private fun startMotionMonitoring(
        settings: AppSettings,
        initialCalibration:
            MotionCalibration? = null,
        recordArmedEvent:
            Boolean = true
    ) {
        motionDetector?.stop()

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

                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase =
                                AlarmPhase.ARMED,
                            message =
                                "Protezione attiva. " +
                                "Non muovere il dispositivo."
                        )
                    )

                    container.sessionStateRepository.savePhase(AlarmPhase.ARMED)
                    container.sessionStateRepository.saveCalibration(calibration)

                    if (recordArmedEvent) {
                        container
                            .eventRepository
                            .add(
                                AlarmEventType.ARMED,
                                "Noise floor " +
                                    String.format(
                                        Locale.US,
                                        "%.3f m/s²",
                                        calibration.noiseFloor
                                    )
                            )
                    }

                    updateNotification(
                        AlarmPhase.ARMED,
                        "Protezione attiva",
                        "Il movimento del telefono " +
                            "farà scattare l'allarme."
                    )
                },
                onMovement = {
                    trigger ->
                    triggerAlarm(trigger)
                }
            )

        motionDetector = detector

        if (!detector.start()) {
            container
                .eventRepository
                .add(
                    AlarmEventType.ERROR,
                    "Accelerometro non disponibile."
                )

            AlarmStateStore.update(
                AlarmRuntimeState(
                    phase =
                        AlarmPhase.ERROR,
                    message =
                        "Accelerometro non disponibile."
                )
            )

            updateNotification(
                AlarmPhase.ERROR,
                "Errore sensore",
                "Accelerometro non disponibile."
            )

            serviceScope.launch {
                delay(2_000)
                disarm()
            }
        }
    }

    private fun triggerAlarm(
        trigger: MotionTrigger
    ) {
        if (
            AlarmStateStore.state
                .value.phase !=
            AlarmPhase.ARMED
        ) {
            return
        }

        val settings =
            activeSettings ?: return

        AlarmStateStore.update(
            AlarmRuntimeState(
                phase =
                    AlarmPhase.TRIGGERED,
                message =
                    "Movimento rilevato!",
                lastMotionDelta =
                    trigger
                        .accelerationDelta,
                triggeredAt =
                    System.currentTimeMillis()
            )
        )

        container.sessionStateRepository.savePhase(AlarmPhase.TRIGGERED)

        container
            .eventRepository
            .add(
                AlarmEventType.TRIGGERED,
                "Accel=" +
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
                    " rad/s"
            )

        startAsForeground(
            phase =
                AlarmPhase.TRIGGERED,
            title =
                "⚠ Movimento rilevato",
            text =
                "Apri l'app e autenticati " +
                "per fermare l'allarme."
        )

        outputController.start(settings)
    }

    private fun disarm() {
        val previous =
            AlarmStateStore.state
                .value.phase

        armingJob?.cancel()
        armingJob = null

        motionDetector?.stop()
        motionDetector = null

        outputController.stop()
        releaseWakeLock()

        when (previous) {
            AlarmPhase.ARMING,
            AlarmPhase.CALIBRATING ->
                container
                    .eventRepository
                    .add(
                        AlarmEventType.CANCELED,
                        "Armamento annullato."
                    )

            AlarmPhase.ARMED,
            AlarmPhase.TRIGGERED ->
                container
                    .eventRepository
                    .add(
                        AlarmEventType.DISARMED,
                        "Allarme disattivato."
                    )

            else -> Unit
        }

        container.sessionStateRepository.clear()
        activeSettings = null
        AlarmStateStore.reset()

        ServiceCompat.stopForeground(
            this,
            ServiceCompat
                .STOP_FOREGROUND_REMOVE
        )

        stopSelf()
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

        val channelId =
            if (
                phase ==
                AlarmPhase.TRIGGERED
            ) {
                CHANNEL_ALARM
            } else {
                CHANNEL_MONITOR
            }

        return NotificationCompat
            .Builder(
                this,
                channelId
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
                phase !=
                    AlarmPhase.TRIGGERED
            )
            .setCategory(
                if (
                    phase ==
                    AlarmPhase.TRIGGERED
                ) {
                    NotificationCompat
                        .CATEGORY_ALARM
                } else {
                    NotificationCompat
                        .CATEGORY_SERVICE
                }
            )
            .setPriority(
                if (
                    phase ==
                    AlarmPhase.TRIGGERED
                ) {
                    NotificationCompat
                        .PRIORITY_HIGH
                } else {
                    NotificationCompat
                        .PRIORITY_LOW
                }
            )
            .build()
    }

    private fun createNotificationChannels() {
        val monitor =
            NotificationChannel(
                CHANNEL_MONITOR,
                "Monitoraggio antifurto",
                NotificationManager
                    .IMPORTANCE_LOW
            ).apply {
                description =
                    "Mostrata mentre l'allarme " +
                    "da tavolo è armato."
                setSound(null, null)
                enableVibration(false)
            }

        val alarm =
            NotificationChannel(
                CHANNEL_ALARM,
                "Allarme movimento",
                NotificationManager
                    .IMPORTANCE_HIGH
            ).apply {
                description =
                    "Notifica quando viene " +
                    "rilevato movimento."
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
                "TabletopAlarm:" +
                    "MotionMonitoring"
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


    private fun restorePersistedSession() {
        val persisted =
            container.sessionStateRepository.read()
                ?: run {
                    stopSelf()
                    return
                }

        // START_STICKY recovery must enter foreground before suspend work.
        if (persisted.phase == AlarmPhase.TRIGGERED) {
            startAsForeground(
                phase = AlarmPhase.TRIGGERED,
                title = "⚠ Movimento rilevato",
                text = "Ripristino allarme. Apri l'app per disattivarlo."
            )
        } else {
            startAsForeground(
                phase = AlarmPhase.CALIBRATING,
                title = "Ripristino protezione",
                text = "Riattivazione monitoraggio sensori."
            )
        }

        acquireWakeLock()

        serviceScope.launch {
            val settings =
                container.settingsRepository.settings.first()

            activeSettings = settings

            when (persisted.phase) {
                AlarmPhase.TRIGGERED -> {
                    AlarmStateStore.update(
                        AlarmRuntimeState(
                            phase = AlarmPhase.TRIGGERED,
                            message = "Movimento rilevato!",
                            triggeredAt = persisted.updatedAtWallClock
                        )
                    )

                    outputController.start(settings)
                }

                AlarmPhase.ARMED -> {
                    val calibration = persisted.calibration

                    if (calibration != null) {
                        AlarmStateStore.update(
                            AlarmRuntimeState(
                                phase = AlarmPhase.CALIBRATING,
                                message = "Ripristino protezione…"
                            )
                        )

                        startMotionMonitoring(
                            settings = settings,
                            initialCalibration = calibration,
                            recordArmedEvent = false
                        )
                    } else {
                        restartWithFreshCalibration(settings)
                    }
                }

                AlarmPhase.ARMING,
                AlarmPhase.CALIBRATING -> {
                    restartWithFreshCalibration(settings)
                }

                AlarmPhase.DISARMED,
                AlarmPhase.ERROR -> {
                    container.sessionStateRepository.clear()
                    releaseWakeLock()
                    stopSelf()
                }
            }
        }
    }

    private fun restartWithFreshCalibration(
        settings: AppSettings
    ) {
        AlarmStateStore.update(
            AlarmRuntimeState(
                phase = AlarmPhase.CALIBRATING,
                message = "Ripristino protezione e ricalibrazione…"
            )
        )

        container.sessionStateRepository.savePhase(
            AlarmPhase.CALIBRATING
        )

        updateNotification(
            AlarmPhase.CALIBRATING,
            "Ripristino protezione",
            "Ricalibrazione sensori in corso."
        )

        startMotionMonitoring(
            settings = settings,
            recordArmedEvent = false
        )
    }

    override fun onDestroy() {
        armingJob?.cancel()
        motionDetector?.stop()
        outputController.stop()
        releaseWakeLock()
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

        private const val NOTIFICATION_ID =
            4101

        private const val CHANNEL_MONITOR =
            "tabletop_monitor"

        private const val CHANNEL_ALARM =
            "tabletop_alarm"

        fun arm(context: Context) {
            val intent =
                Intent(
                    context,
                    AlarmForegroundService::class.java
                ).setAction(
                    ACTION_ARM
                )

            ContextCompat
                .startForegroundService(
                    context,
                    intent
                )
        }

        fun disarm(context: Context) {
            val intent =
                Intent(
                    context,
                    AlarmForegroundService::class.java
                ).setAction(
                    ACTION_DISARM
                )

            // Il disarmo viene richiesto dall'Activity visibile mentre
            // il servizio è già attivo. Non avviare un nuovo FGS solo
            // per spegnerlo, altrimenti Android potrebbe richiedere una
            // nuova chiamata a startForeground().
            runCatching {
                context.startService(intent)
            }
        }
    }
}
