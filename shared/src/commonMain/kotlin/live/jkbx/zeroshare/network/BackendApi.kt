package live.jkbx.zeroshare.network

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.networkIdKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.SSEEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackendApi: KoinComponent {
    private val settings: Settings by inject<Settings>()
    private val engine: HttpClientEngine by inject<HttpClientEngine>()
    private val kJson: Json by inject<Json>()
    private val log by injectLogger("BackendAPI")
    
    private val baseUrl = "https://zeroshare.jkbx.live"
//    private val baseUrl = "http://localhost:4000"

    private val contentTypePlugin = createClientPlugin("Content-Type") {
        onRequest { request, _ ->
            request.headers.append("Content-Type", "application/json")
        }
    }

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

            level = LogLevel.ALL
        }
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
        }
//        TODO: Not sure why this isn't working
//        install(Auth) {
//            bearer {
//                sendWithoutRequest { true }
//                loadTokens {
//                    BearerTokens(settings.getString(tokenKey, ""), "")
//                }
//            }
//
//        }
        install(contentTypePlugin)
    }

    fun creteNetworkURL(sessionToken: String): String {
        return "$baseUrl/login/$sessionToken"
    }

    suspend fun listenToLogin(token: String, onReceived: (networkId: String) -> Unit) {

        client.sse("$baseUrl/sse/$token") {
            while (true) {
                incoming.collect { event ->
                    log.d { "Event from server:" }
                    val sseEvent = parseSseToken(event.data ?: "")
                    settings.putString(tokenKey, sseEvent.token)
                    settings.putString(networkIdKey, sseEvent.networkId)
                    client.close()
                    onReceived(sseEvent.networkId)
                }
            }
        }
    }

    private fun parseSseToken(data: String): SSEEvent {
        val event = kJson.decodeFromString<SSEEvent>(data)
        return event
    }

    suspend fun setNodeId(nodeId: String, machineName: String, networkId: String): Boolean {

        val req = client.postWithAuth("$baseUrl/node", {
            setBody(mapOf("node_id" to nodeId, "machine_name" to machineName, "network_id" to networkId))
        })

        return req.status == HttpStatusCode.OK
    }

    suspend fun verifyGoogleToken(token: String): SSEEvent {
        val req = client.postWithAuth("$baseUrl/login/verify-google", {
            setBody(mapOf("token" to token))
        })

        return req.body<SSEEvent>()
    }

    suspend fun HttpClient.postWithAuth(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return post(url, {
            block()
            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }

    suspend fun HttpClient.getWithAuth(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return get(url, {
            block()
            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }
}