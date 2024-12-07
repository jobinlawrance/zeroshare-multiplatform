package live.jkbx.zeroshare.socket

import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.serialization.Serializable

// Common interface for file representation across platforms
interface SocketFileWrapper {
    fun toByteArray(): ByteArray
    fun getName(): String
    fun getSize(): Long
    fun getPath(): String
}

expect fun fromPlatformFile(file: PlatformFile): SocketFileWrapper

@Serializable
data class FileTransferMetadata(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val extension: String?
)

interface FileTransferListener {
    fun onSpeedUpdate(speedString: String)
    fun onTransferProgress(progress: Float)
    fun onTransferComplete()
    fun onError(throwable: Throwable)
}

enum class TransferType {
    UPLOAD,
    DOWNLOAD
}

// Common interface for speed calculation
interface SpeedCalculator {
    fun calculateSpeed(transferredBytes: Long, durationMillis: Long): Double
    fun formatSpeed(speedBps: Double): String
}

// Enum for speed units
enum class SpeedUnit {
    KBPS,   // Kilobytes per second
    MBPS,   // Megabytes per second
    GBPS    // Gigabytes per second
}

expect object FileSaver {
    fun saveFile(fileName: String, fileBytes: ByteArray)
}

expect fun getPublicDirectory() : String

suspend fun pickFile(): PlatformFile? {
    val files = FileKit.pickFile(
        type = PickerType.File(),
        mode = PickerMode.Single,
    )
    return files
}

interface KmpHashing {
    fun getSha256Hash(bytesArray: ByteArray) : String
}