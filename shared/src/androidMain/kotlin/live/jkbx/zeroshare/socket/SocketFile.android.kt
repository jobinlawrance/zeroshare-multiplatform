package live.jkbx.zeroshare.socket

import androidx.core.net.toFile
import io.github.vinceglb.filekit.core.PlatformFile
import java.io.File
import java.security.MessageDigest

// Common interface for file representation across platforms
class FileWrapperAndroid(private val file: File) : SocketFileWrapper {
    override fun toByteArray(): ByteArray {
        return file.readBytes()
    }

    override fun getName(): String {
        return file.name
    }

    override fun getSize(): Long {
        return file.length()
    }

    override fun getPath(): String {
        return file.absolutePath
    }
}

actual fun fromPlatformFile(file: PlatformFile): SocketFileWrapper {
    return FileWrapperAndroid(file.uri.toFile())
}

actual object FileSaver {
    actual fun saveFile(fileName: String, fileBytes: ByteArray) {
        val downloadsDir =
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, fileName)
        file.writeBytes(fileBytes)
        println("File saved to ${file.absolutePath}")
    }
}

class KmpHashingAndroidImpl : KmpHashing {
    override fun getSha256Hash(bytesArray: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytesArray)
            .joinToString("") { "%02x".format(it) }
    }
}

actual fun getPublicDirectory(): String {
    return android.os.Environment
        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        .absolutePath
}