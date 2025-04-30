package com.icm.security_scorpion_app

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager

class ConfigureDeviceActivity : AppCompatActivity() {

    private val failedIps = mutableListOf<String>() // Lista de IPs con error

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_device)

        val ipList = intent.getStringArrayListExtra("device_ips") ?: arrayListOf()
        ipList.forEach { ip -> Log.d("ConfigDevice", "IP del dispositivo: $ip") }

        val wifiNameEditText: EditText = findViewById(R.id.wifiNameEditText)
        val wifiPasswordEditText: EditText = findViewById(R.id.wifiPasswordEditText)
        val saveButton: Button = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            val ssid = wifiNameEditText.text.toString()
            val password = wifiPasswordEditText.text.toString()

            if (ssid.isEmpty() || password.isEmpty()) {
                showAlert("Error", "SSID y Contraseña no pueden estar vacíos")
                return@setOnClickListener
            }

            val configMessage = "cwc:$ssid:$password"
            failedIps.clear()

            var pendingResponses = ipList.size

            for (ip in ipList) {
                sendWifiCredentialsToDevice(ip, configMessage) { success ->
                    if (!success) {
                        failedIps.add(ip) // Guardar IP fallida
                    }

                    pendingResponses--
                    if (pendingResponses == 0) {
                        showFailureMessageAndRedirect()
                    }
                }
            }
        }
    }

    private fun sendWifiCredentialsToDevice(ip: String, configMessage: String, callback: (Boolean) -> Unit) {
        val connectionManager = ESP32ConnectionManager(ip, 82)

        connectionManager.connect { isConnected ->
            if (isConnected) {
                connectionManager.sendMessage(configMessage)
                runOnUiThread {
                    Log.d("ConfigDevice", "✅ Configuración enviada a $ip")
                }
                callback(true)
            } else {
                runOnUiThread {
                    Log.e("ConfigDevice", "❌ No se pudo conectar a $ip")
                }
                callback(false)
            }
        }
    }

    private fun showFailureMessageAndRedirect() {
        runOnUiThread {
            if (failedIps.isNotEmpty()) {
                val errorMessage = "No se pudo conectar a:\n\n${failedIps.joinToString("\n")}"
                showAlert("Fallo en conexión", errorMessage) {
                    redirectToMain()
                }
            } else {
                redirectToMain()
            }
        }
    }

    private fun showAlert(title: String, message: String, onDismiss: (() -> Unit)? = null) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@ConfigureDeviceActivity) // Usar la versión correcta
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun redirectToMain() {
        val intent = Intent(this, MainActivity2::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
