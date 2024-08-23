package com.icm.security_scorpion_app.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.icm.security_scorpion_app.DeviceAdapter
import com.icm.security_scorpion_app.EditDeviceActivity
import com.icm.security_scorpion_app.R
import com.icm.security_scorpion_app.data.DeleteDeviceStorageManager
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.LoadDeviceStorageManager
import com.icm.security_scorpion_app.data.SaveDeviceStorageManager
import com.icm.security_scorpion_app.data.api.JsonPlaceHolderApi
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
            val tvDeviceId = view.findViewById<TextView>(R.id.tvDeviceId) // Añadir esta línea
            val btnAction = view.findViewById<Button>(R.id.btnAction)
            val btnEditDevice = view.findViewById<ImageView>(R.id.btnEditDevice)

            tvDeviceName.text = device.nameDevice
            tvDeviceIp.text = device.ipLocal
            tvDeviceId.text = device.id.toString()

            btnAction.setOnClickListener {
                connectionManager?.disconnect()
                connectionManager = ESP32ConnectionManager(tvDeviceIp.text.toString(), GlobalSettings.SOCKET_LOCAL_PORT)
                connectionManager?.connect { isConnected ->
                    if (isConnected) {
                        connectionManager?.sendMessage(GlobalSettings.MESSAGE_ACTIVATE)
                        Toast.makeText(context, "Dispositivo Activado Localmente", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceAdapter.sendMessageToWebSocket("${device.nameDevice}:Activating")
                    }
                }
            }

            btnEditDevice.setOnClickListener {
                val intent = Intent(context, EditDeviceActivity::class.java).apply {
                    putExtra("deviceName", device.nameDevice)
                    putExtra("deviceIp", device.ipLocal)
                    putExtra("deviceId", device.id.toString())
                }
                context.startActivity(intent)
            }

            llDevicesContent.addView(view)
        }
        deviceAdapter = DeviceAdapter(context, devices)
    }

    fun fetchDevicesFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl(GlobalSettings.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi::class.java)
        val call = jsonPlaceHolderApi.getDevices()

        call.enqueue(object : Callback<List<DeviceModel>> {
            override fun onResponse(call: Call<List<DeviceModel>>, response: Response<List<DeviceModel>>) {
                if (!response.isSuccessful) {
                    Log.e("DeviceManager(s)", "Response not successful: ${response.errorBody()?.string()}")
                    DialogUtils.showConnectionErrorDialog(context)
                    return
                }

                val devices = response.body()
                if (devices != null) {
                    SaveDeviceStorageManager.saveDevicesToJson(context, emptyList())
                    SaveDeviceStorageManager.saveDevicesToJson(context, devices)
                    loadAndDisplayDevices()
                    val jsonResponse = Gson().toJson(devices)
                    Log.d("DeviceManager (s)", "Devices loaded: $jsonResponse")
                } else {
                    Log.e("DeviceManager (s)", "No devices received")
                    DialogUtils.showConnectionErrorDialog(context)
                }
            }

            override fun onFailure(call: Call<List<DeviceModel>>, t: Throwable) {
                Log.e("DeviceManager", "API call failed: ${t.message}")
                DialogUtils.showConnectionErrorDialog(context)
            }
        })
    }

    fun fetchDevicesFromServerAuth(username: String, password: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(GlobalSettings.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi::class.java)
        val call = jsonPlaceHolderApi.getDevicesByAuth(username, password)

        call.enqueue(object : Callback<List<DeviceModel>> {
            override fun onResponse(call: Call<List<DeviceModel>>, response: Response<List<DeviceModel>>) {
                when (response.code()) {
                    // Código 200 OK: respuesta exitosa
                    200 -> {
                        val devices = response.body()
                        if (devices != null) {
                            SaveDeviceStorageManager.saveDevicesToJson(context, devices)
                            loadAndDisplayDevices()
                            val jsonResponse = Gson().toJson(devices)
                            Log.d("DeviceManager(s)", "Devices loaded: $jsonResponse")
                        } else {
                            Log.e("DeviceManager(s)", "No devices received")
                            DialogUtils.showConnectionErrorDialog(context)
                        }
                    }
                    // Código 401 Unauthorized: usuario o contraseña incorrectos
                    401 -> {
                        Log.e("DeviceManager(s)", "Unauthorized: ${response.errorBody()?.string()}")
                        DialogUtils.showInvalidCredentialsDialog(context)
                    }
                    // Código 403 Forbidden: grupo no activo
                    403 -> {
                        Log.e("DeviceManager(s)", "Forbidden: ${response.errorBody()?.string()}")
                        DialogUtils.showGroupNotActiveDialog(context)
                    }
                    // Otros códigos de error
                    else -> {
                        Log.e("DeviceManager(s)", "Response not successful: ${response.code()} ${response.errorBody()?.string()}")
                        DialogUtils.showConnectionErrorDialog(context)
                    }
                }
            }

            override fun onFailure(call: Call<List<DeviceModel>>, t: Throwable) {
                Log.e("DeviceManager(s)", "API call failed: ${t.message}")
                DialogUtils.showConnectionErrorDialog(context)
            }
        })
    }
}
