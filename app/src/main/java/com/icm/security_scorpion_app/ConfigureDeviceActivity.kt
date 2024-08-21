package com.icm.security_scorpion_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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

        val wifiNameEditText: EditText = findViewById(R.id.wifiNameEditText)
        val wifiPasswordEditText: EditText = findViewById(R.id.wifiPasswordEditText)
        val changeNameCheckBox: CheckBox = findViewById(R.id.changeNameCheckBox)
        val deviceNameEditText: EditText = findViewById(R.id.deviceNameEditText)
        val saveButton: Button = findViewById(R.id.saveButton)

        connectionManager = ESP32ConnectionManager("192.168.1.1", 80)

        saveButton.setOnClickListener {
            val ssid = wifiNameEditText.text.toString()
            val password = wifiPasswordEditText.text.toString()
            val deviceName = deviceNameEditText.text.toString()

            Toast.makeText(this, "Enviando", Toast.LENGTH_SHORT).show()
            // Conectar al dispositivo
            connectionManager.connect { isConnected ->
                if (isConnected) {
                    // Enviar el SSID y la contraseña al dispositivo
                    connectionManager.sendMessage("SSID: $ssid")
                    connectionManager.sendMessage("PSW: $password")

                    if (changeNameCheckBox.isChecked) {
                        connectionManager.sendMessage("DEVICE_NAME: $deviceName")
                    }

                    runOnUiThread {
                        Toast.makeText(this, "Configuración de WiFi enviada", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Manejar la falta de conexión
                    // ...
                }
            }
        }
    }
}