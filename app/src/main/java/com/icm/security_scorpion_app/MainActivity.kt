package com.icm.security_scorpion_app

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.icm.security_scorpion_app.data.DeleteDeviceStorageManager
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.LoadDeviceStorageManager
import com.icm.security_scorpion_app.data.api.JsonPlaceHolderApi
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.NetworkChangeReceiver
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var llDevicesContent: LinearLayout
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var isConnectedTextView: TextView
    private lateinit var ipRouterTextView: TextView

    private var connectionManager: ESP32ConnectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)


        initializeUI()

        initializeNetworkChangeReceiver()

        registerNetworkChangeReceiver()

        loadAndDisplayDevices()
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

        btnAddDevice.setOnClickListener {
            val intent = Intent(this, AddDevice::class.java)
            startActivity(intent)
        }
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
        loadAndDisplayDevices() // Recargar la lista de dispositivos cuando se vuelve a la actividad
    }



    private fun loadAndDisplayDevices() {
        // Carga los dispositivos desde el JSON
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(this)
        Log.d("MainActivity", "Devices loaded: $devices")
        // Limpia el contenedor
        llDevicesContent.removeAllViews()

        // Infla y agrega una tarjeta para cada dispositivo
        val inflater = LayoutInflater.from(this)
        for (device in devices) {
            val view = inflater.inflate(R.layout.device_item, llDevicesContent, false)
            val tvDeviceName = view.findViewById<TextView>(R.id.tvDeviceName)
            val tvDeviceIp = view.findViewById<TextView>(R.id.tvDeviceIp)
            val btnAction = view.findViewById<Button>(R.id.btnAction)
            val btnDeleteDevice = view.findViewById<Button>(R.id.btnDeleteDevice)

            tvDeviceName.text = device.nameDevice
            tvDeviceIp.text = device.ipLocal

            // Configura el botón de acción si es necesario
            btnAction.setOnClickListener {
                Log.d("DeviceAction", "Accionado")
                connectionManager?.disconnect()
                connectionManager = ESP32ConnectionManager(tvDeviceIp.text.toString(), 82)
                connectionManager?.connect { isConnected ->
                    if (isConnected) {
                        connectionManager?.sendMessage("activate")
                        runOnUiThread {
                            Toast.makeText(this, "Dispositivo activado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "No se pudo conectar al dispositivo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            btnDeleteDevice.setOnClickListener{
                showDeleteConfirmationDialog(device.nameDevice) {
                    val result = DeleteDeviceStorageManager.deleteDeviceFromJson(this, device.nameDevice)
                    if (result) {
                        loadAndDisplayDevices() // Recargar la lista de dispositivos después de la eliminación
                    } else {
                        Log.d("DeviceDeletion", "No se pudo eliminar el dispositivo")
                    }
                }
            }

            llDevicesContent.addView(view)
        }
    }

    private fun showDeleteConfirmationDialog(deviceName: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(this, R.style.DialogButtonTextStyle)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar el dispositivo '$deviceName'?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_DEVICE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadAndDisplayDevices() // Recargar la lista de dispositivos después de agregar uno nuevo
        }
    }

    companion object {
        private const val ADD_DEVICE_REQUEST_CODE = 1
    }

    /* Toolbar */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                fetchDevicesFromServer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun fetchDevicesFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://samloto.com:4015/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi::class.java)
        val call = jsonPlaceHolderApi.getDevices()

        call.enqueue(object : Callback<List<DeviceModel>> {
            override fun onResponse(call: Call<List<DeviceModel>>, response: Response<List<DeviceModel>>) {
                if (!response.isSuccessful) {
                    Log.e("MainActivity", "Response not successful: ${response.errorBody()?.string()}")
                    return
                }

                val devices = response.body()
                if (devices != null) {
                    val jsonResponse = Gson().toJson(devices)
                    Log.d("MainActivity", "Devices loaded: $jsonResponse")
                } else {
                    Log.e("MainActivity", "No devices received")
                }
            }

            override fun onFailure(call: Call<List<DeviceModel>>, t: Throwable) {
                Log.e("MainActivity", "API call failed: ${t.message}")
            }
        })
    }
}
