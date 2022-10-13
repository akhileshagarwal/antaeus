package io.pleo.antaeus.core.lock

interface LockProvider<T> {
    fun lockPool():T
}