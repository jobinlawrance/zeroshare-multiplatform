package live.jkbx.zeroshare

import com.mmk.kmpnotifier.notification.NotifierManager
import com.russhwolf.settings.Settings
import com.zerotier.sockets.ZeroTierNative
import com.zerotier.sockets.ZeroTierNode
import com.zerotier.sockets.ZeroTierServerSocket
import com.zerotier.sockets.ZeroTierSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.nodeIdKey
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import java.io.DataInputStream
import java.io.DataOutputStream

actual suspend fun connectToNetwork(networkId: String, onNodeCreated: (String) -> Unit): String {

    val settings by inject<Settings>(Settings::class.java)

    val node = ZeroTierNode()
    node.initFromStorage("id_path")
    node.start()
    while (!node.isOnline) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Node ID: " + String.format("%010x", node.id));
    println("Joining network...");
    node.join(networkId.toLong(16))
    onNodeCreated(String.format("%010x", node.id))
    println("Waiting for network...")
    settings.putString(nodeIdKey, String.format("%010x", node.id))
    while (!node.isNetworkTransportReady(networkId.toLong(16))) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Joined Network")
    val addr4 = node.getIPv4Address(networkId.toLong(16))
    println("IPv4 address = " + addr4.hostAddress)

    return String.format("%010x", node.id)
}

class ZeroTierPeerImpl : ZeroTierPeer, KoinComponent {
    private var serverSocket: ZeroTierServerSocket? = null
    private val connections = mutableListOf<ZeroTierSocket>()
    private var isRunning = true
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val log by injectLogger("ZeroTierPeer")

    override suspend fun startServer(port: Int) {
        withContext(Dispatchers.IO) {
            try {
                serverSocket = ZeroTierServerSocket(port)
                log.d { "Server started on port $port" }

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClient(clientSocket)
                }
            } catch (e: Exception) {
                log.e(e) { "Server error: ${e.message}" }
            }
        }
    }

    private fun handleClient(clientSocket: ZeroTierSocket) {
        connections.add(clientSocket)
        scope.launch {
            try {
                val input = DataInputStream(clientSocket.inputStream)
                val output = DataOutputStream(clientSocket.outputStream)

                while (isRunning) {
                    val message = input.readUTF()
                    log.d { "Received from ${clientSocket.remoteAddress}: $message" }

                    val notifier = NotifierManager.getLocalNotifier()
                    val notificationId = notifier.notify("Ping", message)

                    // Echo back with acknowledgment
//                    output.writeUTF("Received: $message")
                    output.flush()
                }
            } catch (e: Exception) {
                log.e(e) { "Client connection error: ${e.message}" }
            } finally {
                clientSocket.close()
                connections.remove(clientSocket)
            }
        }
    }

    override suspend fun sendMessage(remoteAddr: String, port: Int, message: String) {
        withContext(Dispatchers.IO) {
            try {
                ZeroTierSocket(remoteAddr, port).use { socket ->
                    val output = DataOutputStream(socket.outputStream)
                    val input = DataInputStream(socket.inputStream)

                    output.writeUTF(message)
                    output.flush()

                    // Wait for acknowledgment
                    val response = input.readUTF()
                    log.d { "Server response: $response" }
                }
            } catch (e: Exception) {
                log.d { "Failed to send message: ${e.message}" }
            }
        }
    }

    override fun stop() {
        isRunning = false
        connections.forEach { it.close() }
        serverSocket?.close()
    }
}

private fun ZeroTierSocket.use(block: (ZeroTierSocket) -> Unit) {
    try {
        block(this)
    }finally {
        close()
    }
}