package com.turki.bot

import com.turki.bot.di.botModule
import com.turki.core.database.DatabaseFactory
import com.turki.core.di.coreModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("TurkiBot")

/**
 * Main entry point for the Telegram bot application.
 *
 * This function initializes the application and handles two modes:
 * 1. **Import mode**: `./gradlew :bot:run --args="import [dataDir]"`
 *    - Imports data from CSV files into the database
 *    - Exits after import completion
 *
 * 2. **Bot mode**: `./gradlew :bot:run`
 *    - Starts the Telegram bot server
 *    - Requires BOT_TOKEN environment variable or .env file
 *    - Initializes dependency injection (Koin)
 *    - Seeds initial data
 *    - Starts Ktor HTTP server with Netty engine
 *
 * **Environment Variables:**
 * - `BOT_TOKEN` - Telegram bot token (required for bot mode)
 * - `DB_URL` - Postgres JDBC URL (optional, default: "jdbc:postgresql://localhost:5432/turki")
 * - `DB_USER` - Postgres user (optional, default: "turki")
 * - `DB_PASSWORD` - Postgres password (required)
 * - `PORT` - HTTP server port (optional, default: 8080)
 *
 * @param args Command line arguments:
 *             - `["import"]` - Run in import mode
 *             - `["import", "dataDir"]` - Import from specific directory
 *             - `[]` - Run in bot mode
 */
fun main(args: Array<String>) {
    EnvLoader.load()

    val dbUrl = EnvLoader.get("DB_URL", "jdbc:postgresql://localhost:5432/turki")
    val dbUser = EnvLoader.get("DB_USER", "turki")
    val dbPassword = EnvLoader.require("DB_PASSWORD")

    if (args.isNotEmpty() && args[0] == "import") {
        DatabaseFactory.init(dbUrl, dbUser, dbPassword)
        val dataDir = args.getOrNull(1) ?: "data"
        ImportData.importAll(dataDir)
        return
    }

    val botToken = EnvLoader.get("BOT_TOKEN")
    if (botToken.isNullOrBlank()) {
        logger.error("""
            BOT_TOKEN not found!
            
            Create .env file in project root:
            
            echo "BOT_TOKEN=your_token_from_BotFather" > .env
            
            Or export the variable:
            
            export BOT_TOKEN=your_token_from_BotFather
            ./gradlew :bot:run
        """.trimIndent())
        return
    }

    DatabaseFactory.init(dbUrl, dbUser, dbPassword)

    startKoin {
        modules(coreModule, botModule)
    }

    val port = EnvLoader.get("PORT", "8080")!!.toInt()

    logger.info("Starting Turki Bot on port $port...")

    embeddedServer(Netty, port = port, module = Application::module)
        .start(wait = true)
}

/**
 * Ktor application module configuration.
 *
 * This function configures the Ktor application by:
 * - Setting up the Telegram bot with command and callback handlers
 * - Configuring HTTP routing
 * - Seeding initial database data (lessons, vocabulary, homework)
 *
 * This is called automatically by Ktor when the server starts.
 */
fun Application.module() {
    configureBot()
    configureRouting()
    runBlocking {
        seedInitialData()
    }
}
