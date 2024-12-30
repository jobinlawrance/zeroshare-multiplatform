package live.jkbx.zeroshare.network

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.networkIdKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.Member
import live.jkbx.zeroshare.utils.installOpenTelemetry

import live.jkbx.zeroshare.models.SSEEvent

import live.jkbx.zeroshare.models.SignedKeyResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BackendApi : KoinComponent {
    private val settings: Settings by inject<Settings>()
    private val kJson: Json by inject<Json>()
    private val log by injectLogger("BackendAPI")
    private val client by inject<HttpClient>()

        private val baseUrl = "https://zeroshare.jkbx.live"
//    private val baseUrl = "http://localhost:4000"

    fun creteNetworkURL(sessionToken: String): String {
        return "$baseUrl/login/$sessionToken"
    }

    suspend fun listenToLogin(token: String, onReceived: (sseEvent: SSEEvent) -> Unit) {

        runCatching {
            client.sse("$baseUrl/sse/$token") {
                while (true) {
                    incoming.collect { event ->
                        log.d { "Event from server:" }
                        val sseEvent = parseSseToken(event.data ?: "")
                        settings.putString(tokenKey, sseEvent.token)
                        settings.putString(networkIdKey, sseEvent.networkId)
                        onReceived(sseEvent)
                    }
                }
            }
        }
        client.close()
    }

    suspend fun signPublicKey(publicKey: String, deviceId: String): SignedKeyResponse {
        val req = client.postWithAuth("$baseUrl/nebula/sign-public-key") {
            setBody(mapOf("public_key" to publicKey, "device_id" to deviceId))
        }
        return req.body<SignedKeyResponse>()
    }

    suspend fun getDevices(): List<Device> {
        val req = client.getWithAuth("$baseUrl/devices")
        return req.body<List<Device>>()
    }

//    suspend fun receiveMessage(id: String, onReceived: (sseData: SSEResponse) -> Unit) {
//        runCatching {
//            client.sse(
//                urlString = "$baseUrl/device/receive/$id",
//                request = {
//                    header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
//                }
//            ) {
//                while (true) {
//                    incoming.collect { event ->
//                        log.d { "Event from server: ${event.data}" }
//                        val sseData = kJson.decodeFromString<SSEResponse>(event.data ?: "")
//                        onReceived(sseData)
//                    }
//                }
//            }
//        }
//        client.close()
//    }

//    suspend fun sendMessage(id: String, sseData: SSERequest): HttpStatusCode {
//        val req = client.postWithAuth("$baseUrl/device/send/$id") {
//            setBody(sseData)
//        }
//        return req.body<HttpStatusCode>()
//    }

    private fun parseSseToken(data: String): SSEEvent {
        val event = kJson.decodeFromString<SSEEvent>(data)
        return event
    }

    suspend fun setNodeId(
        nodeId: String,
        machineName: String,
        networkId: String,
        platformName: String
    ): Boolean {

        val req = client.postWithAuth("$baseUrl/node", {
            setBody(
                mapOf(
                    "node_id" to nodeId,
                    "machine_name" to machineName,
                    "network_id" to networkId,
                    "platform" to platformName
                )
            )
        })

        return req.status == HttpStatusCode.OK
    }

    suspend fun setDeviceDetails(
        machineName: String,
        platformName: String,
        deviceId: String
    ): Boolean {
        val req = client.postWithAuth("$baseUrl/device") {
            setBody(
                mapOf(
                    "machine_name" to machineName,
                    "platform" to platformName,
                    "device_id" to deviceId
                )
            )
        }
        return req.status == HttpStatusCode.OK
    }

    suspend fun verifyGoogleToken(token: String): SSEEvent {
        val req = client.postWithAuth("$baseUrl/login/verify-google", {
            setBody(mapOf("token" to token))
        })
        val sseEvent = req.body<SSEEvent>()
        settings.putString(tokenKey, sseEvent.token)
        settings.putString(networkIdKey, sseEvent.networkId)
        return sseEvent
    }

    suspend fun getZTPeers(networkId: String = settings.getString(networkIdKey, "")): List<Member> {
        val req = client.getWithAuth("$baseUrl/peers?networkId=$networkId")
        return req.body<List<Member>>()
    }

    suspend fun HttpClient.postWithAuth(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return post(url, {
            block()
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }

    suspend fun HttpClient.getWithAuth(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return get(url, {
            block()
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }
}


fun getHttpClient(engine: HttpClientEngine, kJson: Json, log: Logger): HttpClient {

    val client = HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }
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
//        install(contentTypePlugin)
//        installRPC {
//            waitForServices = true
//        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(kJson)
        }
//        installOpenTelemetry()
    }
    return client
}
