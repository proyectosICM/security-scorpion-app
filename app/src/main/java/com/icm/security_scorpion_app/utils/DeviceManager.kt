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
import androidx.core.content.ContextCompat
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

class DeviceManager(private val context: Context, private val llDevicesContent: LinearLayout?) {

    private var connectionManager: ESP32ConnectionManager? = null
    private lateinit var deviceAdapter: DeviceAdapter

    fun loadAndDisplayDevices() {
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(context)
        Log.d("DeviceManager", "Devices loaded: $devices")

        llDevicesContent?.removeAllViews()

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
            val currentIp = NetworkUtils.getRouterIpAddress(context)
            val deviceIp = tvDeviceIp.text.toString()
 
            fun getSubnet(ip: String): String {
                return ip.substringBeforeLast(".") // Obtiene los primeros 3 octetos (Ej: "192.168.1")
            }

            btnAction.setOnClickListener {
                connectionManager?.disconnect()

                // Verifica el estado actual del botón
                val isActivated = btnAction.tag as? Boolean ?: false

                // Nueva función para cambiar el estado del botón
                fun updateButtonState(activated: Boolean) {
                    if (activated) {
                        btnAction.setBackgroundColor(ContextCompat.getColor(context, R.color.green_light)) // Verde
                        btnAction.tag = true
                    } else {
                        btnAction.setBackgroundResource(R.drawable.border) // Color original
                        btnAction.tag = false
                    }
                }

                if (getSubnet(currentIp) == getSubnet(deviceIp)) {
                    connectionManager = ESP32ConnectionManager(deviceIp, GlobalSettings.SOCKET_LOCAL_PORT)
                    connectionManager?.connect { isConnected ->
                        if (isConnected) {
                            if (isActivated) {
                                // Enviar mensaje de DESACTIVACIÓN
                                connectionManager?.sendMessage(GlobalSettings.MESSAGE_DEACTIVATE)
                                Toast.makeText(context, "Dispositivo Desactivado Localmente", Toast.LENGTH_SHORT).show()
                            } else {
                                // Enviar mensaje de ACTIVACIÓN
                                connectionManager?.sendMessage(GlobalSettings.MESSAGE_ACTIVATE)
                                Toast.makeText(context, "Dispositivo Activado Localmente", Toast.LENGTH_SHORT).show()
                            }
                            updateButtonState(!isActivated)
                        } else {
                            if (isActivated) {
                                // Enviar mensaje de DESACTIVACIÓN remota
                                deviceAdapter.sendMessageToWebSocket("deactivate:${device.id}")
                                Toast.makeText(context, "Dispositivo Desactivado Remotamente", Toast.LENGTH_SHORT).show()
                            } else {
                                // Enviar mensaje de ACTIVACIÓN remota
                                deviceAdapter.sendMessageToWebSocket("activate:${device.id}")
                                Toast.makeText(context, "Dispositivo Activado Remotamente", Toast.LENGTH_SHORT).show()
                            }
                            updateButtonState(!isActivated)
                        }
                    }
                } else {
                    if (isActivated) {
                        deviceAdapter.sendMessageToWebSocket("deactivate:${device.id}")
                        Toast.makeText(context, "Dispositivo Desactivado Remotamente (IP Diferente)", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceAdapter.sendMessageToWebSocket("activate:${device.id}")
                        Toast.makeText(context, "Dispositivo Activado Remotamente (IP Diferente)", Toast.LENGTH_SHORT).show()
                    }
                    updateButtonState(!isActivated)
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

            llDevicesContent?.addView(view)
        }
        deviceAdapter = DeviceAdapter(context, devices, llDevicesContent )
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
                            GlobalSettings.username = username
                            GlobalSettings.password = password
                            Log.d("DeviceManager", "Credenciales guardadas: ${GlobalSettings.username}, ${GlobalSettings.password}")

                            devices[0].deviceGroupModel?.let {
                                GlobalSettings.groupId = it.id
                            } ?: Log.e("DeviceManager(s)", "deviceGroupModel es nulo, no se asignó Group ID")
                            Log.d("DeviceManager(s)", "Group ID Saved: ${GlobalSettings.groupId}")

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
