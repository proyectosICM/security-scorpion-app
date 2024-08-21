package com.icm.security_scorpion_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText

class EditDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_device)

        // Recuperar los datos del intent
        val deviceName = intent.getStringExtra("deviceName")
        val deviceIp = intent.getStringExtra("deviceIp")

        // Referenciar los EditText desde el layout
        val deviceNameEditText = findViewById<EditText>(R.id.deviceNameEditText)
        val deviceIpEditText = findViewById<EditText>(R.id.deviceIpEditText)

        // Cargar los datos en los EditText
        deviceNameEditText.setText(deviceName)
        deviceIpEditText.setText(deviceIp)
    }
}