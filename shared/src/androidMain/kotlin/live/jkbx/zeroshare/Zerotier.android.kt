package live.jkbx.zeroshare

import android.content.Context
import com.zerotier.sockets.ZeroTierNative
import com.zerotier.sockets.ZeroTierNode
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalStdlibApi::class)
actual suspend fun connectToNetwork(networkId: String): String {
    val context by inject<Context>(Context::class.java)

    val node = ZeroTierNode()
    val storagePath = context.filesDir.absolutePath
    node.initFromStorage(storagePath)
    node.start()
    while (!node.isOnline) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Node ID: " + node.id.toHexString());
    println("Joining network...");
    node.join(networkId.toLong(16))
    println("Waiting for network...")
    while (!node.isNetworkTransportReady(networkId.toLong(16))) {
        ZeroTierNative.zts_util_delay(50);
    }
    println("Joined Network")
    val addr4  = node.getIPv4Address(networkId.toLong(16))
    println("IPv4 address = " + addr4.hostAddress)

    return addr4.hostAddress!!
}