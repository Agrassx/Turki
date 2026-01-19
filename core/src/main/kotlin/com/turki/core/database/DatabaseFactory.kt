package com.turki.core.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * Factory object for database initialization and connection management.
 *
 * This object handles SQLite database setup, schema creation, and provides
 * a coroutine-safe query execution method. It automatically:
 * - Creates the database file if it doesn't exist
 * - Creates all required tables (users, lessons, vocabulary, homework, etc.)
 * - Manages database connections using Exposed ORM
 *
 * **Database Schema:**
 * - UsersTable - User accounts and progress tracking
 * - LessonsTable - Language lessons content
 * - VocabularyTable - Vocabulary items per lesson
 * - HomeworksTable - Homework assignments
 * - HomeworkQuestionsTable - Questions for each homework
 * - HomeworkSubmissionsTable - User submissions and scores
 * - RemindersTable - Scheduled reminders for users
 */
object DatabaseFactory {

    private var dbPath: String? = null

    /**
     * Initializes the database connection and creates all required tables.
     *
     * The database path is determined in the following order:
     * 1. Provided [dbPath] parameter
     * 2. DATABASE_PATH environment variable
     * 3. Auto-detected project root + "/data/turki.db"
     *
     * This function:
     * - Creates the database file and parent directories if needed
     * - Establishes JDBC connection to SQLite
     * - Creates all table schemas using Exposed ORM
     * - Is safe to call multiple times (idempotent)
     *
     * @param dbPath Optional path to the SQLite database file.
     *               If null, uses environment variable or default location.
     *
     * @sample
     * ```
     * DatabaseFactory.init()
     * DatabaseFactory.init("data/custom.db")
     * ```
     */
    fun init(dbPath: String? = null) {
        val path = dbPath ?: System.getenv("DATABASE_PATH") ?: findProjectRoot() + "/data/turki.db"
        this.dbPath = path
        
        val dbFile = File(path)
        dbFile.parentFile?.mkdirs()

        println("📦 База данных: $path")

        val driverClassName = "org.sqlite.JDBC"
        val jdbcUrl = "jdbc:sqlite:$path"

        Database.connect(jdbcUrl, driverClassName)

        transaction {
            SchemaUtils.create(
                UsersTable,
                LessonsTable,
                VocabularyTable,
                HomeworksTable,
                HomeworkQuestionsTable,
                HomeworkSubmissionsTable,
                RemindersTable
            )
        }
    }

    private fun findProjectRoot(): String {
        var dir = File(System.getProperty("user.dir"))
        
        while (dir.parentFile != null) {
            if (File(dir, "settings.gradle.kts").exists()) {
                return dir.absolutePath
            }
            dir = dir.parentFile
        }
        
        return System.getProperty("user.dir")
    }

    /**
     * Executes a database query in a coroutine-safe transaction.
     *
     * This function provides a suspendable way to execute database operations
     * using Exposed ORM. All database queries should be wrapped in this function
     * to ensure proper transaction management and coroutine safety.
     *
     * The transaction runs on the IO dispatcher to avoid blocking the main thread.
     *
     * @param block The suspendable block containing database operations
     * @return The result of the block execution
     *
     * @sample
     * ```
     * val user = DatabaseFactory.dbQuery {
     *     UsersTable.selectAll().where { UsersTable.id eq userId }
     *         .map { toUser(it) }
     *         .singleOrNull()
     * }
     * ```
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
