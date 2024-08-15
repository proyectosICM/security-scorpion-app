package com.icm.security_scorpion_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.icm.security_scorpion_app.common.KeyboardUtils
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.SaveDeviceStorageManager
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.NameDeviceExtractor
import com.icm.security_scorpion_app.utils.NetworkUtils

class AddDevice : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: Button
    private lateinit var btnTest: Button
    private lateinit var btnAdd: Button
    private lateinit var etIpAddress: EditText
    private lateinit var tvValidationMessage: TextView
    private lateinit var deviceNameData: TextView

    private var connectionManager: ESP32ConnectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnTest = findViewById(R.id.btnTest)
        btnAdd = findViewById(R.id.btnAdd)
        etIpAddress = findViewById(R.id.etIpAddress)
        tvValidationMessage = findViewById(R.id.tvValidationMessage)
        deviceNameData = findViewById(R.id.deviceNameData)

        btnBack.setOnClickListener {
            finish()
        }

        btnTest.setOnClickListener {
            // Hide the keyboard
            KeyboardUtils.hideKeyboard(this)

            // Clear previous validation message and button
            tvValidationMessage.text = ""
            deviceNameData.text = ""
            tvValidationMessage.setTextColor(resources.getColor(R.color.white, null)) // Color por defecto
            btnAdd.visibility = View.GONE

            val ipAddress = etIpAddress.text.toString()
            if (ipAddress.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                simulateLongOperation(ipAddress)
            } else {
                tvValidationMessage.text = "Ingrese una IP válida."
                tvValidationMessage.setTextColor(resources.getColor(R.color.colorError, null)) // Color rojo
            }
        }

        btnAdd.setOnClickListener {
            val ipAddress = etIpAddress.text.toString()
            val deviceName = deviceNameData.text.toString()

            if (ipAddress.isNotEmpty() && deviceName.isNotEmpty()) {
                val device = DeviceModel(ipLocal = ipAddress, nameDevice = deviceName)
                val devices = SaveDeviceStorageManager.loadDevicesFromJson(this).toMutableList()
                devices.add(device)
                SaveDeviceStorageManager.saveDevicesToJson(this, devices)
                tvValidationMessage.text = "Dispositivo guardado correctamente."
                tvValidationMessage.setTextColor(resources.getColor(R.color.colorSuccess, null))
                setResult(RESULT_OK)
                finish()
            } else {
                tvValidationMessage.text = "Información del dispositivo incompleta."
                tvValidationMessage.setTextColor(resources.getColor(R.color.colorError, null))
            }
        }

    }

    private fun simulateLongOperation(ipAddress: String) {
        progressBar.visibility = View.VISIBLE

        val routerIp = NetworkUtils.getRouterIpAddress(this)
        if (isIpInSameNetwork(ipAddress, routerIp)) {
            connectionManager?.disconnect()
            connectionManager = ESP32ConnectionManager(ipAddress, 82)
            connectionManager?.connect { isConnected ->
                if (isConnected) {
                    connectionManager?.sendMessage("getName")

                    connectionManager?.receiveMessage { response ->
                        // Ocultar el ProgressBar al recibir la respuesta
                        progressBar.visibility = View.GONE
                        tvValidationMessage.text = "Dispositivo conectado correctamente."
                        tvValidationMessage.setTextColor(resources.getColor(R.color.colorSuccess, null))
                        val name = NameDeviceExtractor.extractName(response)
                        deviceNameData.text = "$name"
                        btnAdd.visibility = View.VISIBLE
                        connectionManager?.sendMessage("disconnect")
                    }
                } else {
                    // Ocultar el ProgressBar si no se pudo conectar
                    progressBar.visibility = View.GONE
                    tvValidationMessage.text = "No se pudo conectar al dispositivo."
                    tvValidationMessage.setTextColor(resources.getColor(R.color.colorError, null))
                    deviceNameData.text = ""
                }
            }
        } else {
            progressBar.visibility = View.GONE
            tvValidationMessage.text = "IP no válida dentro de la red."
            tvValidationMessage.setTextColor(resources.getColor(R.color.colorError, null))
        }
    }

    private fun isIpInSameNetwork(ipAddress: String, routerIp: String): Boolean {
        // Convert IP addresses to integer values
        val ipParts = ipAddress.split(".").map { it.toInt() }
        val routerIpParts = routerIp.split(".").map { it.toInt() }
        return if (ipParts.size == 4 && routerIpParts.size == 4) {
            // Check if IP is in the same subnet as the router IP
            ipParts[0] == routerIpParts[0] && ipParts[1] == routerIpParts[1] && ipParts[2] == routerIpParts[2]
        } else {
            false
        }
    }
}