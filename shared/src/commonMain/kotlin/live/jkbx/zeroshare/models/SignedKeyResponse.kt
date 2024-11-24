package live.jkbx.zeroshare.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignedKeyResponse(
    @SerialName("signed_key") val signedKey: String,
    @SerialName("ca_cert") val caCert: String
)
