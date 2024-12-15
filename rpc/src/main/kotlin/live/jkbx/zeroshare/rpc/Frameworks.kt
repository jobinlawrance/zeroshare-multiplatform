package live.jkbx.zeroshare.rpc
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.RPC
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.*
import live.jkbx.zeroshare.rpc.common.DeviceStream
import live.jkbx.zeroshare.rpc.common.DeviceStreamImpl

fun Application.configureFrameworks() {
    install(RPC)
    routing {
        rpc("/stream") {
            rpcConfig {
                serialization {
                    json{
                        ignoreUnknownKeys = true
                    }
                }
            }
        
            registerService<DeviceStream> { ctx -> DeviceStreamImpl(ctx) }
        }
    }
}
