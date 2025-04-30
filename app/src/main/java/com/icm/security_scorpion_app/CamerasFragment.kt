package com.icm.security_scorpion_app

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.icm.security_scorpion_app.data.CameraModel
import com.icm.security_scorpion_app.data.api.RetrofitInstance
import com.icm.security_scorpion_app.utils.GlobalSettings
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cameras, container, false)
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
                                    addCameraPlayer(view, camera.localUrl, camera.id.toString())
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

    private fun addCameraPlayer(rootView: View, videoUrl: String, cameraId: String) {
        val container = rootView.findViewById<LinearLayout>(R.id.video_container)

        val inflater = LayoutInflater.from(requireContext())
        val cameraView = inflater.inflate(R.layout.camera_item, container, false)

        val titleTextView = cameraView.findViewById<TextView>(R.id.camera_title)
        val videoLayout = cameraView.findViewById<VLCVideoLayout>(R.id.video_layout)
        val actionButton = cameraView.findViewById<Button>(R.id.action_button)

        titleTextView.text = "Cámara ID: $cameraId"

        actionButton.setOnClickListener {
            Toast.makeText(requireContext(), "Accionar cámara $cameraId", Toast.LENGTH_SHORT).show()
            // Aquí puedes llamar a tu API
        }

        container.addView(cameraView)

        // 📢 Imprimir la URL en log
        Log.d("CamerasFragment", "Reproduciendo cámara $cameraId en URL: $videoUrl")

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
                Log.d("VLC", "Evento VLC ($cameraId): ${event.type}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayers.forEach { if (it.isPlaying) it.pause() }
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
