import android.os.AsyncTask
import android.util.Log
import java.io.OutputStream

class SendMessageTask(private val outputStream: OutputStream) : AsyncTask<String, Void, Void>() {
    override fun doInBackground(vararg params: String?): Void? {
        val message = params[0] ?: return null

        try {
            // Delay de 1 segundo (1000 milisegundos)
            //Thread.sleep(1000)

            outputStream.write(message.toByteArray())
            outputStream.flush()
            Log.d("mens-e", "Mensaje enviado: $message")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
