package io.pleo.antaeus.core.lock

import mu.KotlinLogging
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.net.URI

class RedisLockProvider(private val redisHost: String): LockProvider<Jedis> {
    private val log = KotlinLogging.logger {}
    private var lockPool: JedisPool? = null

    override fun lockPool(): Jedis{
        if (lockPool == null) {
            log.info("Establishing redis lock pool connection")
            val redisHostURI = URI.create(redisHost)
            lockPool = JedisPool(redisHostURI.host, redisHostURI.port)
        }
        return lockPool!!.resource
    }
}