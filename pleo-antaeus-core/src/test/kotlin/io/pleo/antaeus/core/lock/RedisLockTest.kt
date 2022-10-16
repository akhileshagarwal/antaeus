package io.pleo.antaeus.core.lock

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.*

@Testcontainers
class RedisLockTest {
    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    @Container
    var redis = KGenericContainer("redis:5.0.3-alpine").withExposedPorts(6379)

    private lateinit var redisLock:RedisLock

    @BeforeEach
    fun setUp() {
        val address = redis.host
        val port = redis.firstMappedPort

        // Now we have an address and port for Redis, no matter where it is running
        redisLock = RedisLock(address, port)
    }

    @Test
    fun `will acquire lock if it is available and release after the lock time has expired`() {
        val ttl = Duration.ofSeconds(2)
        val acquireTimeOut = Duration.ofSeconds(1)
        val key1 = "test-lock1"
        val key1Identifier = UUID.randomUUID().toString()

        val firstLocked: Boolean = redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key1Identifier)
        val isLockedFirst: Boolean = redisLock.isLocked(key1)

        assert(firstLocked){"Was not able to take lock"}
        assert(isLockedFirst)

        Thread.sleep(2100)
        val isLocked: Boolean = redisLock.isLocked(key1)
        assert(isLocked.not()){"Lock is not released"}
    }

    @Test
    fun `will wait to acquire lock if it is not available`() {
        val ttl = Duration.ofSeconds(1)
        val acquireTimeOut = Duration.ofSeconds(2)
        val key1 = "test-lock2"
        val key1Identifier = UUID.randomUUID().toString()
        val key2Identifier = UUID.randomUUID().toString()

        redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key1Identifier)
        //This will wait for 2 seconds to acquire the lock
        val secondLocked: Boolean = redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key2Identifier)
        val isLockedBySecond: Boolean = redisLock.isLocked(key1)

        assert(secondLocked){"Was not able to take lock"}
        assert(isLockedBySecond)
    }

    @Test
    fun `will timeout if time to acquire lock is reached and the lock is not available`() {
        val ttl = Duration.ofSeconds(2)
        val acquireTimeOut = Duration.ofSeconds(1)
        val key1 = "test-lock3"
        val key1Identifier = UUID.randomUUID().toString()
        val key2Identifier = UUID.randomUUID().toString()

        redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key1Identifier)
        val secondLocked: Boolean = redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key2Identifier)

        assert(secondLocked.not()){"Was able to take lock even if it was locked by other process"}
    }

    @Test
    fun `will release lock if there is a lock`() {
        val ttl = Duration.ofSeconds(5)
        val acquireTimeOut = Duration.ofSeconds(1)
        val key1 = "test-lock4"
        val key1Identifier = UUID.randomUUID().toString()

        redisLock.tryLockWithTimeout(key1, ttl, acquireTimeOut, key1Identifier)
        val isLockedBeforeRelease: Boolean = redisLock.isLocked(key1)
        val isLockReleased: Boolean = redisLock.releaseLock(key1, key1Identifier)
        val isLockedAfterRelease: Boolean = redisLock.isLocked(key1)

        assert(isLockReleased)
        assert(isLockedBeforeRelease)
        assert(isLockedAfterRelease.not())

    }
}