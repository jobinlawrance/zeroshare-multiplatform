package live.jkbx.zeroshare.rpc.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext

class DeviceStreamImpl(override val coroutineContext: CoroutineContext) : DeviceStream {
    private val redisPubSub = RedisPubSub()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun subscribe(request: Flow<SSERequest>, device: Device): Flow<SSEResponse> = channelFlow {
        var deviceId: String? = null
        if (deviceId == null) {
            deviceId = device.iD
            redisPubSub.subscribe(deviceId) { message ->
                try {
                    launch(Dispatchers.IO) {
                        send(json.decodeFromString(SSEResponse.serializer(), message))
                    }
                } catch (e: Exception) {
                    println("Error sending to channel: ${e.message}")
                }
            }
            println("Subscribing to $deviceId")
        }
        try {
            request.collect { sseRequest ->

                try {
                    val sseResponse = SSEResponse(
                        type = SSEType.ACKNOWLEDGEMENT,
                        data = sseRequest.data,
                        device = device
                    )
                    redisPubSub.publish(
                        sseRequest.deviceId,
                        json.encodeToString(SSEResponse.serializer(), sseResponse)
                    )
                    println("Published to ${sseRequest.deviceId}")
                } catch (e: Exception) {
                    println("Error during publish: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error during publish: ${e.message}")
        } finally {
//            channel.close()
        }

    }
}