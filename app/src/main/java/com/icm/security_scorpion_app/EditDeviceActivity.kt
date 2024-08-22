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
import com.icm.security_scorpion_app.utils.DialogUtils
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.GlobalSettings
import com.icm.security_scorpion_app.utils.NetworkUtils

class EditDeviceActivity : AppCompatActivity() {

    private var connectionManager: ESP32ConnectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        // Recuperar los datos del intent
        val deviceName = intent.getStringExtra("deviceName")
        val deviceIp = intent.getStringExtra("deviceIp")
        val deviceId = intent.getStringExtra("deviceId")
        // Referenciar los EditText desde el layout
        val deviceNameEditText = findViewById<EditText>(R.id.deviceNameEditText)
        val deviceIpEditText = findViewById<EditText>(R.id.deviceIpEditText)
        val btnDeleteDevice = findViewById<Button>(R.id.btnDeleteDevice)
        val btnSave = findViewById<Button>(R.id.saveButton)
        val deviceIdTextView = findViewById<TextView>(R.id.deviceIdTextView)
        // Cargar los datos en los EditText
        deviceNameEditText.setText(deviceName)
        deviceIpEditText.setText(deviceIp)
        deviceIdTextView.text = deviceId ?: "22"

        btnSave.setOnClickListener {
            val newDeviceName = deviceNameEditText.text.toString()
            val newDeviceIp = deviceIpEditText.text.toString()

            if (newDeviceIp.isNotEmpty() && newDeviceName.isNotEmpty()) {
                val routerIp = NetworkUtils.getRouterIpAddress(this)
                if (NetworkUtils.isIpInSameNetwork(newDeviceIp, routerIp)) {
                    // Verifica que deviceIp no sea null y usa un valor predeterminado si es necesario
                    val actualDeviceIp = deviceIp ?: run {
                        Log.e("DeviceEdit", "IP del dispositivo es null")
                        return@setOnClickListener
                    }
                    val port = GlobalSettings.SOCKET_LOCAL_PORT
                    Log.d("A enviar ", "$actualDeviceIp y $port")
                    connectionManager = ESP32ConnectionManager(actualDeviceIp, GlobalSettings.SOCKET_LOCAL_PORT)
                    connectionManager?.connect { isConnected ->
                        if (isConnected) {
                            // Enviar los datos al ESP32
                            connectionManager?.sendMessage("editConfig:setName:$newDeviceName;setIp:$newDeviceIp")
                            // Actualizar el JSON local
                            val result = UpdateDeviceStorageManager.updateDeviceInJson(
                                this,
                                DeviceModel(deviceIdTextView.text.toString().toLong(), newDeviceName, newDeviceIp)
                            )
                            if (result) {
                                Log.d("DeviceEdit", "Datos actualizados localmente.")
                                //val intent = Intent(this, MainActivity::class.java)
                                //startActivity(intent)
                                finish()
                            }
                            // Mostrar mensaje de éxito
                            Log.d("DeviceEdit", "Datos enviados al ESP32 exitosamente.")
                        } else {
                            Log.d("DeviceEdit", "No se pudo conectar al ESP32.")
                        }
                    }
                } else {
                    Log.d("DeviceEdit", "IP no válida dentro de la red.")
                }
            } else {
                Log.d("DeviceEdit", "Nombre o IP del dispositivo está vacío.")
            }
        }


        btnDeleteDevice.setOnClickListener {
            // Verifica que deviceName no sea nulo antes de usarlo
            deviceName?.let {
                DialogUtils.showDeleteConfirmationDialog(this, it) {
                    val result = DeleteDeviceStorageManager.deleteDeviceFromJson(this, it)
                    if (result) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.d("DeviceDeletion", "No se pudo eliminar el dispositivo")
                    }
                }
            } ?: run {
                Log.e("DeviceDeletion", "deviceName es null")
            }
        }
    }
}