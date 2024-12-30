package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.core.PlatformFile
import io.ktor.http.*
import io.ktor.server.application.install
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.slf4j.event.Level
import java.util.UUID

class KtorServer(private val log: Logger) : KoinComponent {

    private var server: EmbeddedServer<*, *>? = null

    fun startServer(file: PlatformFile): String {
        val randomId = UUID.randomUUID().toString()
        CoroutineScope(Dispatchers.IO).launch {
            log.d { "Starting Ktor Server with random id $randomId" }
            server = embeddedServer(CIO, port = 6969) {
                install(CallLogging) {
                    level = Level.INFO
                    logger = getKermitLoggerForSLF4J(log)
                }
                routing {
                    file.getStream()
                    get("/download/$randomId") {
                        call.respondOutputStream(ContentType.Application.OctetStream) {
                            file.getStream().use { platformInputStream ->
                                val buffer = ByteArray(8 * 1024) // 8 KB buffer
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

            }.start(wait = true)
        }
        return randomId
    }


    fun stopServer() {
        server?.stop(1000, 2000)
    }
}