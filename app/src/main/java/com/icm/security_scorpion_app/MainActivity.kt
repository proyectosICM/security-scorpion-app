package com.icm.security_scorpion_app

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.icm.security_scorpion_app.data.LoadDeviceStorageManager
import com.icm.security_scorpion_app.utils.DeviceManager
import com.icm.security_scorpion_app.utils.NetworkChangeReceiver
import com.icm.security_scorpion_app.utils.DialogUtils
import java.io.File
import java.io.FileWriter

class MainActivity : AppCompatActivity() {

    private lateinit var llDevicesContent: LinearLayout
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var isConnectedTextView: TextView
    private lateinit var ipRouterTextView: TextView

    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        initializeUI()
        initializeNetworkChangeReceiver()
        registerNetworkChangeReceiver()

        deviceManager = DeviceManager(this, llDevicesContent)
        deviceManager.loadAndDisplayDevices()
    }

    private fun initializeUI() {
        llDevicesContent = findViewById(R.id.llDevicesContent)
        val btnAddDevice = findViewById<Button>(R.id.btnAddDevice)
        btnAddDevice.setOnClickListener {
            val intent = Intent(this, AddDevice::class.java)
            startActivity(intent)
        }

        isConnectedTextView = findViewById(R.id.isConnectedTextView)
        ipRouterTextView = findViewById(R.id.ipRouterTextView)
    }

    private fun initializeNetworkChangeReceiver() {
        networkChangeReceiver = NetworkChangeReceiver(isConnectedTextView, ipRouterTextView)
    }

    private fun registerNetworkChangeReceiver() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
    }

    override fun onResume() {
        super.onResume()
        deviceManager.loadAndDisplayDevices()
    }

    private fun showDataOverwriteConfirmationDialog() {
        DialogUtils.showDataOverwriteConfirmationDialog(this) {
            deviceManager.fetchDevicesFromServer()
        }
    }

    private fun showLoginDialog() {
        DialogUtils.showDataOverwriteConfirmationDialog(this) {
            DialogUtils.showLoginDialog(this) { username, password ->
                deviceManager.fetchDevicesFromServerAuth(username, password)
                //Toast.makeText(this, "Usuario: $username, Contraseña: $password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_DEVICE_REQUEST_CODE && resultCode == RESULT_OK) {
            deviceManager.loadAndDisplayDevices()
        }
    }

    companion object {
        private const val ADD_DEVICE_REQUEST_CODE = 1
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.device_settings -> {
                Toast.makeText(this, "Configurar dispositivo nuevo seleccionado", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ConfigureDeviceActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.load_from_json -> {
                val intent = Intent(this, LoadFromJsonActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                //showDataOverwriteConfirmationDialog()
                showLoginDialog()
                true
            }
            R.id.action_share_device_data -> {
                shareDeviceData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareDeviceData() {
        // Obtener la lista de dispositivos
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(this)
        Log.d("ss", "$devices")
        // Convertir la lista de dispositivos a JSON
        val gson = Gson()
        val jsonString = gson.toJson(devices)

        // Crear un archivo para almacenar el JSON
        val fileName = "device_data.json"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            // Escribir el JSON en el archivo
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }

            // Compartir el archivo a través de WhatsApp
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider", // Asegúrate de que el provider esté configurado en tu manifest
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir datos de dispositivos"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir datos: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }
}
