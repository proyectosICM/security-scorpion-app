package com.icm.security_scorpion_app.utils

object GlobalSettings {
    const val SOCKET_LOCAL_PORT = 82

    // Mensajes predeterminados
    const val MESSAGE_ACTIVATE = "activate"
    const val MESSAGE_DISCONNECT = "disconnect"
    const val ERROR_CONNECTION_FAILED = "No se pudo conectar al dispositivo: "
    const val ERROR_MESSAGE_SEND_FAILED = "Error al enviar el mensaje: "

    // URL base para Retrofit
    const val BASE_URL = "https://samloto.com:4015/api/"
    const val WS_URL = "ws://samloto.com:7094/ws"
}