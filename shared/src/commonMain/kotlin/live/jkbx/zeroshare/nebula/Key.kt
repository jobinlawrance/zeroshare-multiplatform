package live.jkbx.zeroshare.nebula

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Key(
    @SerialName("PublicKey") val publicKey: String,
    @SerialName("PrivateKey") val privateKey: String
)
