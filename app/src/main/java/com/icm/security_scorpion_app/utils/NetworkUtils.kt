package com.icm.security_scorpion_app.utils

import android.content.Context
import android.net.wifi.WifiManager

object NetworkUtils {
    fun getRouterIpAddress(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo

        return if (dhcpInfo.gateway != 0) {
            val ipAddress = dhcpInfo.gateway
            String.format(
                "%d.%d.%d.%d",
                (ipAddress and 0xFF).toInt(),
                (ipAddress shr 8 and 0xFF).toInt(),
                (ipAddress shr 16 and 0xFF).toInt(),
                (ipAddress shr 24 and 0xFF).toInt()
            )
        } else {
            "IP no disponible"
        }
    }
}