package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream


class SimpleFTPClient(private val log: Logger) {
    suspend fun sendFile(
        host: String = "localhost",
        port: Int = 21,
        username: String = "ftpuser",
        password: String = "password",
        file: PlatformFile
    ) {
        log.d { "Trying to send"}
        try {
            val ftpClient = FTPClient()
            ftpClient.connect(host, port)
            val success = ftpClient.login(username, password)
            if (!success) {
                log.e { "Login failed." }
                return
            }
            log.d { "Login Success" }
            // Set FTP mode to binary (important for non-text files)
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val inputStream = ByteArrayInputStream(file.readBytes())
            try {
                log.d { "Starting to upload" }
                val uploaded = ftpClient.storeFile(file.name, inputStream)
                if (uploaded) {
                    log.d {"File uploaded successfully." }
                } else {
                    log.d {"File upload failed." }
                }
            } catch (e: IOException) {
                log.e (e) {"Error while uploading the file: " + e.message}
            }

            ftpClient.logout()
            ftpClient.disconnect()

        } catch (e: Exception) {
            log.e(e) { "Error sending file: ${e.message}" }
        }
    }
}
