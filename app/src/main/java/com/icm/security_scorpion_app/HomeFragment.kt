package com.icm.security_scorpion_app

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.icm.security_scorpion_app.data.LoadDeviceStorageManager
import com.icm.security_scorpion_app.utils.DeviceManager
import com.icm.security_scorpion_app.utils.DialogUtils
import com.icm.security_scorpion_app.utils.GlobalSettings
import com.icm.security_scorpion_app.utils.NetworkChangeReceiver
import java.io.File
import java.io.FileWriter

class HomeFragment : Fragment() {

    private lateinit var llDevicesContent: LinearLayout
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var isConnectedTextView: TextView
    private lateinit var ipRouterTextView: TextView
    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Necesario para mostrar el menú en el Fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initializeUI(view)
        initializeNetworkChangeReceiver()
        registerNetworkChangeReceiver()

        deviceManager = DeviceManager(requireContext(), llDevicesContent)
        deviceManager.loadAndDisplayDevices()
        GlobalSettings.init(requireContext())

        deviceManager.fetchDevicesFromServerAuth(GlobalSettings.username ?: "", GlobalSettings.password ?: "")



        return view
    }

    private fun initializeUI(view: View) {
        llDevicesContent = view.findViewById(R.id.llDevicesContent)
        isConnectedTextView = view.findViewById(R.id.isConnectedTextView)
        ipRouterTextView = view.findViewById(R.id.ipRouterTextView)


        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = ""
    }

    private fun initializeNetworkChangeReceiver() {
        networkChangeReceiver = NetworkChangeReceiver(isConnectedTextView, ipRouterTextView)
    }

    private fun registerNetworkChangeReceiver() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkChangeReceiver, intentFilter)
    }

    private fun unregisterNetworkChangeReceiver() {
        requireContext().unregisterReceiver(networkChangeReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterNetworkChangeReceiver()
    }

    override fun onResume() {
        super.onResume()
        deviceManager.loadAndDisplayDevices()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.device_settings -> {
                Toast.makeText(requireContext(), "Configurar una nueva red General", Toast.LENGTH_SHORT).show()
                val devices = LoadDeviceStorageManager.loadDevicesFromJson(requireContext())
                val ipList = ArrayList<String>()
                for (device in devices) {
                    ipList.add(device.ipLocal)
                }
                val intent = Intent(requireContext(), ConfigureDeviceActivity::class.java)
                intent.putStringArrayListExtra("device_ips", ipList)
                startActivity(intent)
                true
            }

            R.id.action_settings -> {
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

    private fun showLoginDialog() {
        DialogUtils.showDataOverwriteConfirmationDialog(requireContext()) {
            DialogUtils.showLoginDialog(requireContext()) { username, password ->
                deviceManager.fetchDevicesFromServerAuth(username, password)
            }
        }
    }

    private fun shareDeviceData() {
        val devices = LoadDeviceStorageManager.loadDevicesFromJson(requireContext())
        val gson = Gson()
        val jsonString = gson.toJson(devices)
        val fileName = "device_data.json"
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileWriter(file).use { writer -> writer.write(jsonString) }
            val uri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
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
            Toast.makeText(requireContext(), "Error al compartir datos", Toast.LENGTH_LONG).show()
            Log.e("HomeFragment", "Error al compartir datos", e)
        }
    }

    companion object {
        private const val ADD_DEVICE_REQUEST_CODE = 1
    }
}
