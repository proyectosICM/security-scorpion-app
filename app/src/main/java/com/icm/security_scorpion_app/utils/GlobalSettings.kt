package com.icm.security_scorpion_app.utils

import android.content.Context
import android.content.SharedPreferences

object GlobalSettings {
    const val SOCKET_LOCAL_PORT = 82

    // Mensajes predeterminados
    const val MESSAGE_ACTIVATE = "activate"
    const val MESSAGE_DEACTIVATE = "deactivate"
    const val MESSAGE_DISCONNECT = "disconnect"
    const val ERROR_CONNECTION_FAILED = "No se pudo conectar al dispositivo: "
    const val ERROR_MESSAGE_SEND_FAILED = "Error al enviar el mensaje: "

    // URL base para Retrofit
    const val BASE_URL = "https://samloto.com:4015/api/"
    const val WS_URL = "ws://samloto.com:7094/ws"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences("GlobalSettings", Context.MODE_PRIVATE)
    }

    var groupId: Int?
        get() = preferences.getInt("groupId", -1).takeIf { it != -1 }
        set(value) = preferences.edit().putInt("groupId", value ?: -1).apply()

    var username: String?
        get() = preferences.getString("username", null)
        set(value) = preferences.edit().putString("username", value).apply()

    var password: String?
        get() = preferences.getString("password", null)
        set(value) = preferences.edit().putString("password", value).apply()
}