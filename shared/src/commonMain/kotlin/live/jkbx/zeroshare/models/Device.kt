package live.jkbx.zeroshare.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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