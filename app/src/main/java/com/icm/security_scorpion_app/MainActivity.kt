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

    /**
     * Initializes the user interface, linking views and configuring buttons.
     */
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

    /**
     * Creates and initializes the BroadcastReceiver that will detect changes in the network connection and update the corresponding views.
     * and update the corresponding views.
     */
    private fun initializeNetworkChangeReceiver() {
        networkChangeReceiver = NetworkChangeReceiver(isConnectedTextView, ipRouterTextView)
    }

    /**
     * Registers the BroadcastReceiver in the system to start receiving events.
     * of changes in the network connection.
     */
    private fun registerNetworkChangeReceiver() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver, intentFilter)
    }

    /**
     * It is executed when the activity is destroyed.
     * BroadcastReceiver is unregistered to avoid memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkChangeReceiver)
    }

    /**
     * Called when the activity is resumed.
     * Reloads the list of devices in case of changes.
     */
    override fun onResume() {
        super.onResume()
        deviceManager.loadAndDisplayDevices()
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

    /**
     * Shows a login dialog to authenticate and load device data from the API.
     */
    private fun showLoginDialog() {
        DialogUtils.showDataOverwriteConfirmationDialog(this) {
            DialogUtils.showLoginDialog(this) { username, password ->
                deviceManager.fetchDevicesFromServerAuth(username, password)
            }
        }
    }

    /**
     * Inflates the toolbar menu options from the XML resource.
     * This method is called when the toolbar menu is created.
     *
     * @param menu The menu in which items are placed.
     * @return true to display the menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    /**
     * Handles toolbar menu item selections. Based on the selected option, it performs different actions:
     * - Opens the network configuration screen.
     * - Loads device data from a JSON file.
     * - Displays the login dialog.
     * - Shares device data via WhatsApp.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            /** Network configuration */
            R.id.device_settings -> {
                Toast.makeText(this, "Configurar una nueva red", Toast.LENGTH_SHORT).show()

                val devices = LoadDeviceStorageManager.loadDevicesFromJson(this)
                val ipList = ArrayList<String>()

                for (device in devices) {
                    ipList.add(device.ipLocal)
                }

                val intent = Intent(this, ConfigureDeviceActivity::class.java)
                intent.putStringArrayListExtra("device_ips", ipList)
                startActivity(intent)
                true
            }

            /** Loads device data from a JSON file */
            R.id.load_from_json -> {
                val intent = Intent(this, LoadFromJsonActivity::class.java)
                startActivity(intent)
                true
            }

            /** Shows login dialog for API authentication */
            R.id.action_settings -> {
                showLoginDialog()
                true
            }

            /** Shares device data via WhatsApp */
            R.id.action_share_device_data -> {
                shareDeviceData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * This function shares the device data by converting it to JSON,
     * saving it as a file, and then sharing it via WhatsApp.
     */
    private fun shareDeviceData() {
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(this)
        val gson = Gson()
        val jsonString = gson.toJson(devices)
        val fileName = "device_data.json"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileWriter(file).use { writer ->
                writer.write(jsonString)
            }

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
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
            Toast.makeText(this, "Error al compartir datos", Toast.LENGTH_LONG).show()
            Log.e("MainActivity", "Error al compartir datos", e)
        }

    }
}
