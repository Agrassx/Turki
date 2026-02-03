package com.turki.bot.service

import com.turki.core.domain.AnalyticsEvent
import com.turki.core.repository.AnalyticsRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository
) {
    private val json = Json { encodeDefaults = true }

    suspend fun log(
        eventName: String,
        userId: Long,
        sessionId: String? = null,
        props: Map<String, String> = emptyMap()
    ) {
        val payload = JsonObject(props.mapValues { JsonPrimitive(it.value) })
        analyticsRepository.create(
            AnalyticsEvent(
                eventName = eventName,
                userId = userId,
                sessionId = sessionId,
                props = json.encodeToString(payload),
                createdAt = Clock.System.now()
            )
        )
    }
}
