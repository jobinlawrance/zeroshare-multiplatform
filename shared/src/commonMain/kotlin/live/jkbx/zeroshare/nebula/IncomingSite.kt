package live.jkbx.zeroshare.nebula

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IncomingSite(
    val name: String,
    val id: String,
    val staticHostmap: HashMap<String, StaticHosts>,
    val unsafeRoutes: List<UnsafeRoute>?,
    var cert: String,
    var ca: String,
    val lhDuration: Int,
    val port: Int,
    val mtu: Int?,
    val cipher: String,
    val sortKey: Int?,
    val logVerbosity: String?,
    var key: String?,
    val managed: Boolean?,
)

@Serializable
data class StaticHosts(
    val lighthouse: Boolean,
    val destinations: List<String>
)

@Serializable
data class UnsafeRoute(
    val route: String,
    val via: String,
    val mtu: Int?
)

@Serializable
data class CertificateInfo(
    @SerialName("Cert") val cert: Certificate,
    @SerialName("RawCert") val rawCert: String,
    @SerialName("Validity") val validity: CertificateValidity
)

@Serializable
data class Certificate(
    val fingerprint: String,
    val signature: String,
    val details: CertificateDetails
)

@Serializable
data class CertificateDetails(
    val name: String,
    val notBefore: String,
    val notAfter: String,
    val publicKey: String,
    val groups: List<String>,
    val ips: List<String>,
    val subnets: List<String>,
    val isCa: Boolean,
    val issuer: String
)

@Serializable
data class CertificateValidity(
    @SerialName("Valid") val valid: Boolean,
    @SerialName("Reason") val reason: String
)