package com.turki.admin.webserver

import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import com.turki.core.domain.Lesson as CoreLesson
import com.turki.core.domain.User as CoreUser
import com.turki.core.domain.VocabularyItem as CoreVocabularyItem
import com.turki.core.repository.LessonRepository
import com.turki.core.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticResources
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AdminServer")

fun main(args: Array<String>) {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/turki"
    val dbUser = System.getenv("DB_USER") ?: "turki"
    val dbPassword = System.getenv("DB_PASSWORD")
        ?: error("DB_PASSWORD environment variable is required")
    val port = System.getenv("ADMIN_PORT")?.toIntOrNull() ?: 8081

    val adminUser = System.getenv("ADMIN_USER") ?: "admin"
    val adminPassword = System.getenv("ADMIN_PASSWORD")
        ?: error("ADMIN_PASSWORD environment variable is required")

    runBlocking {
        DatabaseFactory.init(dbUrl, dbUser, dbPassword)
    }

    startKoin {
        modules(coreModule)
    }

    logger.info("Starting Admin Server on port $port")

    val server = embeddedServer(Netty, port = port) {
        adminModule(adminUser, adminPassword)
    }

    server.start(wait = true)
}

fun Application.adminModule(adminUser: String, adminPassword: String) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }

    install(CORS) {
        anyHost() // For development. In production, specify allowed hosts.
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowCredentials = true
    }

    install(Authentication) {
        basic("admin-auth") {
            realm = "Turki Admin"
            validate { credentials ->
                if (credentials.name == adminUser && credentials.password == adminPassword) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    val userRepository: UserRepository by inject(UserRepository::class.java)
    val lessonRepository: LessonRepository by inject(LessonRepository::class.java)

    routing {
        // Health check - no auth required
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        // Static resources - no auth for JS/CSS
        staticResources("/static", "static")

        // All admin routes require authentication
        authenticate("admin-auth") {
            get("/") {
                call.respondText(
                    contentType = io.ktor.http.ContentType.Text.Html,
                    text = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Turki Admin Panel</title>
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body {
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            }
                            #root {
                                width: 100%;
                                height: 100vh;
                                display: flex;
                            }
                        </style>
                    </head>
                    <body>
                        <div id="root"></div>
                        <script src="/static/admin-web.js"></script>
                    </body>
                    </html>
                """.trimIndent()
                )
            }

            get("/api/users") {
                runBlocking {
                    val users: List<CoreUser> = userRepository.findAll()
                    call.respond(users.map { user ->
                        com.turki.admin.common.domain.User(
                            id = user.id,
                            telegramId = user.telegramId,
                            username = user.username,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            language = when (user.language) {
                                com.turki.core.domain.Language.TURKISH ->
                                    com.turki.admin.common.domain.Language.TURKISH
                                com.turki.core.domain.Language.ENGLISH ->
                                    com.turki.admin.common.domain.Language.ENGLISH
                                com.turki.core.domain.Language.RUSSIAN ->
                                    com.turki.admin.common.domain.Language.RUSSIAN
                            },
                            subscriptionActive = user.subscriptionActive,
                            subscriptionExpiresAt = user.subscriptionExpiresAt,
                            currentLessonId = user.currentLessonId,
                            createdAt = user.createdAt,
                            updatedAt = user.updatedAt
                        )
                    })
                }
            }

            get("/api/lessons") {
                runBlocking {
                    val lessons: List<CoreLesson> = lessonRepository.findAll().map { lesson ->
                        val vocabulary = lessonRepository.getVocabularyItems(lesson.id)
                        lesson.copy(vocabularyItems = vocabulary)
                    }
                    call.respond(lessons.map { lesson ->
                        com.turki.admin.common.domain.Lesson(
                            id = lesson.id,
                            orderIndex = lesson.orderIndex,
                            targetLanguage = when (lesson.targetLanguage) {
                                com.turki.core.domain.Language.TURKISH ->
                                    com.turki.admin.common.domain.Language.TURKISH
                                com.turki.core.domain.Language.ENGLISH ->
                                    com.turki.admin.common.domain.Language.ENGLISH
                                com.turki.core.domain.Language.RUSSIAN ->
                                    com.turki.admin.common.domain.Language.RUSSIAN
                            },
                            title = lesson.title,
                            description = lesson.description,
                            content = lesson.content,
                            vocabularyItems = lesson.vocabularyItems.map { vocab ->
                                com.turki.admin.common.domain.VocabularyItem(
                                    id = vocab.id,
                                    lessonId = vocab.lessonId,
                                    word = vocab.word,
                                    translation = vocab.translation,
                                    pronunciation = vocab.pronunciation,
                                    example = vocab.example
                                )
                            }
                        )
                    })
                }
            }

            post("/api/users/{id}/subscription") {
                val userId = call.parameters["id"]?.toLongOrNull()
                @Serializable
                data class SubscriptionRequest(val active: Boolean)
                val body = call.receive<SubscriptionRequest>()
                val active = body.active

                if (userId != null) {
                    runBlocking {
                        val expiresAt = if (active) Clock.System.now() + 30.days else null
                        userRepository.updateSubscription(userId, active, expiresAt)
                        logger.info("Subscription updated for user $userId: active=$active")
                        call.respond(HttpStatusCode.OK)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/api/users/{id}/reset") {
                val userId = call.parameters["id"]?.toLongOrNull()
                if (userId != null) {
                    runBlocking {
                        userRepository.resetProgress(userId)
                        logger.info("Progress reset for user $userId")
                        call.respond(HttpStatusCode.OK)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/api/users/reset-all") {
                runBlocking {
                    userRepository.resetAllProgress()
                    logger.warn("All users progress has been reset!")
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
