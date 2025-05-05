package com.icm.security_scorpion_app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.icm.security_scorpion_app.data.CameraModel
import com.icm.security_scorpion_app.data.api.RetrofitInstance
import com.icm.security_scorpion_app.utils.ESP32ConnectionManager
import com.icm.security_scorpion_app.utils.GlobalSettings
import com.icm.security_scorpion_app.utils.NetworkUtils
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CamerasFragment : Fragment() {

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private val libVLCInstances = mutableListOf<LibVLC>()

    private var connectionManager: ESP32ConnectionManager? = null
    private val autoDisableHandler = android.os.Handler()
    private var autoDisableRunnable: Runnable? = null
    private lateinit var deviceAdapter: DeviceAdapter // Asegúrate de inicializarlo correctamente

    private lateinit var currentIp: String
    fun getSubnet(ip: String): String {
        return ip.substringBeforeLast(".") // Obtiene los primeros 3 octetos (Ej: "192.168.1")
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cameras, container, false)

        currentIp = NetworkUtils.getRouterIpAddress(requireContext())

        deviceAdapter = DeviceAdapter(requireContext(), emptyList(), null)

        val groupId = GlobalSettings.groupId

        if (groupId == null) {
            Toast.makeText(requireContext(), "No se encontró el ID de grupo", Toast.LENGTH_SHORT).show()
            return view
        }



        RetrofitInstance.api.getCamerasByGroup(groupId.toString())
            .enqueue(object : Callback<List<CameraModel>> {
                override fun onResponse(call: Call<List<CameraModel>>, response: Response<List<CameraModel>>) {
                    if (response.isSuccessful) {
                        val cameras = response.body()
                        Log.d("CamerasFragment", "Respuesta del servidor: $cameras")
                        if (!cameras.isNullOrEmpty()) {
                            cameras.forEach { camera ->
                                if (camera.active) {
                                    addCameraPlayer(view, camera.localUrl, camera)
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "No hay cámaras disponibles", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<CameraModel>>, t: Throwable) {
                    Toast.makeText(requireContext(), "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        return view
    }

    private fun addCameraPlayer(rootView: View, videoUrl: String, camera: CameraModel) {
        val container = rootView.findViewById<LinearLayout>(R.id.video_container)

        val inflater = LayoutInflater.from(requireContext())
        val cameraView = inflater.inflate(R.layout.camera_item, container, false)

        val titleTextView = cameraView.findViewById<TextView>(R.id.camera_title)
        val videoLayout = cameraView.findViewById<VLCVideoLayout>(R.id.video_layout)
        val actionButton = cameraView.findViewById<Button>(R.id.action_button)

        titleTextView.text = "Cámara ID: ${camera.id}"


        if (camera.deviceId == 0L) {
            actionButton.visibility = View.GONE
        } else {
            actionButton.visibility = View.VISIBLE
            actionButton.tag = false // Inicialmente desactivado

            actionButton.setOnClickListener {
                connectionManager?.disconnect()

                val isActivated = actionButton.tag as? Boolean ?: false

                fun updateButtonState(activated: Boolean) {
                    if (activated) {
                        actionButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green_light))
                        actionButton.tag = true

                        autoDisableRunnable?.let { autoDisableHandler.removeCallbacks(it) }
                        autoDisableRunnable = Runnable {
                            if (actionButton.tag as? Boolean == true) {
                                actionButton.performClick()
                                Toast.makeText(requireContext(), "⏳ Desactivado automáticamente tras 2 minutos", Toast.LENGTH_SHORT).show()
                            }
                        }
                        autoDisableHandler.postDelayed(autoDisableRunnable!!, 120000)

                    } else {
                        actionButton.setBackgroundResource(R.drawable.border)
                        actionButton.tag = false
                        autoDisableRunnable?.let { autoDisableHandler.removeCallbacks(it) }
                    }
                }

                if (getSubnet(currentIp) == getSubnet(camera.deviceIp ?: "")) {
                    connectionManager = ESP32ConnectionManager(camera.deviceIp!!, GlobalSettings.SOCKET_LOCAL_PORT)
                    connectionManager?.connect { isConnected ->
                        if (isConnected) {
                            if (isActivated) {
                                connectionManager?.sendMessage(GlobalSettings.MESSAGE_DEACTIVATE)
                                Toast.makeText(requireContext(), "Dispositivo Desactivado Localmente", Toast.LENGTH_SHORT).show()
                            } else {
                                connectionManager?.sendMessage(GlobalSettings.MESSAGE_ACTIVATE)
                                Toast.makeText(requireContext(), "Dispositivo Activado Localmente", Toast.LENGTH_SHORT).show()
                            }
                            updateButtonState(!isActivated)
                        } else {
                            if (isActivated) {
                                deviceAdapter.sendMessageToWebSocket("deactivate:${camera.deviceId}")
                                Toast.makeText(requireContext(), "Dispositivo Desactivado Remotamente", Toast.LENGTH_SHORT).show()
                            } else {
                                deviceAdapter.sendMessageToWebSocket("activate:${camera.deviceId}")
                                Toast.makeText(requireContext(), "Dispositivo Activado Remotamente", Toast.LENGTH_SHORT).show()
                            }
                            updateButtonState(!isActivated)
                        }
                    }
                } else {
                    if (isActivated) {
                        deviceAdapter.sendMessageToWebSocket("deactivate:${camera.deviceId}")
                        Toast.makeText(requireContext(), "Dispositivo Desactivado Remotamente (IP diferente)", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceAdapter.sendMessageToWebSocket("activate:${camera.deviceId}")
                        Toast.makeText(requireContext(), "Dispositivo Activado Remotamente (IP diferente)", Toast.LENGTH_SHORT).show()
                    }
                    updateButtonState(!isActivated)
                }
            }
        }

        container.addView(cameraView)

        val libVLC = LibVLC(requireContext(), arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--rtsp-tcp"))
        libVLCInstances.add(libVLC)

        val mediaPlayer = MediaPlayer(libVLC)
        mediaPlayers.add(mediaPlayer)

        videoLayout.post {
            mediaPlayer.attachViews(videoLayout, null, false, false)

            val media = Media(libVLC, Uri.parse(videoUrl)).apply {
                setHWDecoderEnabled(true, false)
                addOption(":network-caching=150")
            }

            mediaPlayer.media = media
            mediaPlayer.play()

            mediaPlayer.setEventListener { event ->
                Log.d("VLC", "Evento VLC (${camera.id}): ${event.type}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayers.forEach { if (!it.isPlaying) it.play() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach {
            it.stop()
            it.detachViews()
        }
        libVLCInstances.forEach { it.release() }
        mediaPlayers.clear()
        libVLCInstances.clear()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CamerasFragment()
    }
}
