package live.jkbx.zeroshare.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SSERequest(
    val type: SSEType,
    val data: JsonElement,
    val uniqueId: String
)

enum class SSEType {
    ACKNOWLEDGEMENT,
    DOWNLOAD_REQUEST,
}

@Serializable
data class SSEResponse(
    val type: SSEType,
    val data: JsonElement,
    val device: Device
)


