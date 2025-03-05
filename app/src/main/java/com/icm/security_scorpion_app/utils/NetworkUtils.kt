package com.icm.security_scorpion_app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    fun getRouterIpAddress(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return when {
            // 📡 Si está en Wi-Fi, obtener IP del router
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> getWifiRouterIp(context)

            // 📶 Si está compartiendo internet (hotspot), obtener IP de la interfaz de red
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> getHotspotIp()

            else -> "IP no disponible"
        }
    }

    private fun getWifiRouterIp(context: Context): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo

        return if (dhcpInfo.gateway != 0) {
            val ipAddress = dhcpInfo.gateway
            String.format(
                "%d.%d.%d.%d",
                (ipAddress and 0xFF),
                (ipAddress shr 8 and 0xFF),
                (ipAddress shr 16 and 0xFF),
                (ipAddress shr 24 and 0xFF)
            )
        } else {
            "IP no disponible"
        }
    }

    private fun getHotspotIp(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (netInterface in interfaces) {
                // Solo buscamos interfaces activas que puedan ser del hotspot
                if (netInterface.name.startsWith("wlan") || netInterface.name.startsWith("ap") || netInterface.name == "rndis0") {
                    for (address in netInterface.inetAddresses) {
                        if (!address.isLoopbackAddress && address is Inet4Address) {
                            return address.hostAddress ?: "IP no disponible"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "IP no disponible"
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