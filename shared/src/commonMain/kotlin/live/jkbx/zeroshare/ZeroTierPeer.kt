package live.jkbx.zeroshare

interface ZeroTierPeer {
    suspend fun startServer(port: Int)
    suspend fun sendMessage(remoteAddr: String,port: Int, message: String)
    fun stop()
}