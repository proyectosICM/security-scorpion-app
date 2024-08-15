import android.os.AsyncTask
import android.util.Log
import java.io.BufferedReader
import java.util.regex.Pattern

class ReceiveMessageTask(private val reader: BufferedReader, private val callback: (String?) -> Unit) : AsyncTask<Void, Void, String?>() {
    override fun doInBackground(vararg params: Void?): String? {
        return try {
            val message = reader.readLine()
            Log.d("mens-r", "Mensaje recibido: $message")
            message
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(result: String?) {
        callback(result)
    }
}
