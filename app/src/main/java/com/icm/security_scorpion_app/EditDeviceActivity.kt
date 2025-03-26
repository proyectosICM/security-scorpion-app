package com.icm.security_scorpion_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.icm.security_scorpion_app.data.DeleteDeviceStorageManager
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.UpdateDeviceStorageManager
import com.icm.security_scorpion_app.data.api.DeviceUpdateRequest
import com.icm.security_scorpion_app.data.api.JsonPlaceHolderApi
import com.icm.security_scorpion_app.utils.DialogUtils
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.GlobalSettings
import com.icm.security_scorpion_app.utils.NetworkUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EditDeviceActivity : AppCompatActivity() {

    private var connectionManager: ESP32ConnectionManager? = null
    private lateinit var jsonPlaceHolderApi: JsonPlaceHolderApi
    private lateinit var deviceAdapter: DeviceAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        // Configurar Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://samloto.com:4015/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi::class.java)

        // Recuperar los datos del intent
        val deviceName = intent.getStringExtra("deviceName")
        val deviceIp = intent.getStringExtra("deviceIp")
        val deviceId = intent.getStringExtra("deviceId")

        // Referenciar los EditText desde el layout
        val deviceNameEditText = findViewById<EditText>(R.id.deviceNameEditText)
        val deviceIpEditText = findViewById<EditText>(R.id.deviceIpEditText)
        val btnDeleteDevice = findViewById<Button>(R.id.btnDeleteDevice)
        val btnSave = findViewById<Button>(R.id.saveButton)
        val btnChangeWifi = findViewById<Button>(R.id.btnChangeWifi)
        val deviceIdTextView = findViewById<TextView>(R.id.deviceIdTextView)

        // Cargar los datos en los EditText
        deviceNameEditText.setText(deviceName)
        deviceIpEditText.setText(deviceIp)
        deviceIdTextView.text = deviceId ?: "22"

        // Configurar listeners de botones
        btnSave.setOnClickListener { handleSave(deviceName, deviceIp, deviceIdTextView.text.toString(), deviceNameEditText.text.toString(), deviceIpEditText.text.toString()) }
        btnDeleteDevice.setOnClickListener { handleDelete(deviceName) }
        btnChangeWifi.setOnClickListener{handleChangeRed()}
    }

    private fun handleSave(originalDeviceName: String?, originalDeviceIp: String?, deviceId: String, newDeviceName: String, newDeviceIp: String) {
        if (newDeviceName.isEmpty() || newDeviceIp.isEmpty()) {
            Log.d("DeviceEdit", "Nombre o IP del dispositivo está vacío.")
            return
        }

        val routerIp = NetworkUtils.getRouterIpAddress(this)
        if (!NetworkUtils.isIpInSameNetwork(newDeviceIp, routerIp)) {
            Log.d("DeviceEdit", "IP no válida dentro de la red.")
            return
        }

        val actualDeviceIp = originalDeviceIp ?: run {
            Log.e("DeviceEdit", "IP del dispositivo es null")
            return
        }

        connectionManager = ESP32ConnectionManager(actualDeviceIp, GlobalSettings.SOCKET_LOCAL_PORT)
        connectionManager?.connect { isConnected ->
            if (isConnected) {
                sendConfigToDevice(newDeviceName, newDeviceIp)
                if (updateLocalJson(deviceId, newDeviceName, newDeviceIp)) {
                    sendUpdateRequestToServer(deviceId, newDeviceName, newDeviceIp)
                    finish()
                }
            } else {
                Log.d("DeviceEdit", "No se pudo conectar al ESP32.")
            }
        }
    }

    private fun handleDelete(deviceName: String?) {
        deviceName?.let {
            DialogUtils.showDeleteConfirmationDialog(this, it) {
                if (DeleteDeviceStorageManager.deleteDeviceFromJson(this, it)) {
                    finish()
                } else {
                    Log.d("DeviceDeletion", "No se pudo eliminar el dispositivo")
                }
            }
        } ?: run {
            Log.e("DeviceDeletion", "deviceName es null")
        }
    }

    private fun handleChangeRed(){
        val intent = Intent(this, ConfigureDeviceActivity::class.java)

        // Obtener la IP actual del dispositivo desde el campo de texto
        val ipList = arrayListOf(findViewById<EditText>(R.id.deviceIpEditText).text.toString())

        // Enviar la IP del dispositivo al Intent
        intent.putExtra("device_ips", ipList)

        startActivity(intent)
    }

    private fun sendConfigToDevice(newDeviceName: String, newDeviceIp: String) {
        connectionManager?.sendMessage("editConfig:setName:$newDeviceName;setIp:$newDeviceIp")

        //deviceAdapter.sendMessageToWebSocket("editConfig:setName:$newDeviceName;setIp:$newDeviceIp")
        Log.d("DeviceEdit", "Datos enviados al ESP32 exitosamente.")
    }

    private fun updateLocalJson(deviceId: String, newDeviceName: String, newDeviceIp: String): Boolean {
        val result = UpdateDeviceStorageManager.updateDeviceInJson(
            this,
            DeviceModel(deviceId.toLong(), newDeviceName, newDeviceIp)
        )
        if (!result) {
            Log.d("DeviceEdit", "Error al actualizar los datos en el JSON local.")
        }
        return result
    }

    private fun sendUpdateRequestToServer(deviceId: String, newDeviceName: String, newDeviceIp: String) {
        val deviceUpdateRequest = DeviceUpdateRequest(newDeviceName, newDeviceIp)
        val call = jsonPlaceHolderApi.updateDevice(deviceId, deviceUpdateRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("DeviceEdit", "Datos actualizados en el servidor exitosamente.")
                } else {
                    Log.d("DeviceEdit", "Error al actualizar los datos en el servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("DeviceEdit", "Fallo la conexión con el servidor: ${t.message}")
            }
        })
    }
}
