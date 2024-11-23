package live.jkbx.zeroshare

import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile

interface ZeroTierPeer {
    suspend fun startServer(port: Int)
    suspend fun sendMessage(remoteAddr: String,port: Int, message: String)
    fun stop()
    suspend fun sendFile(remoteAddr: String, port: Int, file: PlatformFile)
}