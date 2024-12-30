package live.jkbx.zeroshare.rpc.common

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DownloadResponse(
    val downloadUrl: String,
    val fileName: String
)

@Serializable
data class SSERequest(
    val deviceId: String,
    val senderId: String,
    val type: SSEType,
    val data: JsonElement,
    val uniqueId: String
)

enum class SSEType {
    ACKNOWLEDGEMENT,
    DOWNLOAD_REQUEST,
    DOWNLOAD_RESPONSE,
}

@Serializable
data class SSEResponse(
    val type: SSEType,
    val data: JsonElement,
    val device: Device
)

@Serializable
data class Device(
    @SerialName("Created")
    val created: Int,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("ID")
    val iD: String,
    @SerialName("ip_address")
    val ipAddress: String,
    @SerialName("machine_name")
    val machineName: String,
    @SerialName("platform")
    val platform: String,
    @SerialName("updated")
    val updated: Long,
    @SerialName("user_id")
    val userId: String
)

@Rpc
interface DeviceStream: RemoteService {
    suspend fun subscribe(request: Flow<SSERequest>, device: Device): Flow<SSEResponse>
}