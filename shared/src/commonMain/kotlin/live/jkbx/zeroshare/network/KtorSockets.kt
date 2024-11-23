package live.jkbx.zeroshare.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import live.jkbx.zeroshare.ZeroTierPeer
import live.jkbx.zeroshare.di.injectLogger
import org.koin.core.component.KoinComponent

class KtorPier: ZeroTierPeer, KoinComponent {

    val log by injectLogger("KtorPeer")

    private var serverSocket: ServerSocket? = null

    // Start the server to listen on a given port.
    override suspend fun startServer(port: Int) {
        runBlocking {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", port)
            println("Server is listening at ${serverSocket.localAddress}")
            while (true) {
                val socket = serverSocket.accept()
                println("Accepted $socket")
                launch {
                    val receiveChannel = socket.openReadChannel()
                    val sendChannel = socket.openWriteChannel(autoFlush = true)
                    sendChannel.writeStringUtf8("Please enter your name\n")
                    try {
                        while (true) {
                            val name = receiveChannel.readUTF8Line()
                            sendChannel.writeStringUtf8("Hello, $name!\n")
                        }
                    } catch (e: Throwable) {
                        socket.close()
                    }
                }
            }
        }
    }

    // Handle the client connection
    private suspend fun handleClient(clientSocket: Socket) {
        val input = clientSocket.openReadChannel()
        val output = clientSocket.openWriteChannel(autoFlush = true)

        // Read the incoming message from the client
        val receivedMessage = input.readUTF8Line()
        println("Received: $receivedMessage")

        // Respond to the client (Echo)
        output.writeStringUtf8("Server received: $receivedMessage\n")
        clientSocket.close()
    }

    // Send a message to a remote peer.
    override suspend fun sendMessage(remoteAddr: String, port: Int, message: String) {
        try {
            // Connect to the peer using its ZeroTier IP address
            val socket = aSocket(SelectorManager(Dispatchers.IO))
                .tcp()
                .connect(remoteAddr, port)

            val output = socket.openWriteChannel(autoFlush = true)
            val input = socket.openReadChannel()

            // Send the message to the peer
            println("Sending message: $message")
            output.writeStringUtf8(message)

            // Read the response from the peer
            val response = input.readUTF8Line()
            println("Received from peer: $response")

            // Close the socket connection
            socket.close()
        } catch (e: Exception) {
            println("Error during message sending: ${e}")
        }
    }

    // Stop the server and clean up resources
    override fun stop() {
        serverSocket?.close()
        println("Server stopped.")
    }
}