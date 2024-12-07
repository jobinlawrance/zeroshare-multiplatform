package live.jkbx.zeroshare.socket

import co.touchlab.kermit.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.slf4j.event.Level
import java.io.File
import java.util.UUID

class KtorServer(private val log: Logger) : KoinComponent {

    private var server: EmbeddedServer<*, *>? = null

    fun startServer(file: File) = CoroutineScope(Dispatchers.IO).launch {
        val randomId = UUID.randomUUID().toString()

        log.d { "Starting Ktor Server with random id $randomId" }
        server = embeddedServer(CIO, port = 6969) {
            install(CallLogging) {
                level = Level.INFO
                logger = getKermitLoggerForSLF4J(log)
            }
            routing {

                get("/download/$randomId") {
                    if (file.exists()) {
                        call.respondFile(file)
                    } else {
                        call.respondText("File not found", status = HttpStatusCode.NotFound)
                    }
                }
            }
        }.start(wait = true)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
    }
}