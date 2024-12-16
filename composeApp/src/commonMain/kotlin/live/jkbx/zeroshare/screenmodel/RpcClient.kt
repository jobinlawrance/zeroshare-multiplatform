package live.jkbx.zeroshare.screenmodel

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import live.jkbx.zeroshare.rpc.common.DeviceStream
import live.jkbx.zeroshare.rpc.common.SSERequest
import live.jkbx.zeroshare.rpc.common.SSEResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RpcClient : KoinComponent {
    val client by inject<HttpClient>()

    val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun sendMessage(request: Flow<SSERequest>): Flow<SSEResponse>  = flow {
        val channel = Channel<SSEResponse>(Channel.UNLIMITED)
        coroutineScope.launch {
                try {
                    streamScoped {
                        getRpcClient().withService<DeviceStream>().subscribe(request).collect { sseResponse ->
                            println { "Sending response to channel: $sseResponse" }
                            channel.trySend(sseResponse).isSuccess
                        }
                    }
                } catch (e: Exception) {
                    println { "Error in stream subscription: ${e.message}" }
                }

        }
        for (response in channel) {
            emit(response) // Emit responses to the outer flow
        }
    }

    private suspend fun getRpcClient(): KtorRPCClient {
        return client.rpc {
            url("ws://localhost:8080/stream")
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }
}