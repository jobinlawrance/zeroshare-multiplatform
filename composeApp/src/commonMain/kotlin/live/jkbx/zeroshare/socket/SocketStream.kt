package live.jkbx.zeroshare.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import live.jkbx.zeroshare.models.Device
import live.jkbx.zeroshare.models.SSERequest
import live.jkbx.zeroshare.models.SSEResponse
import live.jkbx.zeroshare.screenmodel.SocketClient

class SocketStream {
//    private val rpcClient = SocketClient("ws://69.69.0.5:4000/stream")
    private val rpcClient = SocketClient("wss://zeroshare.jkbx.live/stream")
    private val requestChannel = Channel<SSERequest>(Channel.UNLIMITED)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening(device: Device, onReceived: (SSEResponse) -> Unit) {
        coroutineScope.launch {
            rpcClient.subscribe(requestChannel.consumeAsFlow(), device).collect {
                onReceived(it)
            }
        }
    }

    fun send(sseRequest: SSERequest) = coroutineScope.launch {
        requestChannel.send(sseRequest)
    }
}