package com.icm.security_scorpion_app.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.icm.security_scorpion_app.R
object DialogUtils {

    fun showDeleteConfirmationDialog(context: Context, deviceName: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar el dispositivo '$deviceName'?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun showDataOverwriteConfirmationDialog(context: Context, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Confirmación de Datos")
        builder.setMessage("Se eliminarán los datos guardados y se descargarán nuevos datos. ¿Deseas continuar?")
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun showConnectionErrorDialog(context: Context) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Error de Conexión")
        builder.setMessage("No se pudo conectar con el servidor. Por favor, inténtalo de nuevo.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun showLoginDialog(context: Context, onLogin: (username: String, password: String) -> Unit) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Autenticación")

        // Inflar el layout del diálogo
        val view = View.inflate(context, R.layout.dialog_login, null)
        builder.setView(view)

        // Configurar campos de entrada
        val usernameEditText = view.findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)

        builder.setPositiveButton("Iniciar sesión") { dialog, _ ->
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            onLogin(username, password)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    fun showInvalidCredentialsDialog(context: Context) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Error de Autenticación")
        builder.setMessage("Usuario o contraseña incorrectos.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun showGroupNotActiveDialog(context: Context) {
        val builder = AlertDialog.Builder(context, R.style.DialogButtonTextStyle)
        builder.setTitle("Grupo No Activo")
        builder.setMessage("El grupo de dispositivos no está activo en el servidor.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}