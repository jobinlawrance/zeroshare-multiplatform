package live.jkbx.zeroshare


expect suspend fun connectToNetwork(networkId: String, onNodeCreated: (String) -> Unit): String