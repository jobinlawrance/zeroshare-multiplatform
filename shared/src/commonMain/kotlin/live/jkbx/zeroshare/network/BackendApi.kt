package live.jkbx.zeroshare.network

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import org.koin.core.component.KoinComponent

class BackendApi(private val settings: Settings, engine: HttpClientEngine, private val kJson: Json): KoinComponent {
    val log by injectLogger("BackendAPI")

    private val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(kJson)
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    log.v { message }
                }
            }

            level = LogLevel.INFO
        }
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
        }
    }

    suspend fun listenToLogin(token: String, onReceived: () -> Unit) {
        client.sse("http://localhost:4000/sse/$token") {
            while (true) {
                incoming.collect { event ->
                    log.d { "Event from server:" }
                    val sseEvent = parseSseToken(event.data ?: "")
                    log.d { "Token Received $sseEvent" }
                    settings.putString(tokenKey, sseEvent.token)
                    client.close()
                    onReceived()
                }
            }
        }
    }

    private fun parseSseToken(data: String): SSEEvent {
        val event = kJson.decodeFromString<SSEEvent>(data)
        return event
    }
}