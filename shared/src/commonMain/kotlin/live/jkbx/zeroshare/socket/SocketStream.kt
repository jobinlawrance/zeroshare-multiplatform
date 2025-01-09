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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SocketStream: KoinComponent {
    //    private val rpcClient = SocketClient("ws://69.69.0.5:4000/stream")
    private val rpcClient = SocketClient("wss://zeroshare.jkbx.live/stream")
    private val requestChannel = Channel<SSERequest>(Channel.UNLIMITED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json by inject<Json>()

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

    private inline fun <reified T> send(sseRequest: TypedSSERequest<T>) {
        coroutineScope.launch {
            requestChannel.send(sseRequest.toSSERequest(json))
        }
    }

    private inline fun <reified T> TypedSSERequest<T>.toSSERequest(json: Json): SSERequest {
        return SSERequest(
            type = type,
            data = json.encodeToJsonElement(data),
            uniqueId = uniqueId,
            deviceId = deviceId,
            senderId = senderId
        )
    }

    fun sendAcknowledgement(accept: Boolean, incomingDevice: Device, uniqueDeviceId: String) {
        val sseRequest = TypedSSERequest(
            type = SSEType.ACKNOWLEDGEMENT,
            data = accept,
            uniqueId = uniqueDeviceId,
            deviceId = incomingDevice.iD,
            senderId = uniqueDeviceId
        )
        send(sseRequest)
    }

    fun sendDownloadRequest(id: String, fileMetadata: FileTransferMetadata, uniqueDeviceId: String) {
        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_REQUEST,
            data = fileMetadata,
            uniqueId = uniqueDeviceId,
            deviceId = id,
            senderId = uniqueDeviceId
        )
        send(sseRequest)
    }

    fun sendDownloadComplete(uniqueDeviceId: String, incomingDevice: Device) {
        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_COMPLETE,
            data = "",
            uniqueId = uniqueDeviceId,
            deviceId = incomingDevice.iD,
            senderId = uniqueDeviceId
        )
        send(sseRequest)
    }

    fun sendDownloadResponse(downloadResponse: DownloadResponse, uniqueDeviceId: String, value: Device) {
        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_RESPONSE,
            data = downloadResponse,
            uniqueId = uniqueDeviceId,
            deviceId = value.iD,
            senderId = uniqueDeviceId
        )
        send(sseRequest)
    }
}

