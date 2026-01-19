package com.turki.bot

import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
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
    }
}
