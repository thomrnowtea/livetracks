package com.thomrnowtea.livetracks.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: String,
    val channelCounts: List<Int>,
    val sampleRates: List<Int>,
    val encodings: List<Int>,
)

class AudioHardwareMonitor(
    context: Context,
    private val onChanged: (List<AudioOutputDevice>) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = publish()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = publish()
    }

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, handler)
        publish()
    }

    fun stop() {
        audioManager.unregisterAudioDeviceCallback(callback)
    }

    fun nativeOutputSampleRate(): Int = audioManager
        .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        ?.toIntOrNull()
        ?: 0

    private fun publish() {
        onChanged(
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { device ->
                AudioOutputDevice(
                    id = device.id,
                    name = device.productName?.toString().orEmpty().ifBlank { "Android output" },
                    type = typeName(device.type),
                    channelCounts = device.channelCounts.toList(),
                    sampleRates = device.sampleRates.toList(),
                    encodings = device.encodings.toList(),
                )
            },
        )
    }

    private fun typeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset/DAC"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP (preview only)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE (preview only)"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        else -> "Android device type $type"
    }
}

