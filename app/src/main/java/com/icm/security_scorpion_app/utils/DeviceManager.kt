package com.icm.security_scorpion_app.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.icm.security_scorpion_app.DeviceAdapter
import com.icm.security_scorpion_app.R
import com.icm.security_scorpion_app.data.DeleteDeviceStorageManager
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.LoadDeviceStorageManager
import com.icm.security_scorpion_app.data.SaveDeviceStorageManager
import com.icm.security_scorpion_app.data.api.JsonPlaceHolderApi
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.DialogUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DeviceManager(private val context: Context, private val llDevicesContent: LinearLayout) {

    private var connectionManager: ESP32ConnectionManager? = null
    private lateinit var deviceAdapter: DeviceAdapter

    fun loadAndDisplayDevices() {
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(context)
        Log.d("DeviceManager", "Devices loaded: $devices")

        llDevicesContent.removeAllViews()

        val inflater = LayoutInflater.from(context)
        for (device in devices) {
            val view = inflater.inflate(R.layout.device_item, llDevicesContent, false)
            val tvDeviceName = view.findViewById<TextView>(R.id.tvDeviceName)
            val tvDeviceIp = view.findViewById<TextView>(R.id.tvDeviceIp)
            val btnAction = view.findViewById<Button>(R.id.btnAction)
            val btnDeleteDevice = view.findViewById<Button>(R.id.btnDeleteDevice)

            tvDeviceName.text = device.nameDevice
            tvDeviceIp.text = device.ipLocal

            btnAction.setOnClickListener {
                connectionManager?.disconnect()
                connectionManager = ESP32ConnectionManager(tvDeviceIp.text.toString(), 82)
                connectionManager?.connect { isConnected ->
                    if (isConnected) {
                        connectionManager?.sendMessage("activate")
                        Toast.makeText(context, "Dispositivo Activado Localmente", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceAdapter.sendMessageToWebSocket("${device.nameDevice}:Activating")
                    }
                }
            }

            btnDeleteDevice.setOnClickListener {
                DialogUtils.showDeleteConfirmationDialog(context, device.nameDevice) {
                    val result = DeleteDeviceStorageManager.deleteDeviceFromJson(context, device.nameDevice)
                    if (result) {
                        loadAndDisplayDevices()
                    } else {
                        Log.d("DeviceDeletion", "No se pudo eliminar el dispositivo")
                    }
                }
            }

            llDevicesContent.addView(view)
        }
        deviceAdapter = DeviceAdapter(context, devices)
    }

    fun fetchDevicesFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://samloto.com:4015/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi::class.java)
        val call = jsonPlaceHolderApi.getDevices()

        call.enqueue(object : Callback<List<DeviceModel>> {
            override fun onResponse(call: Call<List<DeviceModel>>, response: Response<List<DeviceModel>>) {
                if (!response.isSuccessful) {
                    Log.e("DeviceManager", "Response not successful: ${response.errorBody()?.string()}")
                    DialogUtils.showConnectionErrorDialog(context)
                    return
                }

                val devices = response.body()
                if (devices != null) {
                    SaveDeviceStorageManager.saveDevicesToJson(context, emptyList())
                    SaveDeviceStorageManager.saveDevicesToJson(context, devices)
                    loadAndDisplayDevices()
                    val jsonResponse = Gson().toJson(devices)
                    Log.d("DeviceManager", "Devices loaded: $jsonResponse")
                } else {
                    Log.e("DeviceManager", "No devices received")
                    DialogUtils.showConnectionErrorDialog(context)
                }
            }

            override fun onFailure(call: Call<List<DeviceModel>>, t: Throwable) {
                Log.e("DeviceManager", "API call failed: ${t.message}")
                DialogUtils.showConnectionErrorDialog(context)
            }
        })
    }
}
