package live.jkbx.zeroshare.socket

// Common interface for file representation across platforms
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import platform.Foundation.NSData
import platform.Foundation.getBytes
import platform.Foundation.*
import kotlinx.cinterop.*

class FileWrapperNative(private val nsData: NSData, private val fileName: String) : SocketFileWrapper {
    @OptIn(ExperimentalForeignApi::class)
    override fun toByteArray(): ByteArray {
        // Convert NSData to ByteArray
        val bytes = ByteArray(nsData.length.toInt())
        nsData.getBytes(bytes.pin().addressOf(0))
        return bytes
    }

    override fun getName(): String {
        // Implement file name extraction logic
        return fileName
    }

    override fun getSize(): Long {
        return nsData.length.toLong()
    }

    fun toData() = nsData
}

actual fun fromPlatformFile(file: PlatformFile): SocketFileWrapper {
    return FileWrapperNative(file.nsUrl.dataRepresentation, file.name)
}


actual object FileSaver {
    actual fun saveFile(fileName: String, fileBytes: ByteArray) {
        val documentsDir = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
        val filePath = "$documentsDir/$fileName"
        val fileData = fileBytes.toNSData()
        fileData.writeToFile(filePath, true)
        println("File saved to $filePath")
    }

    fun getNSData(bytesArray: ByteArray): NSData {
        return bytesArray.toNSData()
    }

}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

