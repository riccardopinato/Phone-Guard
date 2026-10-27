package com.riccardopinato.phoneguard.accessory

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import java.util.Locale

data class ProtectedAccessory(
    val id: Int,
    val type: Int,
    val name: String
) {
    fun stableFingerprint():
        String =
        type.toString() +
            "|" +
            name
                .trim()
                .lowercase(
                    Locale.ROOT
                )

    fun recoveryIdentity():
        String =
        stableFingerprint() +
            "|id=" +
            id

class AccessoryStateMonitor(
    context: Context
) {
    private val audioManager =
        context.applicationContext
            .getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var callback:
        AudioDeviceCallback? = null

    private var protectedIds:
        Set<Int> = emptySet()

    fun connectedAccessories():
        List<ProtectedAccessory> =
        audioManager
            .getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )
            .filter {
                isProtectedType(
                    it.type
                )
            }
            .map {
                ProtectedAccessory(
                    id = it.id,
                    type = it.type,
                    name =
                        it.productName
                            ?.toString()
                            ?.takeIf { name ->
                                name.isNotBlank()
                            }
                            ?: typeLabel(
                                it.type
                            )
                )
            }

    fun hasConnectedAccessory():
        Boolean =
        connectedAccessories()
            .isNotEmpty()

    fun start(
        protectedFingerprints:
            Set<String>? = null,
        onProtectedAccessoryRemoved:
            (List<ProtectedAccessory>) -> Unit
    ): List<ProtectedAccessory> {
        stop()

        val connected =
            connectedAccessories()

        val initial =
            selectProtectedAccessories(
                connected,
                protectedFingerprints
            )

        protectedIds =
            initial
                .map {
                    it.id
                }
                .toSet()

        if (protectedIds.isEmpty()) {
            return emptyList()
        }

        val registered =
            object :
                AudioDeviceCallback() {
                override fun onAudioDevicesRemoved(
                    removedDevices:
                        Array<out AudioDeviceInfo>
                ) {
                    val removed =
                        removedDevices
                            .filter {
                                it.id in
                                    protectedIds
                            }
                            .map {
                                ProtectedAccessory(
                                    id = it.id,
                                    type = it.type,
                                    name =
                                        it.productName
                                            ?.toString()
                                            ?.takeIf { name ->
                                                name.isNotBlank()
                                            }
                                            ?: typeLabel(
                                                it.type
                                            )
                                )
                            }

                    if (
                        removed.isNotEmpty()
                    ) {
                        protectedIds =
                            protectedIds -
                                removed
                                    .map {
                                        it.id
                                    }
                                    .toSet()

                        onProtectedAccessoryRemoved(
                            removed
                        )
                    }
                }
            }

        callback =
            registered

        audioManager
            .registerAudioDeviceCallback(
                registered,
                mainHandler
            )

        return initial
    }

    fun stop() {
        callback?.let {
            runCatching {
                audioManager
                    .unregisterAudioDeviceCallback(
                        it
                    )
            }
        }

        callback = null
        protectedIds =
            emptySet()
    }

    companion object {
        fun selectProtectedAccessories(
            connected: List<ProtectedAccessory>,
            protectedFingerprints:
                Set<String>?
        ): List<ProtectedAccessory> =
            if (
                protectedFingerprints
                    .isNullOrEmpty()
            ) {
                connected
            } else {
                connected
                    .filter {
                        it.recoveryIdentity() in
                            protectedFingerprints
                    }
            }

        fun isProtectedType(
            type: Int
        ): Boolean =
            type in
                PROTECTED_TYPES

        fun typeLabel(
            type: Int
        ): String =
            when (type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                    "wired-headset"

                AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                    "wired-headphones"

                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
                    "bluetooth-audio"

                AudioDeviceInfo.TYPE_USB_DEVICE ->
                    "usb-audio"

                AudioDeviceInfo.TYPE_USB_ACCESSORY ->
                    "usb-accessory"

                AudioDeviceInfo.TYPE_USB_HEADSET ->
                    "usb-headset"

                AudioDeviceInfo.TYPE_HEARING_AID ->
                    "hearing-aid"

                AudioDeviceInfo.TYPE_BLE_HEADSET ->
                    "ble-headset"

                AudioDeviceInfo.TYPE_BLE_SPEAKER ->
                    "ble-speaker"

                else ->
                    "audio-accessory"
            }

        private val PROTECTED_TYPES =
            setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER
            )
    }
}
