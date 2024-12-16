package live.jkbx.zeroshare.rpc.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

class DeviceStreamImpl(override val coroutineContext: CoroutineContext) : DeviceStream {
    private val redisPubSub = RedisPubSub()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun subscribe(request: Flow<SSERequest>): Flow<SSEResponse> {
        val channel = Channel<SSEResponse>(Channel.UNLIMITED)
        var deviceId: String? = null

        try {
            request.collect { sseRequest ->
                if (deviceId == null) {
                    deviceId = sseRequest.senderId
                    redisPubSub.subscribe(sseRequest.deviceId) { message ->
                        try {
                            launch(Dispatchers.IO) {
                                channel.send(json.decodeFromString(SSEResponse.serializer(), message))
                            }
                        } catch (e: Exception) {
                            println("Error sending to channel: ${e.message}")
                        }
                    }

                }
                try {
                    val sseResponse = SSEResponse(
                        SSEType.ACKNOWLEDGEMENT,
                        sseRequest.data,
                        Device(
                            0,
                            sseRequest.deviceId,
                            sseRequest.senderId,
                            sseRequest.deviceId,
                            "Unknown",
                            "Unknown",
                            0,
                            sseRequest.senderId
                        )
                    )
                    redisPubSub.publish(
                        sseRequest.deviceId,
                        json.encodeToString(SSEResponse.serializer(), sseResponse)
                    )
                } catch (e: Exception) {
                    println("Error during publish: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error during publish: ${e.message}")
        } finally {
//            channel.close()
        }

        return channel.consumeAsFlow()
    }
}