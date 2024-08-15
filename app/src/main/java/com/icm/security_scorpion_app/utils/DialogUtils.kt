package com.icm.security_scorpion_app.utils

import android.content.Context
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
}