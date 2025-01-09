package live.jkbx.zeroshare.screenmodel

import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.DownloadResponse
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.network.baseUrl
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.socket.KtorServer
import live.jkbx.zeroshare.socket.SocketStream
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class TransferScreenModel : ScreenModel, KoinComponent {
    private val log by injectLogger("TransferScreenModel")
    private val backendApi by inject<BackendApi>()

    val devices = mutableStateOf<List<Device>>(emptyList())
    val defaultDevice = mutableStateOf<Device?>(null)
    val selectedFileMeta = mutableStateOf<FileTransferMetadata?>(null)
    val selectedFile = mutableStateOf<PlatformFile?>(null)

    val incomingFileDialog = mutableStateOf(false)
    val incomingFile = mutableStateOf<FileTransferMetadata?>(null)
    val incomingDevice = mutableStateOf<Device?>(null)

    private val socketStream = SocketStream()
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
                            val randomId =
                                ktorServer.startServer(selectedFile.value!!, selectedFileMeta.value!!.fileSize)
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
                    backendApi.downloadFile(downloadResponse.downloadUrl, downloadResponse.fileName, onCompleted = {
                        sendDownloadComplete()
                    })
                }
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun sendDownloadResponse(ipAddress: String, randomId: String) {
        val url = "http://$ipAddress:6969/download/$randomId"
        val proxyUrl = Base64.encode(url.toByteArray())
        val downloadResponse = DownloadResponse(
            downloadUrl = url,
            fileName = selectedFileMeta.value!!.fileName,
            proxyUrl = "$baseUrl/proxy/$proxyUrl"
        )
        socketStream.sendDownloadResponse(downloadResponse, uniqueDeviceId(), incomingDevice.value!!)
    }

    private fun sendDownloadComplete() {
        socketStream.sendDownloadComplete(uniqueDeviceId(), incomingDevice.value!!)
    }

    fun sendDownloadRequest(id: String, fileMetadata: FileTransferMetadata, file: PlatformFile) {
        selectedFileMeta.value = fileMetadata
        selectedFile.value = file
        socketStream.sendDownloadRequest(id, fileMetadata, uniqueDeviceId())
    }

    fun sendAcknowledgement(accept: Boolean) {
        socketStream.sendAcknowledgement(accept, incomingDevice.value!!, uniqueDeviceId())
    }

    override fun onDispose() {
        super.onDispose()
    }
}