package com.turki.bot.util

import kotlinx.datetime.Clock
import java.util.concurrent.ConcurrentHashMap

class UpdateDeduper(private val ttlMs: Long = 60_000L) {
    private val seen = ConcurrentHashMap<String, Long>()

    fun shouldProcess(key: String): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val previous = seen.putIfAbsent(key, now)
        cleanup(now)
        return previous == null
    }

    private fun cleanup(now: Long) {
        seen.entries.removeIf { now - it.value > ttlMs }
    }
}

class RateLimiter(private val minIntervalMs: Long = 300L) {
    private val lastAction = ConcurrentHashMap<Long, Long>()

    fun allow(userId: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val previous = lastAction.put(userId, now)
        return previous == null || now - previous > minIntervalMs
    }
}
