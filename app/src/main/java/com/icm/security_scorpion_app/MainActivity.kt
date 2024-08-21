package com.icm.security_scorpion_app

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.icm.security_scorpion_app.utils.DeviceManager
import com.icm.security_scorpion_app.utils.NetworkChangeReceiver
import com.icm.security_scorpion_app.utils.DialogUtils

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
            R.id.action_settings -> {
                showDataOverwriteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
