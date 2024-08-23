package com.icm.security_scorpion_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.icm.security_scorpion_app.data.DeviceModel
import com.icm.security_scorpion_app.data.SaveDeviceStorageManager

class LoadFromJsonActivity : AppCompatActivity() {

    private lateinit var selectJsonButton: Button
    private lateinit var loadDataButton: Button
    private lateinit var statusTextView: TextView

    private val PICK_JSON_FILE_REQUEST_CODE = 1
    private val TAG = "LoadFromJsonActivity"

    // Variable to store the URI of the selected file
    private var selectedFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_from_json)
        Log.d(TAG, "Iniciando")

        selectJsonButton = findViewById(R.id.selectJsonButton)
        loadDataButton = findViewById(R.id.loadDataButton)
        statusTextView = findViewById(R.id.statusTextView)

        selectJsonButton.setOnClickListener {
            Log.d(TAG, "Select JSON button clicked")
            openFilePicker()
        }

        loadDataButton.setOnClickListener {
            Log.d(TAG, "Load data button clicked")
            loadDataFromJson()
        }

        // Handle incoming Intents
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.let {
            if (Intent.ACTION_SEND == it.action && "application/json" == it.type) {
                it.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                    Log.d(TAG, "File selected from external app: $uri")
                    val fileName = getFileName(uri)
                    statusTextView.text = "Archivo seleccionado: $fileName"
                    storeSelectedFileUri(uri)
                    loadDataFromJson()  // Automatically load data after file selection
                }
            } else if (Intent.ACTION_VIEW == it.action && "application/json" == it.type) {
                it.data?.let { uri ->
                    Log.d(TAG, "File selected from external app: $uri")
                    val fileName = getFileName(uri)
                    statusTextView.text = "Archivo seleccionado: $fileName"
                    storeSelectedFileUri(uri)
                    loadDataFromJson()  // Automatically load data after file selection
                }
            } else {
                Log.d(TAG, "No valid action or type")
            }
        } ?: run {
            Log.d(TAG, "Intent is null")
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        Log.d(TAG, "Opening file picker for JSON files")
        startActivityForResult(intent, PICK_JSON_FILE_REQUEST_CODE)
    }

    private fun loadDataFromJson() {
        // First, clear the existing data
        clearExistingData()

        // Now, load data from the JSON file
        val fileUri = getSelectedFileUri()  // Get URI of the selected file
        fileUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    // Process the JSON content here
                    Log.d(TAG, "JSON content: $jsonString")

                    // Parse JSON and update the data store
                    val gson = Gson()
                    val deviceListType = object : TypeToken<List<DeviceModel>>() {}.type
                    val devices: List<DeviceModel> = gson.fromJson(jsonString, deviceListType)

                    // Save the parsed devices to the main JSON file
                    SaveDeviceStorageManager.saveDevicesToJson(this, devices)

                    statusTextView.text = "Datos cargados exitosamente desde el JSON."
                    Toast.makeText(this, "Datos cargados exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                } ?: run {
                    Log.d(TAG, "Input stream is null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading JSON file", e)
                Toast.makeText(this, "Error al cargar datos desde el JSON", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.d(TAG, "No URI available to load data from")
        }
    }

    private fun clearExistingData() {
        // Clear existing data
        SaveDeviceStorageManager.saveDevicesToJson(this, emptyList())
        Log.d(TAG, "Existing data cleared")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult called with requestCode: $requestCode, resultCode: $resultCode")
        if (requestCode == PICK_JSON_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                Log.d(TAG, "File selected: $uri")
                val fileName = getFileName(uri)
                statusTextView.text = "Archivo seleccionado: $fileName"
                // Store the URI to be used later
                storeSelectedFileUri(uri)
            } ?: run {
                Log.d(TAG, "No file selected or data is null")
            }
        } else {
            Log.d(TAG, "Result not OK or request code mismatch")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                        Log.d(TAG, "File name extracted: $fileName")
                    } else {
                        Log.d(TAG, "Name index not found in cursor")
                    }
                } else {
                    Log.d(TAG, "Cursor is empty or could not move to first")
                }
            } ?: run {
                Log.d(TAG, "Content resolver query returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name", e)
        }
        return fileName
    }

    private fun storeSelectedFileUri(uri: Uri) {
        // Store the URI in a variable or persistent storage
        selectedFileUri = uri
        Log.d(TAG, "Stored URI: $uri")
    }

    private fun getSelectedFileUri(): Uri? {
        // Retrieve the stored URI
        return selectedFileUri
    }
}
