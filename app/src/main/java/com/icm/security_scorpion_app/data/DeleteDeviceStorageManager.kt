package com.icm.security_scorpion_app.data

import android.content.Context

class DeleteDeviceStorageManager {
    companion object {
        fun deleteDeviceFromJson(context: Context, deviceName: String): Boolean {
            val devices = LoadDeviceStorageManager.loadDevicesFromJson(context).toMutableList()
            val deviceToRemove = devices.find { it.nameDevice == deviceName }
            return if (deviceToRemove != null) {
                devices.remove(deviceToRemove)
                SaveDeviceStorageManager.saveDevicesToJson(context, devices)
                true
            } else {
                false
            }
        }
    }
}