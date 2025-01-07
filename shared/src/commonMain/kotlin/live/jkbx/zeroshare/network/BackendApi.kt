package live.jkbx.zeroshare.network

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.di.networkIdKey
import live.jkbx.zeroshare.di.refreshTokenKey
import live.jkbx.zeroshare.di.tokenKey
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.Member
import live.jkbx.zeroshare.models.SSEEvent
import live.jkbx.zeroshare.models.SignedKeyResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val baseUrl = "https://zeroshare.jkbx.live"
//private val baseUrl = "http://69.69.0.5:4000"

class BackendApi : KoinComponent {
    private val settings: Settings by inject<Settings>()
    private val kJson: Json by inject<Json>()
    private val log by injectLogger("BackendAPI")
    private val client by inject<HttpClient>()


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
                        settings.putString(refreshTokenKey, sseEvent.refreshToken)
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
        settings.putString(refreshTokenKey, sseEvent.refreshToken)
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
//            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }

    suspend fun HttpClient.getWithAuth(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return get(url, {
            block()
            header(HttpHeaders.ContentType, "application/json")
//            header(HttpHeaders.Authorization, "Bearer ${settings.getString(tokenKey, "")}")
        })
    }
}


fun getHttpClient(
    engine: HttpClientEngine,
    kJson: Json,
    log: Logger,
    settings: Settings,
): HttpClient {

    val client = HttpClient(engine) {
        expectSuccess = false
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
        install(Auth) {
            bearer {
                sendWithoutRequest { true }
                loadTokens {
                    BearerTokens(
                        accessToken = settings.getString(tokenKey, ""),
                        refreshToken = settings.getString(refreshTokenKey, "")
                    )
                }
                refreshTokens {
                    val refreshToken = client.post("$baseUrl/refresh") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("refresh_Token" to oldTokens?.refreshToken))
                        markAsRefreshTokenRequest()
                    }
                    try {

                        if (refreshToken.status == HttpStatusCode.OK) {
                            val tokens = refreshToken.body<SSEEvent>()
                            settings.putString(refreshTokenKey, tokens.refreshToken)
                            settings.putString(tokenKey, tokens.token)
                            BearerTokens(
                                accessToken = tokens.token,
                                refreshToken = tokens.refreshToken
                            )
                        } else {
                            log.e { "Refresh token failed" }
                            //TODO: Navigate to login screen
                            null
                        }
                    } catch (e: Exception) {
                        log.e(e) { "Refresh token failed" }
                        //TODO: Navigate to login screen
                        null
                    }
                }

            }

        }
//        install(contentTypePlugin)
//        installRPC {
//            waitForServices = true
//        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(kJson)
            pingInterval = 20.seconds
        }
//        installOpenTelemetry()
    }
    return client
}