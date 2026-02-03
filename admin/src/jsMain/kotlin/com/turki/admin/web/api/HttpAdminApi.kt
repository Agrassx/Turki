package com.turki.admin.web.api

import com.turki.admin.common.api.AdminApi
import com.turki.admin.common.domain.Lesson
import com.turki.admin.common.domain.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpAdminApi(private val baseUrl: String = "") : AdminApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
    }

    override suspend fun getUsers(): List<User> {
        return client.get("$baseUrl/api/users").body()
    }

    override suspend fun getLessons(): List<Lesson> {
        return client.get("$baseUrl/api/lessons").body()
    }

    override suspend fun toggleSubscription(userId: Long, active: Boolean) {
        @Serializable
        data class SubscriptionRequest(val active: Boolean)
        client.post("$baseUrl/api/users/$userId/subscription") {
            contentType(ContentType.Application.Json)
            setBody(SubscriptionRequest(active))
        }
    }

    override suspend fun resetUserProgress(userId: Long) {
        client.post("$baseUrl/api/users/$userId/reset")
    }

    override suspend fun resetAllProgress() {
        client.post("$baseUrl/api/users/reset-all")
    }
}
