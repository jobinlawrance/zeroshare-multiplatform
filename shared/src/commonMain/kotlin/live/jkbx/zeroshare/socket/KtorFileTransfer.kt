package live.jkbx.zeroshare.socket

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.socket.FileTransfer.Companion.CHUNK_SIZE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.time.measureTime

class KtorFileTransfer : FileTransfer, KoinComponent {

    private val log by injectLogger("KtorFileTransfer")
    private val kmpHashing by inject<KmpHashing>()
    private val jsonFormatter = Json { prettyPrint = false }

    override fun startServer(onFileReceived: (FileTransferMetadata, ByteArray) -> Unit): Job =
        CoroutineScope(Dispatchers.IO).launch {
            val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind("0.0.0.0", 6969)

            while (true) {
                val clientSocket = serverSocket.accept()
                launch {
                    try {
                        val input = clientSocket.openReadChannel()
                        // Read metadata length (4 bytes, Big Endian)
                        val metadataLength = input.readInt()

                        // Read metadata
                        val metadataBytes = ByteArray(metadataLength)
                        input.readFully(metadataBytes)
                        val metadataJson = metadataBytes.toString()
                        log.d { "Metadata json is $metadataJson" }

                        val metadata =
                            jsonFormatter.decodeFromString<FileTransferMetadata>(metadataJson)

                        log.d { "Metadata is $metadata" }

                        // Read file content
                        var receivedSize = 0L
                        val fileBytes =
                            ByteArray(metadata.fileSize.toInt()) // Preallocate the file size
                        val buffer = ByteArray(CHUNK_SIZE)

                        while (receivedSize < metadata.fileSize) {
                            val bytesRead = input.readAvailable(buffer)
                            if (bytesRead == -1) break

                            // Copy data using a loop
                            for (i in 0 until bytesRead) {
                                fileBytes[receivedSize.toInt() + i] = buffer[i]
                            }
                            receivedSize += bytesRead
                        }

                        val hash = kmpHashing.getSha256Hash(fileBytes)
                        if (hash == metadata.mimeType) {
                            onFileReceived(metadata, fileBytes)
                        } else {
                            throw IllegalStateException("File hash mismatch")
                        }
                    } catch (e: Exception) {
                        log.e(e) { "Error during file transfer: ${e.message}" }
                    } finally {
                        clientSocket.close()
                    }
                }
            }
        }

    override suspend fun sendFile(
        destinationIp: String,
        fileWrapper: SocketFileWrapper,
        listener: FileTransferListener?
    ) = withContext(Dispatchers.IO) {
        val client =
            aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(destinationIp, 6969)
        val output = client.openWriteChannel(autoFlush = true)

        val fileBytes = fileWrapper.toByteArray()
        val metadata = FileTransferMetadata(
            fileName = fileWrapper.getName(),
            fileSize = fileBytes.size.toLong(),
            mimeType = kmpHashing.getSha256Hash(fileBytes),
            extension = null
        )
        val metadataJson = jsonFormatter.encodeToString(FileTransferMetadata.serializer(), metadata)
        val metadataBytes = metadataJson.toByteArray(Charsets.UTF_8)

        // Send metadata length (4 bytes, Big Endian)
        output.writeInt(metadataBytes.size)
        output.flush()

        // Send metadata
        output.writeFully(metadataBytes)
        output.flush()

        // Transfer file with progress tracking
        val totalSize = fileBytes.size
        var transferredBytes = 0

        val transferTimeMillis = measureTime {
            var offset = 0
            val chunkSize = CHUNK_SIZE

            while (offset < totalSize) {
                val bytesToWrite = minOf(chunkSize, totalSize - offset)
                output.writeFully(fileBytes, offset, bytesToWrite)
                output.flush()

                offset += bytesToWrite
                transferredBytes += bytesToWrite

                // Progress update
                val progress = (transferredBytes.toFloat() / totalSize) * 100
                val speedMbps = calculateTransferSpeed(transferredBytes)

                listener?.onTransferProgress(progress)
                listener?.onSpeedUpdate("${speedMbps.first} ${speedMbps.second}")
            }
        }

        listener?.onTransferComplete()
        true
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
}