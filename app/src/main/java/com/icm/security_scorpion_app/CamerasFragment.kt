package com.icm.security_scorpion_app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class CamerasFragment : Fragment() {
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragment
        val view = inflater.inflate(R.layout.fragment_cameras, container, false)

        try {
            // Configurar VLC
            setupVLCPlayer(view)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al inicializar el reproductor: ${e.message}", Toast.LENGTH_LONG).show()
        }

        return view
    }

    private fun setupVLCPlayer(view: View) {
        // Inicializar el layout de video
        videoLayout = VLCVideoLayout(requireContext())

        // Añadir el video layout al contenedor
        val container = view.findViewById<ViewGroup>(R.id.video_container)
        container.addView(videoLayout)

        // Configurar opciones de VLC - Usar ArrayList mutable
        val args = arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--rtsp-tcp"
        )

        // Inicializar VLC
        libVLC = LibVLC(requireContext(), args)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.attachViews(videoLayout, null, false, false)

        // Configurar la fuente de video RTSP
        val rtspUrl = "rtsp://admin:Dakar*2024@192.168.10.117:554/cam/realmonitor?channel=1&subtype=0"
        val media = Media(libVLC, Uri.parse(rtspUrl))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=150") // Reduce el delay

        mediaPlayer.media = media
        mediaPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberar recursos cuando el fragment se destruye
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.detachViews()
        }
        if (::libVLC.isInitialized) {
            libVLC.release()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = CamerasFragment()
    }
}