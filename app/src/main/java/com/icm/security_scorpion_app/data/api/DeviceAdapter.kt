package com.icm.security_scorpion_app

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.icm.security_scorpion_app.data.DeviceModel
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.Timer
import java.util.TimerTask

class DeviceAdapter(private val context: Context, private val devices: List<DeviceModel>) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private var webSocketClient: WebSocketClient? = null

    init {
        val uri = URI("ws://samloto.com:7094/ws")
        createWebSocketClient(uri)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val deviceIp: TextView = itemView.findViewById(R.id.tvDeviceIp)
        val btnAction: Button = itemView.findViewById(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.nameDevice
        holder.deviceIp.text = device.ipLocal

        holder.btnAction.setOnClickListener {
            Toast.makeText(context, "Activado Remotamente ${device.nameDevice}", Toast.LENGTH_SHORT).show()
            Log.d("Activado Remotamente", "Clicked on ${device.nameDevice}")
            sendMessageToWebSocket("${device.nameDevice}:Activating")
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    private fun createWebSocketClient(uri: URI) {
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d("WebSocket", "Connected to the server")
            }

            override fun onMessage(message: String?) {
                Log.d("WebSocket", "Message received: $message")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d("WebSocket", "Disconnected from the server")
            }

            override fun onError(ex: Exception?) {
                Log.e("WebSocket", "Error: ${ex?.message}")

            }
        }
        webSocketClient?.connect()
    }

    private fun reconnect() {
        // Logic to reconnect with a delay
        Timer().schedule(object : TimerTask() {
            override fun run() {
                createWebSocketClient(URI("ws://samloto.com:7094/ws"))
            }
        }, 5000) // Reconnect after 5 seconds
    }

    fun sendMessageToWebSocket(message: String) {
        if (webSocketClient?.isOpen == true) {
            webSocketClient?.send(message)
            (context as MainActivity).runOnUiThread {
                Toast.makeText(context, "Dispositivo Activado Remotamente", Toast.LENGTH_SHORT).show()
            }
            webSocketClient?.close()
        } else {
            Log.e("WebSocket", "WebSocket is not open. Cannot send message.")
            (context as MainActivity).runOnUiThread {
                Toast.makeText(context, "No se puede conectar al dispositivo", Toast.LENGTH_SHORT).show()
            }
            reconnect()
        }
    }
}