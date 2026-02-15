package com.turki.core.database

import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory object for database initialization and connection management.
 *
 * Uses Flyway for versioned schema migrations, then Exposed for ORM access.
 *
 * Migration files live in `resources/db/migration/` and follow the naming
 * convention `V<version>__<description>.sql`.
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    /**
     * Initializes the database: runs Flyway migrations, then connects Exposed.
     *
     * For an **existing** database that was created before Flyway was introduced,
     * Flyway baselines at version 1 so it won't re-run the initial DDL but will
     * pick up V2+ migrations.
     */
    fun init(dbUrl: String? = null, dbUser: String? = null, dbPassword: String? = null) {
        val url = dbUrl ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/turki"
        val user = dbUser ?: System.getenv("DB_USER") ?: "turki"
        val password = dbPassword ?: System.getenv("DB_PASSWORD")
            ?: error("DB_PASSWORD environment variable is required")

        logger.info("Connecting to database: $url")

        // 1. Run Flyway migrations
        runMigrations(url, user, password)

        // 2. Connect Exposed ORM
        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )

        // 3. Safety net: create any columns/tables that migrations might have missed
        //    (useful during local development when you add a column to Tables.kt)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                LessonsTable,
                VocabularyTable,
                HomeworksTable,
                HomeworkQuestionsTable,
                HomeworkSubmissionsTable,
                RemindersTable,
                UserStatesTable,
                UserProgressTable,
                UserDictionaryTable,
                UserCustomDictionaryTable,
                ReviewCardsTable,
                ReminderPreferencesTable,
                AnalyticsEventsTable,
                UserStatsTable,
                SubscriptionPlansTable,
                UserSubscriptionsTable,
                PaymentTransactionsTable,
                MetricsSnapshotsTable,
                ErrorLogsTable
            )
        }
    }

    private fun runMigrations(url: String, user: String, password: String) {
        try {
            val flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                // For existing databases: baseline at V1 so V1 is skipped,
                // but V2+ will be applied.
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()

            val result = flyway.migrate()
            if (result.migrationsExecuted > 0) {
                logger.info(
                    "Flyway: applied {} migration(s), now at version {}",
                    result.migrationsExecuted,
                    result.targetSchemaVersion
                )
            } else {
                logger.info("Flyway: schema is up to date (version {})", result.targetSchemaVersion)
            }
        } catch (e: Exception) {
            logger.error("Flyway migration failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Executes a database query in a coroutine-safe transaction on the IO dispatcher.
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
