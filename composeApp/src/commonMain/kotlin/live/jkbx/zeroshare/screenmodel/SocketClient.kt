package live.jkbx.zeroshare.screenmodel

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.SSERequest
import live.jkbx.zeroshare.models.SSEResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SocketClient(private val serverUrl: String) : KoinComponent {
    private val client by inject<HttpClient>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val channel = Channel<SSEResponse>(Channel.UNLIMITED)
    private val log by injectLogger("SocketClient")
    private val json by inject<Json>()

    fun subscribe(requests: Flow<SSERequest>, device: Device): Flow<SSEResponse> = flow {
        coroutineScope.launch {
            client.webSocket(serverUrl) {
                try {
                    sendSerialized(device)
                    log.d { "Sending device to server" }
                    launch {
                        requests.collect { request ->
                            sendSerialized(request)
                            log.d { "Sending request to server" }
                        }
                    }
                    // Receive responses from the server
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val response = json.decodeFromString<SSEResponse>(frame.readText())
                            channel.trySend(response).isSuccess
                        }
                    }
                } catch (e: Exception) {
                    log.e(e) { "Error in socket connection" }
                }
            }
        }
        for (response in channel) {
            emit(response)
        }
    }
}