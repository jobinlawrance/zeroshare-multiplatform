package live.jkbx.zeroshare.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SSEEvent(
    @SerialName("auth_token") val token: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("zt_network_id") val networkId: String
)