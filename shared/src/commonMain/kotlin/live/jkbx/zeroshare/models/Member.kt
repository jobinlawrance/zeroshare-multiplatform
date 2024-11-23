package live.jkbx.zeroshare.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Member(
    @SerialName("activeBridge")
    val activeBridge: Boolean,

    @SerialName("address")
    val address: String,

    @SerialName("authenticationExpiryTime")
    val authenticationExpiryTime: Long,

    @SerialName("authorized")
    val authorized: Boolean,

    @SerialName("capabilities")
    val capabilities: List<String>,

    @SerialName("creationTime")
    val creationTime: Long,

    @SerialName("id")
    val id: String,

    @SerialName("identity")
    val identity: String,

    @SerialName("ipAssignments")
    val ipAssignments: List<String>,

    @SerialName("lastAuthorizedCredential")
    val lastAuthorizedCredential: String?,

    @SerialName("lastAuthorizedCredentialType")
    val lastAuthorizedCredentialType: String,

    @SerialName("lastAuthorizedTime")
    val lastAuthorizedTime: Long,

    @SerialName("lastDeauthorizedTime")
    val lastDeauthorizedTime: Long,

    @SerialName("name")
    val name: String,

    @SerialName("noAutoAssignIps")
    val noAutoAssignIps: Boolean,

    @SerialName("nwid")
    val nwid: String,

    @SerialName("objtype")
    val objtype: String,

    @SerialName("remoteTraceLevel")
    val remoteTraceLevel: Int,

    @SerialName("remoteTraceTarget")
    val remoteTraceTarget: String?,

    @SerialName("revision")
    val revision: Int,

    @SerialName("ssoExempt")
    val ssoExempt: Boolean,

    @SerialName("tags")
    val tags: List<String>,

    @SerialName("vMajor")
    val vMajor: Int,

    @SerialName("vMinor")
    val vMinor: Int,

    @SerialName("vProto")
    val vProto: Int,

    @SerialName("vRev")
    val vRev: Int,

    @SerialName("platform")
    val platform: String,
)
