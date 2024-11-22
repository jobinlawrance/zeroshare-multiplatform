package live.jkbx.zeroshare.models

data class GoogleUser(
    val idToken: String,
    val displayName: String = "",
    val profilePicUrl: String? = null,
)

data class GoogleAuthCredentials(val serverId: String) //Web client ID

