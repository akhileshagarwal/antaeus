package io.pleo.antaeus.core.lock

import mu.KotlinLogging
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction
import redis.clients.jedis.exceptions.JedisException
import java.time.Duration
import java.util.*

class DistributedLocks(private val lockProvider: LockProvider<Jedis>) {
    private val log = KotlinLogging.logger {}
    fun tryLockWithTimeout(lockKey: String, lockDuration: Duration, acquireTimeout: Duration, lockIdentifier: String?): Boolean{
        try {
            lockProvider.lockPool().use { jedis ->
                val lockExpireInSec: Long = lockDuration.toSeconds()
                val acquireEndTimeInMillis = System.currentTimeMillis() + acquireTimeout.toMillis()
                while (System.currentTimeMillis() < acquireEndTimeInMillis) {
                    if (jedis.setnx(lockKey, lockIdentifier) == 1L) {
                        jedis.expire(lockKey, lockExpireInSec)
                    }
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        log.error("Interrupted while taking the lock")
                        Thread.currentThread().interrupt()
                    }
                }
            }
        } catch (e: JedisException) {
            log.error("Exception occurred while acquiring lock", e)
            return false
        }
        return true
    }

    fun releaseLock(lockKey: String, identifier: String): Boolean {
        Objects.requireNonNull(lockKey)
        Objects.requireNonNull(identifier)
        try {
            lockProvider.lockPool().use { conn ->
                // Determine whether the lock is the one returned from the previous value. If the lock is deleted, the lock has already been released.
                if (identifier == conn.get(lockKey)) {
                    // If the value of the lockKey changes before the transaction is executed, the transaction will not be completed successfully.
                    conn.watch(lockKey)
                    val transaction: Transaction = conn.multi()
                    transaction.del(lockKey)
                    transaction.exec()
                    conn.unwatch()
                }
            }
        } catch (e: JedisException) {
            log.error("Exception occurred while releasing lock", e)
            return false
        }
        return true
    }
}