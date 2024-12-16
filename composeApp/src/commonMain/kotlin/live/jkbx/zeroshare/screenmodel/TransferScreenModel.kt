package live.jkbx.zeroshare.screenmodel

import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import live.jkbx.zeroshare.di.injectLogger
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.network.BackendApi
import live.jkbx.zeroshare.rpc.common.SSERequest
import live.jkbx.zeroshare.rpc.common.SSEType
import live.jkbx.zeroshare.socket.FileTransferMetadata
import live.jkbx.zeroshare.utils.uniqueDeviceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class TransferScreenModel : ScreenModel, KoinComponent {
    private val log by injectLogger("TransferScreenModel")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val backendApi by inject<BackendApi>()
    private val json by inject<Json>()
    private val rpcClient = RpcClient()

    val devices = mutableStateOf<List<Device>>(emptyList())
    val defaultDevice = mutableStateOf<Device?>(null)
    val receiveRequest = mutableStateOf<FileTransferMetadata?>(null)
    val receiver = mutableStateOf<Device?>(null)

    init {
        screenModelScope.launch {

            val _devices = backendApi.getDevices()

            devices.value = _devices
            defaultDevice.value = _devices.firstOrNull { uniqueDeviceId() != it.deviceId }

            val id = _devices.first { it.deviceId == uniqueDeviceId() }.iD

            log.d { "Device id is $id" }

//            //TODO - Start a SSE event here to listen to incoming files
//            backendApi.receiveMessage(id) { sseData ->
//                log.d { "Received message $sseData" }
//                when (sseData.type) {
//                    SSEType.DOWNLOAD_REQUEST -> {
//                        receiver.value = sseData.device
//                        receiveRequest.value = json.decodeFromJsonElement(
//                            FileTransferMetadata.serializer(),
//                            sseData.data
//                        )
//                    }
//
//                    SSEType.ACKNOWLEDGEMENT -> {
//
//                    }
//                }
//            }
        }
    }

    fun sendToDevice(id: String, file: File, fileMetadata: FileTransferMetadata) {

        val sseRequest = SSERequest(
            type = SSEType.DOWNLOAD_REQUEST,
            data = json.encodeToJsonElement(fileMetadata),
            uniqueId = uniqueDeviceId(),
            deviceId = id,
            senderId = uniqueDeviceId()
        )

        screenModelScope.launch {
            rpcClient.sendMessage(flowOf(sseRequest)).collect {
                log.d { "Received response $it" }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
    }
}