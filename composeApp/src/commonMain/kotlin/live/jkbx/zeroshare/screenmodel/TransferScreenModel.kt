package live.jkbx.zeroshare.screenmodel

import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.DownloadResponse
import live.jkbx.zeroshare.models.SSERequest
import live.jkbx.zeroshare.models.SSEType
import live.jkbx.zeroshare.models.TypedSSERequest
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.socket.KtorServer
import live.jkbx.zeroshare.socket.SocketStream
import live.jkbx.zeroshare.utils.toJsonElement
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TransferScreenModel : ScreenModel, KoinComponent {
    private val log by injectLogger("TransferScreenModel")
    private val backendApi by inject<BackendApi>()
    private val json by inject<Json>()

    val devices = mutableStateOf<List<Device>>(emptyList())
    val defaultDevice = mutableStateOf<Device?>(null)
    val selectedFileMeta = mutableStateOf<FileTransferMetadata?>(null)
    val selectedFile = mutableStateOf<PlatformFile?>(null)

    val incomingFileDialog = mutableStateOf(false)
    val incomingFile = mutableStateOf<FileTransferMetadata?>(null)
    val incomingDevice = mutableStateOf<Device?>(null)

    private val socketStream = SocketStream(json)
    private val ktorServer = KtorServer(log)

    init {
        screenModelScope.launch {

            val _devices = backendApi.getDevices()

            devices.value = _devices
            defaultDevice.value = _devices.firstOrNull { uniqueDeviceId() != it.deviceId }

            val id = _devices.first { it.deviceId == uniqueDeviceId() }.iD

            log.d { "Device id is $id" }
            val device = devices.value.first { it.deviceId == uniqueDeviceId() }

            socketStream.startListening(
                device,
                onDownloadRequest = {
                    log.d { "Received download request $it" }
                    incomingFileDialog.value = true
                    incomingFile.value = it.data
                    incomingDevice.value = it.device
                },
                onAcknowledgement = {
                    log.d { "Received acknowledgement $it" }
                    val accept = it.data
                    if (accept) {
                        screenModelScope.launch {
                            val randomId = ktorServer.startServer(selectedFile.value!!)
                            incomingDevice.value = it.device
                            sendDownloadResponse(device.ipAddress, randomId)
                        }
                    }
                },
                onDownloadComplete = {
                    log.d { "Received download complete $it" }
                    ktorServer.stopServer()
                },
                onDownloadResponse = {
                    log.d { "Received download response $it" }
                    val downloadResponse = it.data
                    ktorServer.downloadFile(downloadResponse.downloadUrl, downloadResponse.fileName, onCompleted = {
                        sendDownloadComplete()
                    })
                }
            )

        }
    }


    private fun sendDownloadResponse(ipAddress: String, randomId: String) {
        val downloadResponse = DownloadResponse(
            downloadUrl = "http://$ipAddress:6969/download/$randomId",
            fileName = selectedFileMeta.value!!.fileName
        )
        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_RESPONSE,
            data = downloadResponse,
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )
        socketStream.send(sseRequest)
    }

    private fun sendDownloadComplete() {
        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_COMPLETE,
            data = "",
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )
        socketStream.send(sseRequest)
    }

    fun sendDownloadRequest(id: String, fileMetadata: FileTransferMetadata, file: PlatformFile) {

        val sseRequest = TypedSSERequest(
            type = SSEType.DOWNLOAD_REQUEST,
            data = fileMetadata,
            uniqueId = uniqueDeviceId(),
            deviceId = id,
            senderId = uniqueDeviceId()
        )

        selectedFileMeta.value = fileMetadata
        selectedFile.value = file

        socketStream.send(sseRequest)
    }

    fun sendAcknowledgement(accept: Boolean) {
        val sseRequest = TypedSSERequest(
            type = SSEType.ACKNOWLEDGEMENT,
            data = accept,
            uniqueId = uniqueDeviceId(),
            deviceId = incomingDevice.value!!.iD,
            senderId = uniqueDeviceId()
        )

        socketStream.send(sseRequest)
    }

    override fun onDispose() {
        super.onDispose()
    }
}