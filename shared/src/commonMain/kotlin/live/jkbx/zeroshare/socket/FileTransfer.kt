package live.jkbx.zeroshare.socket

import kotlinx.coroutines.Job

interface FileTransfer {
    companion object {
        const val CHUNK_SIZE = 64 * 1024 // 64 KB
    }
    fun startServer(onFileReceived: (FileTransferMetadata, ByteArray) -> Unit = { _, _ -> }): Job
    suspend fun sendFile(
        destinationIp: String,
        fileWrapper: SocketFileWrapper,
        listener: FileTransferListener? = null
    ): Boolean
}