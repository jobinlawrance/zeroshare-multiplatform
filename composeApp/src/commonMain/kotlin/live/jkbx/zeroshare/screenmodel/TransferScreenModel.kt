package live.jkbx.zeroshare.screenmodel

import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.rpc.common.DownloadResponse
import live.jkbx.zeroshare.rpc.common.SSERequest
import live.jkbx.zeroshare.rpc.common.SSEType
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.socket.KtorServer
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransferScreenModel : ScreenModel, KoinComponent {
    private val log by injectLogger("TransferScreenModel")
    private val backendApi by inject<BackendApi>()
    private val json by inject<Json>()
    private val rpcClient = SocketClient("ws://69.69.0.5:4000/stream")

    val devices = mutableStateOf<List<Device>>(emptyList())
    val defaultDevice = mutableStateOf<Device?>(null)
    val selectedFileMeta = mutableStateOf<FileTransferMetadata?>(null)
    val selectedFile = mutableStateOf<PlatformFile?>(null)

    val incomingFileDialog = mutableStateOf(false)
    val incomingFile = mutableStateOf<FileTransferMetadata?>(null)
    val incomingDevice = mutableStateOf<live.jkbx.zeroshare.rpc.common.Device?>(null)

    private val requestChannel = Channel<SSERequest>(Channel.UNLIMITED)
    private val ktorServer = KtorServer(log)

    init {
        screenModelScope.launch {

            val _devices = backendApi.getDevices()

            devices.value = _devices
            defaultDevice.value = _devices.firstOrNull { uniqueDeviceId() != it.deviceId }

            val id = _devices.first { it.deviceId == uniqueDeviceId() }.iD

            log.d { "Device id is $id" }
            val device = devices.value.first { it.deviceId == uniqueDeviceId() }

            rpcClient.subscribe(requestChannel.consumeAsFlow(), device).collect {
                log.d { "Received response $it" }
                when (it.type) {
                    SSEType.DOWNLOAD_REQUEST -> {
                        incomingFileDialog.value = true
                        incomingFile.value = json.decodeFromJsonElement(FileTransferMetadata.serializer(), it.data)
                        incomingDevice.value = it.device
                    }

                    SSEType.ACKNOWLEDGEMENT -> {
                        // start server
                        val randomId = ktorServer.startServer(selectedFile.value!!)
                        incomingDevice.value = it.device
                        sendDownloadResponse(device.ipAddress, randomId)
                    }

                    SSEType.DOWNLOAD_RESPONSE -> {
                        val downloadResponse = json.decodeFromJsonElement(DownloadResponse.serializer(), it.data)
                        ktorServer.downloadFile(downloadResponse.downloadUrl, downloadResponse.fileName, onCompleted = {
                            sendDownloadComplete()
                        })
                    }
                    SSEType.DOWNLOAD_COMPLETE -> {
                        ktorServer.stopServer()
                    }
                }
            }
        }
    }

    private fun sendDownloadResponse(ipAddress: String, randomId: String) {
        val downloadResponse = DownloadResponse(
            downloadUrl = "http://$ipAddress:6969/download/$randomId",
            fileName = selectedFileMeta.value!!.fileName
        )
        val sseRequest = SSERequest(
            type = SSEType.DOWNLOAD_RESPONSE,
            data = json.encodeToJsonElement(downloadResponse),
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )

        screenModelScope.launch {
            requestChannel.send(sseRequest)
        }
    }

    private fun sendDownloadComplete() {
        val sseRequest = SSERequest(
            type = SSEType.DOWNLOAD_COMPLETE,
            data = json.encodeToJsonElement(""),
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )
        screenModelScope.launch {
            requestChannel.send(sseRequest)
        }
    }

    fun sendDownloadRequest(id: String, fileMetadata: FileTransferMetadata, file: PlatformFile) {

        val sseRequest = SSERequest(
            type = SSEType.DOWNLOAD_REQUEST,
            data = json.encodeToJsonElement(fileMetadata),
            uniqueId = uniqueDeviceId(),
            deviceId = id,
            senderId = uniqueDeviceId()
        )

        selectedFileMeta.value = fileMetadata
        selectedFile.value = file

        screenModelScope.launch {
            requestChannel.send(sseRequest)
        }
    }

    fun sendAcknowledgement(accept: Boolean) {
        val sseRequest = SSERequest(
            type = SSEType.ACKNOWLEDGEMENT,
            data = json.encodeToJsonElement(accept),
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )

        screenModelScope.launch {
            requestChannel.send(sseRequest)
        }
    }

    override fun onDispose() {
        super.onDispose()
    }
}