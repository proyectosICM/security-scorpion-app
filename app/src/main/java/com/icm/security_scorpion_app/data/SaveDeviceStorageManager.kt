package com.icm.security_scorpion_app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SaveDeviceStorageManager {

    companion object {
        private const val FILE_NAME = "devices.json"

        fun saveDevicesToJson(context: Context, devices: List<DeviceModel>) {
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

        fun loadDevicesFromJson(context: Context): List<DeviceModel> {
            val gson = Gson()
            val deviceListType = object : TypeToken<List<DeviceModel>>() {}.type
            val devices = mutableListOf<DeviceModel>()

            try {
                val inputStream = context.openFileInput(FILE_NAME)
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val json = bufferedReader.readText()
                devices.addAll(gson.fromJson(json, deviceListType))
                bufferedReader.close()
                Log.d("SaveDeviceStorageManager", "JSON: $json")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return devices
        }
    }
}
