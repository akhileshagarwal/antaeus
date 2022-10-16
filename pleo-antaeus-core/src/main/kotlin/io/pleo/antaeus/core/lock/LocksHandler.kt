package io.pleo.antaeus.core.lock

import java.time.Duration

interface LocksHandler {
    fun tryLockWithTimeout(lockKey: String, lockDuration: Duration, acquireTimeout: Duration, lockIdentifier: String?): Boolean
    fun releaseLock(lockKey: String, identifier: String): Boolean
    fun isLocked(lockKey: String): Boolean
}