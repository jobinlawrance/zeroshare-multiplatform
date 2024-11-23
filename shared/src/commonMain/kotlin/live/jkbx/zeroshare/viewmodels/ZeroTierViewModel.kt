package live.jkbx.zeroshare.viewmodels

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.jkbx.zeroshare.ViewModel
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.network.BackendApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ZeroTierViewModel : ViewModel(), KoinComponent {
    private val backendApi by inject<BackendApi>()

    fun creteNetworkURL(sessionToken: String) = backendApi.creteNetworkURL(sessionToken)

    suspend fun listenToLogin(token: String, onReceived: (sseEvent: SSEEvent) -> Unit) {
        backendApi.listenToLogin(token, onReceived)
    }

    suspend fun setNodeId(
        nodeId: String,
        machineName: String,
        networkId: String,
        platformName: String
    ) = backendApi.setNodeId(nodeId, machineName, networkId, platformName)


    suspend fun verifyGoogleToken(token: String): SSEEvent {
        return backendApi.verifyGoogleToken(token)
    }

    suspend fun getZTPeers(networkId: String? = null) = if (networkId == null)
        backendApi.getZTPeers()
    else
        backendApi.getZTPeers(networkId)


    fun startServer(port: Int) = runBlocking {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selectorManager).tcp().bind(InetSocketAddress("0.0.0.0", port))

        println("Starting server on port $port...")

        try {
            while (true) {
                val socket = serverSocket.accept()
                println("Accepted connection from ${socket.remoteAddress}")

                launch {
                    val receiveChannel = socket.openReadChannel()
                    val message = receiveChannel.readUTF8Line()
                    println("Received: $message")

                    socket.close()
                }
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            serverSocket.close()
        }
    }
}