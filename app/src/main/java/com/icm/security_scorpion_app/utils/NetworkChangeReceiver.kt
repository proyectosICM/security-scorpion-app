package com.icm.security_scorpion_app.utils


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.widget.TextView
import com.icm.security_scorpion_app.utils.NetworkUtils

class NetworkChangeReceiver(
    private val connectionTextView: TextView,
    private val routerIpTextView: TextView,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        val isConnected = networkCapabilities != null
        val isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        connectionTextView.post {
            connectionTextView.text = when {
                isWifi -> "Conectado a Wi-Fi"
                isCellular -> "Conectado a Datos Móviles"
                else -> "No Conectado a una red"
            }
        }

        routerIpTextView.text = "IP Router: ${NetworkUtils.getRouterIpAddress(context)}"
    }

}
