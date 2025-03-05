package com.icm.security_scorpion_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager

class ConfigureDeviceActivity : AppCompatActivity() {

    private lateinit var connectionManager: ESP32ConnectionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_device)

        val ipList = intent.getStringArrayListExtra("device_ips") ?: arrayListOf()

        if (ipList != null) {
            for (ip in ipList) {
                Log.d("ConfigDevice", "IP del dispositivo: $ip")
            }
        }

        val wifiNameEditText: EditText = findViewById(R.id.wifiNameEditText)
        val wifiPasswordEditText: EditText = findViewById(R.id.wifiPasswordEditText)
        val saveButton: Button = findViewById(R.id.saveButton)

        // connectionManager = ESP32ConnectionManager("192.168.1.100", 82)

        saveButton.setOnClickListener {
            val ssid = wifiNameEditText.text.toString()
            val password = wifiPasswordEditText.text.toString()

            if (ssid.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "SSID y Contraseña no pueden estar vacíos", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val configMessage = "cwc:$ssid:$password"

            Toast.makeText(this, "Enviando credenciales...", Toast.LENGTH_SHORT).show()

            for (ip in ipList) {
                sendWifiCredentialsToDevice(ip, configMessage)
            }
        }
    }

    private fun sendWifiCredentialsToDevice(ip: String, configMessage: String) {
        val connectionManager = ESP32ConnectionManager(ip, 82)

        connectionManager.connect { isConnected ->
            if (isConnected) {
                connectionManager.sendMessage(configMessage)

                runOnUiThread {
                    Toast.makeText(this, "Configuración enviada a $ip", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "❌ No se pudo conectar a $ip", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}