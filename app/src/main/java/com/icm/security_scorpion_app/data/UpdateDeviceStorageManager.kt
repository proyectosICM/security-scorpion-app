package com.icm.security_scorpion_app.data

import android.content.Context
import com.google.gson.Gson
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class UpdateDeviceStorageManager {

    companion object {
        private const val FILE_NAME = "devices.json"

        fun updateDeviceInJson(context: Context, updatedDevice: DeviceModel): Boolean {
            val devices = LoadDeviceStorageManager.loadDevicesFromJson(context).toMutableList()
            val deviceIndex = devices.indexOfFirst { it.id  == updatedDevice.id  }

            return if (deviceIndex != -1) {
                devices[deviceIndex] = updatedDevice
                saveDevicesToJson(context, devices)
                true
            } else {
                false
            }
        }

        private fun saveDevicesToJson(context: Context, devices: List<DeviceModel>) {
            val gson = Gson()
            val json = gson.toJson(devices)

            try {
                val fileOutputStream: FileOutputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
                val outputStreamWriter = OutputStreamWriter(fileOutputStream)
                outputStreamWriter.write(json)
                outputStreamWriter.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
