package live.jkbx.zeroshare.rpc.common

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisPubSub {

    private val uri = RedisURI.Builder.redis("localhost")
        .withPassword("testpass".toCharArray())
        .withDatabase(0)
        .build();

    private val redisClient = RedisClient.create(uri)
    private val pubSubConnection = redisClient.connectPubSub()
    private val coroutineCommands = redisClient.connect().coroutines()

    suspend fun subscribe(channel: String, onMessage: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            pubSubConnection.reactive().observeChannels().subscribe {
                println("Received: ${it.message} on channel: ${it.channel}")
                onMessage(it.message)
            }
            pubSubConnection.async().subscribe(channel)
        }
    }

    suspend fun publish(channel: String, message: String) = withContext(Dispatchers.IO) {
        coroutineCommands.publish(channel, message)
    }

    fun close() {
        pubSubConnection.close()
        redisClient.shutdown()
    }
}
