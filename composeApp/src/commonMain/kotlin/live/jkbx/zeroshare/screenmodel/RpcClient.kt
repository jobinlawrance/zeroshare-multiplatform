package live.jkbx.zeroshare.screenmodel

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRPCClient
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class RpcClient : KoinComponent {
    private val client by inject<HttpClient>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

//    fun subscribe(request: Flow<SSERequest>, device: Device): Flow<SSEResponse> = flow {
//        val channel = Channel<SSEResponse>(Channel.UNLIMITED)
//        coroutineScope.launch {
//                try {
//                    streamScoped {
//                        getRpcClient().withService<DeviceStream>().subscribe(request, device).collect { sseResponse ->
//                            println { "Sending response to channel: $sseResponse" }
//                            channel.trySend(sseResponse).isSuccess
//                        }
//                    }
//                } catch (e: Exception) {
//                    println { "Error in stream subscription: ${e.message}" }
//                }
//
//        }
//        for (response in channel) {
//            emit(response) // Emit responses to the outer flow
//        }
//    }
//
//    private suspend fun getRpcClient(): KtorRPCClient {
//        val openTelemetry = getOpenTelemetry(serviceName = "krpc-client")
//
//        return client.rpc {
////            url("ws://localhost:8080/stream")
//            url("wss://zeroshare-krpc.jkbx.live/stream")
//            rpcConfig {
//                serialization {
//                    json()
//                }
//            }
//        }
//    }
}