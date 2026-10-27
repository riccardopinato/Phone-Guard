package com.riccardopinato.phoneguard.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

/**
 * Continuous alarm strobe adapted from the proven Luminous Circle controller.
 * A generation token guarantees stale tasks cannot re-enable the torch.
 */
class FlashStrobeController(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val tag = "FlashStrobeController"
    private val cameraManager =
        appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor(
            ThreadFactory { Thread(it, "PhoneGuard-Strobe") }
        )

    private val running = AtomicBoolean(false)
    private val generation = AtomicLong(0)
    private var currentTask: Future<*>? = null
    private var cameraId: String? = findBackFlashCamera()
    private var closed = false

    @Synchronized
    fun startContinuous(
        frequencyHz: Float
    ): Boolean {
        if (
            closed ||
            cameraId == null ||
            !hasCameraPermission()
        ) {
            return false
        }

        val safeHz = frequencyHz.coerceIn(MIN_HZ, MAX_HZ)
        val halfPeriodMs =
            (500f / safeHz)
                .roundToLong()
                .coerceIn(MIN_PHASE_MS, MAX_PHASE_MS)

        stopLocked()

        if (!setTorch(cameraId, true)) {
            running.set(false)
            return false
        }

        val myGeneration =
            generation.incrementAndGet()

        running.set(true)

        return try {
            currentTask = executor.submit {
                try {
                    if (
                        !sleepInterruptibly(
                            halfPeriodMs,
                            myGeneration
                        )
                    ) {
                        return@submit
                    }

                    while (
                        isCurrent(
                            myGeneration
                        )
                    ) {
                        if (
                            !setTorch(
                                cameraId,
                                false
                            )
                        ) {
                            break
                        }

                        if (
                            !sleepInterruptibly(
                                halfPeriodMs,
                                myGeneration
                            )
                        ) {
                            break
                        }

                        if (
                            !setTorch(
                                cameraId,
                                true
                            )
                        ) {
                            break
                        }

                        if (
                            !sleepInterruptibly(
                                halfPeriodMs,
                                myGeneration
                            )
                        ) {
                            break
                        }
                    }
                } catch (
                    _: InterruptedException
                ) {
                    Thread.currentThread()
                        .interrupt()
                } finally {
                    setTorch(
                        cameraId,
                        false
                    )

                    if (
                        generation.get() ==
                        myGeneration
                    ) {
                        running.set(false)
                    }
                }
            }

            true
        } catch (
            _: RejectedExecutionException
        ) {
            running.set(false)
            setTorch(
                cameraId,
                false
            )
            false
        }
    }

    @Synchronized
    fun stop() {
        if (closed) return
        stopLocked()
        generation.incrementAndGet()
        running.set(false)
        try {
            executor.execute { setTorch(cameraId, false) }
        } catch (_: RejectedExecutionException) {
            setTorch(cameraId, false)
        }
    }

    fun isAvailable(): Boolean =
        cameraId != null &&
            !closed &&
            hasCameraPermission()

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        stopLocked()
        generation.incrementAndGet()
        running.set(false)
        setTorch(cameraId, false)
        executor.shutdownNow()
    }

    @Synchronized
    private fun stopLocked() {
        currentTask?.cancel(true)
        currentTask = null
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun isCurrent(myGeneration: Long): Boolean =
        running.get() &&
            generation.get() == myGeneration &&
            !Thread.currentThread().isInterrupted

    private fun sleepInterruptibly(
        ms: Long,
        myGeneration: Long
    ): Boolean {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return false
        }
        return isCurrent(myGeneration)
    }

    private fun findBackFlashCamera(): String? =
        try {
            cameraManager
                ?.cameraIdList
                ?.firstOrNull { id ->
                    val characteristics =
                        cameraManager.getCameraCharacteristics(id)
                    characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true &&
                        characteristics.get(
                            CameraCharacteristics.LENS_FACING
                        ) == CameraCharacteristics.LENS_FACING_BACK
                }
                ?: cameraManager
                    ?.cameraIdList
                    ?.firstOrNull { id ->
                        cameraManager
                            .getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
        } catch (e: Exception) {
            Log.w(tag, "Unable to detect flash camera", e)
            null
        }

    private fun setTorch(
        id: String?,
        enabled: Boolean
    ): Boolean {
        if (
            id == null ||
            cameraManager == null
        ) {
            return false
        }

        return try {
            cameraManager.setTorchMode(
                id,
                enabled
            )
            true
        } catch (
            _: CameraAccessException
        ) {
            Log.d(
                tag,
                "Camera busy or unavailable"
            )
            false
        } catch (
            _: SecurityException
        ) {
            Log.d(
                tag,
                "Camera permission unavailable"
            )
            false
        } catch (
            _: IllegalArgumentException
        ) {
            Log.d(
                tag,
                "Invalid camera id"
            )
            false
        }
    }

    private companion object {
        const val MIN_HZ = 0.5f
        const val MAX_HZ = 3f
        const val MIN_PHASE_MS = 160L
        const val MAX_PHASE_MS = 1_000L
    }
}
