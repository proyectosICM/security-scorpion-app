package com.icm.security_scorpion_app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader

class LoadDeviceStorageManager {

    companion object {
        private const val FILE_NAME = "devices.json"

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
                Log.d("LoadDeviceStorageManager", "JSON-2: $json")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return devices
        }
    }
}
