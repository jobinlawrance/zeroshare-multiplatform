package live.jkbx.zeroshare.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.InetAddress
import java.net.URI

actual fun openUrlInBrowser(url: String) {
    Desktop.getDesktop().browse(URI(url))
}

actual suspend fun getMachineName(): String {
    return try {
        withContext(Dispatchers.IO) {
            InetAddress.getLocalHost()
        }.hostName
    } catch (e: Exception) {
        throw IllegalStateException("Failed to get machine name", e)
    }
}