package com.turki.admin.web

import com.turki.admin.di.adminModule
import com.turki.admin.viewmodel.LessonsViewModel
import com.turki.admin.viewmodel.UsersViewModel
import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.staticResources
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

fun main(args: Array<String>) {
    val dbPath = System.getenv("DB_PATH") ?: "data/turki.db"
    val port = System.getenv("ADMIN_PORT")?.toIntOrNull() ?: 8081

    runBlocking {
        DatabaseFactory.init(dbPath)
    }

    startKoin {
        modules(coreModule, adminModule)
    }

    val server = embeddedServer(Netty, port = port) {
        adminModule()
    }

    server.start(wait = true)
}

fun Application.adminModule() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }

    val usersViewModel: UsersViewModel by inject(UsersViewModel::class.java)
    val lessonsViewModel: LessonsViewModel by inject(LessonsViewModel::class.java)

    routing {
        staticResources("/static", "static")

        get("/") {
            call.respondHtml {
                head {
                    title { +"Turki Admin Panel" }
                    style {
                        unsafe {
                            raw("""
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #1a1a2e; color: #e8e8e8; }
                                .container { max-width: 1400px; margin: 0 auto; padding: 24px; }
                                .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
                                h1 { font-size: 28px; font-weight: bold; }
                                .nav { display: flex; gap: 12px; margin-bottom: 24px; }
                                .nav-btn { padding: 12px 24px; background: #2980b9; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; }
                                .nav-btn:hover { background: #3498db; }
                                .nav-btn.active { background: #6dd5fa; }
                                .content { background: #16213e; border-radius: 12px; padding: 24px; }
                                .card { background: #16213e; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
                                .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
                                .card-title { font-size: 16px; font-weight: 600; }
                                .card-info { color: #999; font-size: 13px; }
                                .btn { padding: 8px 16px; border: none; border-radius: 6px; cursor: pointer; font-size: 12px; }
                                .btn-danger { background: #e53935; color: white; }
                                .btn-warning { background: #ff9800; color: white; }
                                .btn-primary { background: #2980b9; color: white; }
                                .switch { position: relative; display: inline-block; width: 44px; height: 24px; }
                                .switch input { opacity: 0; width: 0; height: 0; }
                                .slider { position: absolute; cursor: pointer; top: 0; left: 0; right: 0; bottom: 0; background-color: #666; transition: .4s; border-radius: 24px; }
                                .slider:before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background-color: white; transition: .4s; border-radius: 50%; }
                                input:checked + .slider { background-color: #4caf50; }
                                input:checked + .slider:before { transform: translateX(20px); }
                                .stats { color: #999; font-size: 14px; margin-bottom: 24px; }
                            """)
                        }
                    }
                }
                body {
                    div(classes = "container") {
                        div(classes = "header") {
                            h1 { +"🇹🇷 Turki Admin Panel" }
                        }
                        div {
                            attributes["class"] = "nav"
                            button {
                                attributes["class"] = "nav-btn active"
                                attributes["id"] = "btn-users"
                                +"👥 Пользователи"
                            }
                            button {
                                attributes["class"] = "nav-btn"
                                attributes["id"] = "btn-lessons"
                                +"📚 Уроки"
                            }
                        }
                        div {
                            attributes["class"] = "stats"
                            attributes["id"] = "stats"
                        }
                        div {
                            attributes["class"] = "content"
                            attributes["id"] = "content"
                            +"Загрузка..."
                        }
                    }
                    script {
                        unsafe {
                            raw("""
                                let currentView = 'users';
                                
                                async function loadUsers() {
                                    const response = await fetch('/api/users');
                                    const data = await response.json();
                                    renderUsers(data);
                                    updateStats(data);
                                }
                                
                                async function loadLessons() {
                                    const response = await fetch('/api/lessons');
                                    const data = await response.json();
                                    renderLessons(data);
                                }
                                
                                function updateStats(users) {
                                    const total = users.length;
                                    const active = users.filter(u => u.subscriptionActive).length;
                                    document.getElementById('stats').innerHTML = 
                                        'Всего: ' + total + ' | Активных подписок: ' + active;
                                }
                                
                                function renderUsers(users) {
                                    const content = document.getElementById('content');
                                    content.innerHTML = users.map(function(user) {
                                        return '<div class="card">' +
                                            '<div class="card-header">' +
                                                '<div>' +
                                                    '<div class="card-title">' + user.firstName + ' ' + (user.lastName || '') + '</div>' +
                                                    '<div class="card-info">@' + (user.username || 'no username') + ' • ID: ' + user.telegramId + '</div>' +
                                                    '<div class="card-info">Урок: ' + user.currentLessonId + ' • Регистрация: ' + formatDate(user.createdAt) + '</div>' +
                                                '</div>' +
                                                '<div style="display: flex; gap: 8px; align-items: center;">' +
                                                    '<button class="btn btn-warning" onclick="resetProgress(' + user.id + ')">↩ Сброс</button>' +
                                                    '<div>' +
                                                        '<div style="color: ' + (user.subscriptionActive ? '#4caf50' : '#999') + '; font-size: 13px;">' +
                                                            (user.subscriptionActive ? 'Активна' : 'Неактивна') +
                                                        '</div>' +
                                                        '<label class="switch">' +
                                                            '<input type="checkbox" ' + (user.subscriptionActive ? 'checked' : '') + 
                                                            ' onchange="toggleSubscription(' + user.id + ', this.checked)">' +
                                                            '<span class="slider"></span>' +
                                                        '</label>' +
                                                    '</div>' +
                                                '</div>' +
                                            '</div>' +
                                        '</div>';
                                    }).join('');
                                }
                                
                                function renderLessons(lessons) {
                                    const content = document.getElementById('content');
                                    content.innerHTML = lessons.map(function(lesson) {
                                        return '<div class="card">' +
                                            '<div class="card-header">' +
                                                '<div>' +
                                                    '<div class="card-title">Урок ' + lesson.orderIndex + ': ' + lesson.title + '</div>' +
                                                    '<div class="card-info">Язык: ' + lesson.targetLanguage + ' • ID: ' + lesson.id + '</div>' +
                                                    '<div class="card-info">' + lesson.description.substring(0, 150) + (lesson.description.length > 150 ? '...' : '') + '</div>' +
                                                    '<div class="card-info">📖 ' + (lesson.vocabularyItems ? lesson.vocabularyItems.length : 0) + ' слов</div>' +
                                                '</div>' +
                                            '</div>' +
                                        '</div>';
                                    }).join('');
                                }
                                
                                function formatDate(timestamp) {
                                    const date = new Date(timestamp);
                                    return date.toLocaleDateString('ru-RU');
                                }
                                
                                async function toggleSubscription(userId, active) {
                                    await fetch('/api/users/' + userId + '/subscription', {
                                        method: 'POST',
                                        headers: {'Content-Type': 'application/json'},
                                        body: JSON.stringify({active: active})
                                    });
                                    loadUsers();
                                }
                                
                                async function resetProgress(userId) {
                                    if (confirm('Сбросить прогресс пользователя?')) {
                                        await fetch('/api/users/' + userId + '/reset', {method: 'POST'});
                                        loadUsers();
                                    }
                                }
                                
                                document.getElementById('btn-users').onclick = function() {
                                    currentView = 'users';
                                    document.getElementById('btn-users').classList.add('active');
                                    document.getElementById('btn-lessons').classList.remove('active');
                                    loadUsers();
                                };
                                
                                document.getElementById('btn-lessons').onclick = function() {
                                    currentView = 'lessons';
                                    document.getElementById('btn-lessons').classList.add('active');
                                    document.getElementById('btn-users').classList.remove('active');
                                    loadLessons();
                                };
                                
                                loadUsers();
                            """)
                        }
                    }
                }
            }
        }

        get("/api/users") {
            runBlocking {
                usersViewModel.loadUsers()
                kotlinx.coroutines.delay(100)
                val state = usersViewModel.state.value
                call.respond(state.users.map { user ->
                    mapOf(
                        "id" to user.id,
                        "telegramId" to user.telegramId,
                        "username" to (user.username ?: ""),
                        "firstName" to user.firstName,
                        "lastName" to (user.lastName ?: ""),
                        "currentLessonId" to user.currentLessonId,
                        "subscriptionActive" to user.subscriptionActive,
                        "createdAt" to user.createdAt.toString()
                    )
                })
            }
        }

        get("/api/lessons") {
            runBlocking {
                lessonsViewModel.loadLessons()
                kotlinx.coroutines.delay(100)
                val state = lessonsViewModel.state.value
                call.respond(state.lessons.map { lesson ->
                    mapOf(
                        "id" to lesson.id,
                        "orderIndex" to lesson.orderIndex,
                        "title" to lesson.title,
                        "targetLanguage" to lesson.targetLanguage.displayName,
                        "description" to lesson.description,
                        "vocabularyItems" to lesson.vocabularyItems.map { vocab ->
                            mapOf(
                                "word" to vocab.word,
                                "translation" to vocab.translation
                            )
                        }
                    )
                })
            }
        }

        post("/api/users/{id}/subscription") {
            val userId = call.parameters["id"]?.toLongOrNull()
            @kotlinx.serialization.Serializable
            data class SubscriptionRequest(val active: Boolean)
            val body = call.receive<SubscriptionRequest>()
            val active = body.active

            if (userId != null) {
                runBlocking {
                    usersViewModel.loadUsers()
                    delay(100)
                    val user = usersViewModel.state.value.users.find { it.id == userId }
                    if (user != null) {
                        usersViewModel.toggleSubscription(user)
                    }
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
                    usersViewModel.loadUsers()
                    delay(100)
                    val user = usersViewModel.state.value.users.find { it.id == userId }
                    if (user != null) {
                        usersViewModel.resetUserProgress(user)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
    }
}

