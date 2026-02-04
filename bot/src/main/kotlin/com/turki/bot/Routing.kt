package com.turki.bot

import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.java.KoinJavaComponent.inject

private val userService: UserService by inject(UserService::class.java)
private val lessonService: LessonService by inject(LessonService::class.java)

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        get("/api/stats") {
            val users = userService.getAllUsers()
            val lessons = lessonService.getAllLessons()

            call.respond(
                mapOf(
                    "totalUsers" to users.size,
                    "activeSubscribers" to users.count { it.subscriptionActive },
                    "totalLessons" to lessons.size
                )
            )
        }

        get("/privacy") {
            val html = this::class.java.classLoader
                .getResourceAsStream("static/privacy.html")
                ?.bufferedReader()
                ?.readText()
                ?: "<h1>Privacy Policy not found</h1>"
            call.respondText(html, ContentType.Text.Html)
        }
    }
}
