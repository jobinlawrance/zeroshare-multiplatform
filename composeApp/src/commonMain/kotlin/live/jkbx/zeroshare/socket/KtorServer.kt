package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.readByteArray
import live.jkbx.zeroshare.utils.FileSaver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.event.Level
import java.util.UUID

class KtorServer(private val log: Logger) : KoinComponent {

    private var server: EmbeddedServer<*, *>? = null
    private val client by inject<HttpClient>()

    suspend fun startServer(file: PlatformFile): String {
        val randomId = UUID.randomUUID().toString()

        if (server != null) {
            server?.stop(1000, 2000)
        }

        // Create a suspendable coroutine with cancellable continuation
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO).launch {
                log.d { "Starting Ktor Server with random id $randomId" }

                server = embeddedServer(CIO, port = 6969) {
                    install(CallLogging) {
                        level = Level.ERROR
                        logger = getKermitLoggerForSLF4J(log)
                    }
                    routing {
                        get("/download/$randomId") {
                            call.respondOutputStream(ContentType.Application.OctetStream) {
                                file.getStream().use { platformInputStream ->
                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE) // 8 KB buffer
                                    while (platformInputStream.hasBytesAvailable()) {
                                        val bytesRead = platformInputStream.readInto(buffer, buffer.size)
                                        if (bytesRead > 0) {
                                            this.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Start the server without blocking the coroutine
                server?.start(wait = false)

                // Complete the continuation when the server starts
                continuation.resume(randomId) { throwable ->
                    log.e(throwable) { "Error while starting the server" }
                }
            }
        }
    }

    fun downloadFile(url: String, fileName: String, onCompleted: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val file = FileSaver(fileName)
            client.prepareGet(url).execute { response ->
                if (response.status == HttpStatusCode.OK) {
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    log.d { "Starting to download file" }
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            file.append(bytes)
                        }
                    }
                    log.d { "File downloaded successfully" }
                    onCompleted()
                }
            }
        }
    }

    fun stopServer() {
        log.d { "Stopping server" }
        server?.stop(1000, 2000)
    }
}