package live.jkbx.zeroshare.socket

import io.github.vinceglb.filekit.core.PlatformFile
import java.security.MessageDigest

// Common interface for file representation across platforms

class FileWrapperJVM(private val file: java.io.File): SocketFileWrapper {
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
    return FileWrapperJVM(file.file)
}

actual object FileSaver {
    actual fun saveFile(fileName: String, fileBytes: ByteArray) {
        val userHome = System.getProperty("user.home")
        val file = java.io.File("$userHome/Documents/$fileName")
        file.writeBytes(fileBytes)
        println("File saved to ${file.absolutePath}")
    }
}

class KmpHashingJVMImpl : KmpHashing {
    override fun getSha256Hash(bytesArray: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytesArray)
            .joinToString("") { "%02x".format(it) }
    }
}

actual fun getPublicDirectory(): String {
    val userHome = System.getProperty("user.home")
    return "$userHome/Documents"
}