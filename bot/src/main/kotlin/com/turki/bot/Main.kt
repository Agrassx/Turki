package com.turki.bot

import com.turki.bot.di.botModule
import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    // Загружаем .env файл
    EnvLoader.load()

    val dbPath = EnvLoader.get("DB_PATH", "data/turki.db")!!

    // Режим импорта данных
    if (args.isNotEmpty() && args[0] == "import") {
        DatabaseFactory.init(dbPath)
        val dataDir = args.getOrNull(1) ?: "data"
        ImportData.importAll(dataDir)
        return
    }

    // Проверяем наличие токена
    val botToken = EnvLoader.get("BOT_TOKEN")
    if (botToken.isNullOrBlank()) {
        println("""
            ❌ BOT_TOKEN не найден!
            
            Создайте файл .env в корне проекта:
            
            echo "BOT_TOKEN=ваш_токен_от_BotFather" > .env
            
            Или экспортируйте переменную:
            
            export BOT_TOKEN=ваш_токен_от_BotFather
            ./gradlew :bot:run
        """.trimIndent())
        return
    }

    // Обычный запуск бота
    DatabaseFactory.init(dbPath)

    startKoin {
        modules(coreModule, botModule)
    }

    val port = EnvLoader.get("PORT", "8080")!!.toInt()

    println("🚀 Запуск Turki Bot на порту $port...")

    embeddedServer(Netty, port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureBot()
    configureRouting()
    runBlocking {
        seedInitialData()
    }
}
