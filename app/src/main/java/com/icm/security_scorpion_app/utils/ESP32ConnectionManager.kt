package com.icm.security_scorpion_app.utils

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

class ESP32ConnectionManager(private val serverIp: String, private val port: Int) {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null
    private var messageCallback: ((String?) -> Unit)? = null
    private var isListening: Boolean = false
    var errorCallback: ((String) -> Unit)? = null

    fun connect(callback: (Boolean) -> Unit) {
        ConnectTask(callback).execute()
    }

    fun disconnect() {
        try {
            sendMessage(GlobalSettings.MESSAGE_DISCONNECT)
            isListening = false
            socket?.close()
            Log.d("mens", "Desconectado del servidor")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(message: String) {
        if (outputStream != null) {
            SendMessageTask(outputStream!!).execute(message)
        } else {
            errorCallback?.invoke("No se pudo enviar el mensaje. El dispositivo no está conectado.")
        }
    }

    fun receiveMessage(callback: (String?) -> Unit) {
        if (reader != null) {
            messageCallback = callback
            ReceiveMessageTask(reader!!, callback).execute()
        }
    }

    fun startListening(callback: (String?) -> Unit) {
        if (reader != null) {
            messageCallback = callback
            isListening = true
            ReceiveMessageTask(reader!!, callback).execute()
        }
    }

    private inner class ConnectTask(val callback: (Boolean) -> Unit) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                socket = Socket(serverIp, port)
                outputStream = socket?.getOutputStream()
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Error (l)","Error en conexion socket local")
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Log.d("mens", "Conectado al servidor")
                callback(true)
            } else {
                errorCallback?.invoke("No se pudo conectar al dispositivo.")
                callback(false)
            }
        }
    }

    private inner class ReceiveMessageTask(private val reader: BufferedReader, private val callback: (String?) -> Unit) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg params: Void?): String? {
            return try {
                reader.readLine().also { message ->
                    Log.d("mens-r", "Mensaje recibido: $message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("mens-r", "Error al recibir el mensaje: ${e.message}")
                null
            }
        }

        override fun onPostExecute(result: String?) {
            callback(result)
            if (isListening) {
                ReceiveMessageTask(reader, callback).execute()
            }
        }
    }

    private inner class SendMessageTask(private val outputStream: OutputStream) : AsyncTask<String, Void, Void>() {
        override fun doInBackground(vararg params: String?): Void? {
            val message = params[0] ?: return null

            try {
                outputStream.write(message.toByteArray())
                outputStream.flush()
                Log.d("mens-e", "Mensaje enviado: $message")
            } catch (e: Exception) {
                e.printStackTrace()
                errorCallback?.invoke("Error al enviar el mensaje: ${e.message}")
            }

            return null
        }
    }
}
