package live.jkbx.zeroshare

import com.zerotier.sockets.ZeroTierNative
import com.zerotier.sockets.ZeroTierNode
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalStdlibApi::class)
actual suspend fun connectToNetwork(networkId: String): String {
    val node = ZeroTierNode()
    node.initFromStorage("id_path")
    node.start()
    while (!node.isOnline) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Node ID: " + String.format("%010x", node.id));
    println("Joining network...");
    node.join(networkId.toLong(16))
    println("Waiting for network...")
    while (!node.isNetworkTransportReady(networkId.toLong(16))) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Joined Network")
    val addr4 = node.getIPv4Address(networkId.toLong(16))
    println("IPv4 address = " + addr4.hostAddress)

    return String.format("%010x", node.id)
}