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

     fun isIpInSameNetwork(ipAddress: String, routerIp: String): Boolean {
        // Convert IP addresses to integer values
        val ipParts = ipAddress.split(".").map { it.toInt() }
        val routerIpParts = routerIp.split(".").map { it.toInt() }
        return if (ipParts.size == 4 && routerIpParts.size == 4) {
            // Check if IP is in the same subnet as the router IP
            ipParts[0] == routerIpParts[0] && ipParts[1] == routerIpParts[1] && ipParts[2] == routerIpParts[2]
        } else {
            false
        }
    }
}