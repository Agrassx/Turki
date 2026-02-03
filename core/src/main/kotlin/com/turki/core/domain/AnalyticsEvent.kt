package com.turki.core.domain

import kotlinx.datetime.Instant

data class AnalyticsEvent(
    val eventName: String,
    val userId: Long,
    val sessionId: String?,
    val props: String,
    val createdAt: Instant
)
