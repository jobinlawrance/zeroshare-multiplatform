package live.jkbx.zeroshare.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import live.jkbx.zeroshare.nebula.IncomingSite

@Serializable
data class SignedKeyResponse(
    @SerialName("signed_key") val signedKey: String,
    @SerialName("ca_cert") val caCert: String,
    @SerialName("incoming_site") val incomingSite: IncomingSite
)
