package com.turki.bot.testutil

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TestClock(private var instant: Instant) : Clock {
    override fun now(): Instant = instant

    fun set(instant: Instant) {
        this.instant = instant
    }
}
