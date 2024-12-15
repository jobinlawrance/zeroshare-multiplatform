package live.jkbx.zeroshare.rpc.common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.UnifiedJedis


class RedisPubSub {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var jedis: UnifiedJedis = UnifiedJedis("redis://localhost:6379")

    suspend fun subscribe(channel: String, onMessage: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        println("Subscribing to $channel")
        jedis.subscribe(object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                coroutineScope.launch {
                    onMessage(message)
                }
            }
        }, channel)
    }

    suspend fun publish(channel: String, message: String) = withContext(Dispatchers.IO) {
        jedis.publish(channel, message)
    }

    fun close() {
        jedis.close()
    }
}
