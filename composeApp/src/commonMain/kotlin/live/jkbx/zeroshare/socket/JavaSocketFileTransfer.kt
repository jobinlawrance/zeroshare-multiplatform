package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.socket.FileTransfer.Companion.CHUNK_SIZE
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import kotlin.math.roundToInt
import kotlin.time.measureTime

@Serializable
enum class TransferStatus {
    SUCCESS,
    FAILED,
    HASH_MISMATCH
}

class JavaSocketFileTransfer(
    private val log: Logger,
    private val host: String = "0.0.0.0",
    private val port: Int = 6969
): FileTransfer {

    private val jsonFormatter = Json { prettyPrint = false }

    // Update startServer method in Kotlin
    override fun startServer(
        onFileReceived: (FileTransferMetadata, ByteArray) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName(host))

        while (isActive) {
            val clientSocket = serverSocket.accept()

            launch {
                val outputStream = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream()))
                try {
                    // Input streams
                    val inputStream = DataInputStream(BufferedInputStream(clientSocket.getInputStream()))

                    // Receive metadata
                    val metadataJson = inputStream.readUTF()
                    log.d { "Metadata is $metadataJson" }
                    val metadata = jsonFormatter.decodeFromString<FileTransferMetadata>(metadataJson)

                    // Receive file
                    val receivedBytes = receiveFileBytes(inputStream, metadata)

                    // Verify and process file
                    val hash = calculateFileHash(receivedBytes)
                    if (hash == metadata.fileHash) {
                        onFileReceived(metadata, receivedBytes)
                        // Send success acknowledgement
                        outputStream.writeUTF(TransferStatus.SUCCESS.name)
                    } else {
                        // Send hash mismatch acknowledgement
                        outputStream.writeUTF(TransferStatus.HASH_MISMATCH.name)
                        throw IllegalStateException("File hash mismatch")
                    }
                } catch (e: Exception) {
                    // Send failure acknowledgement
                    outputStream.writeUTF(TransferStatus.FAILED.name)
                    log.e(e) {"File transfer error: ${e.message}"}
                } finally {
                    outputStream.flush()
                    clientSocket.close()
                }
            }
        }
    }

    // Update sendFile method
    override suspend fun sendFile(
        destinationIp: String,
        fileWrapper: SocketFileWrapper,
        listener: FileTransferListener?
    ) = withContext(Dispatchers.IO) {
        Socket(destinationIp, port).use { socket ->
            try {
                val outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                val inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))

                // Prepare file data
                val fileBytes = fileWrapper.toByteArray()
                val metadata = FileTransferMetadata(
                    fileName = fileWrapper.getName(),
                    fileSize = fileBytes.size.toLong(),
                    fileHash = calculateFileHash(fileBytes),
                    transferType = TransferType.UPLOAD
                )

                // Send metadata
                val metadataJson = jsonFormatter.encodeToString(metadata)
                outputStream.writeUTF(metadataJson)
                outputStream.flush()

                // Transfer file with progress tracking
                val totalSize = fileBytes.size
                var transferredBytes = 0

                val transferTime = measureTime {
                    var offset = 0
                    while (offset < totalSize) {
                        val chunkSize = minOf(FileTransfer.CHUNK_SIZE, totalSize - offset)
                        outputStream.write(fileBytes, offset, chunkSize)
                        outputStream.flush()

                        transferredBytes += chunkSize
                        offset += chunkSize

                        // Progress and speed updates
                        val progress = (transferredBytes.toFloat() / totalSize) * 100
                        val speedMbps = calculateTransferSpeed(transferredBytes)

                        listener?.onTransferProgress(progress)
                        listener?.onSpeedUpdate("${speedMbps.first} ${speedMbps.second}")
                    }
                }

                // Wait for receiver's acknowledgement
                val status = TransferStatus.valueOf(inputStream.readUTF())
                when (status) {
                    TransferStatus.SUCCESS -> {
                        listener?.onTransferComplete()
                        true
                    }
                    TransferStatus.HASH_MISMATCH -> {
                        listener?.onError(IOException("File hash mismatch on receiver side"))
                        false
                    }
                    TransferStatus.FAILED -> {
                        listener?.onError(IOException("Transfer failed on receiver side"))
                        false
                    }
                }
            } catch (e: Exception) {
                listener?.onError(e)
                false
            }
        }
    }

    private fun receiveFileBytes(
        inputStream: DataInputStream,
        metadata: FileTransferMetadata
    ): ByteArray {
        val receivedBytes = ByteArrayOutputStream()
        var receivedSize = 0L
        val buffer = ByteArray(CHUNK_SIZE)

        while (receivedSize < metadata.fileSize) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break

            receivedBytes.write(buffer, 0, bytesRead)
            receivedSize += bytesRead
        }

        return receivedBytes.toByteArray()
    }

    private fun calculateTransferSpeed(transferredBytes: Int): Pair<Float, String> {
        val speedBps = transferredBytes.toFloat()
        return when {
            speedBps >= 1_000_000_000 ->
                Pair((speedBps / 1_000_000_000).roundToInt().toFloat(), "GBPS")
            speedBps >= 1_000_000 ->
                Pair((speedBps / 1_000_000).roundToInt().toFloat(), "MBPS")
            else ->
                Pair((speedBps / 1_000).roundToInt().toFloat(), "KBPS")
        }
    }

    private fun calculateFileHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}

// Speed Calculator Implementation
class DefaultSpeedCalculator : SpeedCalculator {
    override fun calculateSpeed(transferredBytes: Long, durationMillis: Long): Double {
        return transferredBytes.toDouble() / (durationMillis / 1000.0)
    }

    override fun formatSpeed(speedBps: Double): String {
        return when {
            speedBps >= 1_000_000_000 ->
                "${(speedBps / 1_000_000_000).roundToInt()} GBPS"
            speedBps >= 1_000_000 ->
                "${(speedBps / 1_000_000).roundToInt()} MBPS"
            else ->
                "${(speedBps / 1_000).roundToInt()} KBPS"
        }
    }
}