package live.jkbx.zeroshare

import com.mmk.kmpnotifier.notification.NotifierManager
import com.russhwolf.settings.Settings
import com.zerotier.sockets.ZeroTierNative
import com.zerotier.sockets.ZeroTierNode
import com.zerotier.sockets.ZeroTierServerSocket
import com.zerotier.sockets.ZeroTierSocket
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.nodeIdKey
import live.jkbx.zeroshare.utils.getPlatform
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

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
                    val header = input.readUTF() // Read header to determine message or file
                    when (header) {
                        "MESSAGE" -> {
                            val message = input.readUTF()
                            log.d { "Received message from ${clientSocket.remoteAddress}: $message" }

                            val notifier = NotifierManager.getLocalNotifier()
                            notifier.notify("Ping", message)

                            // Echo back with acknowledgment
                            output.writeUTF("Received: $message")
                            output.flush()
                        }
                        "FILE" -> {
                            val fileName = input.readUTF()
                            val fileSize = input.readLong()

                            log.d { "Receiving file '$fileName' (${fileSize} bytes) from ${clientSocket.remoteAddress}" }
                            val fileOutput = FileOutputStream(fileName)
                            val buffer = ByteArray(4096)
                            var bytesReceived = 0L

                            while (bytesReceived < fileSize) {
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                fileOutput.write(buffer, 0, bytesRead)
                                bytesReceived += bytesRead
                            }

                            fileOutput.close()

                            log.d { "File '$fileName' received successfully" }
                            output.writeUTF("File received: $fileName")
                            output.flush()
                        }
                        else -> log.e { "Unknown header received: $header" }
                    }
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
        sendData(remoteAddr, port, "MESSAGE") {
            it.writeUTF(message)
        }
    }

    override suspend fun sendFile(remoteAddr: String, port: Int, platformFile: PlatformFile) {
        val file = platformFile.file
        sendData(remoteAddr, port, "FILE") { output ->
            output.writeUTF(file.name)
            output.writeLong(file.length())

            val fileInputStream = FileInputStream(file)
            fileInputStream.copyTo(output)   // Send file contents
            fileInputStream.close()

            output.flush()
            log.d { "File '${file.name}' sent successfully" }
        }
    }

    private suspend fun sendData(remoteAddr: String, port: Int, header: String, dataWriter: (DataOutputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                ZeroTierSocket(remoteAddr, port).use { socket ->
                    val output = DataOutputStream(socket.outputStream)
                    val input = DataInputStream(socket.inputStream)

                    output.writeUTF(header) // Send header
                    dataWriter(output)     // Write data (message or file)
                    output.flush()

                    // Wait for acknowledgment
                    val response = input.readUTF()
                    log.d { "Server response: $response" }
                }
            } catch (e: Exception) {
                log.d { "Failed to send data: ${e.message}" }
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