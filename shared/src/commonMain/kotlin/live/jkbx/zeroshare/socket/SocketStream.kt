package live.jkbx.zeroshare.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.DownloadResponse
import live.jkbx.zeroshare.models.SSERequest
import live.jkbx.zeroshare.models.SSEType
import live.jkbx.zeroshare.models.TypedSSERequest
import live.jkbx.zeroshare.models.TypedSSEResponse
import live.jkbx.zeroshare.utils.toBoolean
import live.jkbx.zeroshare.utils.toDownloadResponse
import live.jkbx.zeroshare.utils.toFileMetaData

class SocketStream(val json: Json) {
    //    private val rpcClient = SocketClient("ws://69.69.0.5:4000/stream")
    private val rpcClient = SocketClient("wss://zeroshare.jkbx.live/stream")
    val requestChannel = Channel<SSERequest>(Channel.UNLIMITED)
    val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening(
        device: Device,
        onDownloadRequest: (TypedSSEResponse<FileTransferMetadata>) -> Unit,
        onAcknowledgement: (TypedSSEResponse<Boolean>) -> Unit,
        onDownloadComplete: (TypedSSEResponse<String>) -> Unit,
        onDownloadResponse: (TypedSSEResponse<DownloadResponse>) -> Unit
    ) {
        coroutineScope.launch {
            rpcClient.subscribe(requestChannel.consumeAsFlow(), device).collect { response ->
                when (response.type) {
                    SSEType.DOWNLOAD_RESPONSE -> {
                        val responseData = response.data.toDownloadResponse(json)
                        onDownloadResponse(TypedSSEResponse(response.type, responseData, response.device))
                    }

                    SSEType.ACKNOWLEDGEMENT -> {
                        val accept = response.data.toBoolean(json)
                        onAcknowledgement(TypedSSEResponse(response.type, accept, response.device))
                    }

                    SSEType.DOWNLOAD_COMPLETE -> {
                        onDownloadComplete(TypedSSEResponse(response.type, "", response.device))
                    }

                    SSEType.DOWNLOAD_REQUEST -> {
                        val responseData = response.data.toFileMetaData(json)
                        onDownloadRequest(TypedSSEResponse(response.type, responseData, response.device))
                    }
                }
            }
        }
    }

    inline fun <reified T> send(sseRequest: TypedSSERequest<T>) {
        coroutineScope.launch {
            requestChannel.send(sseRequest.toSSERequest(json))
        }
    }
}

inline fun <reified T> TypedSSERequest<T>.toSSERequest(json: Json): SSERequest {
    return SSERequest(
        type = type,
        data = json.encodeToJsonElement(data),
        uniqueId = uniqueId,
        deviceId = deviceId,
        senderId = senderId
    )
}