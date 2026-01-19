package com.turki.core.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun init(dbPath: String = "data/turki.db") {
        val resolvedPath = resolvePath(dbPath)
        val dbFile = File(resolvedPath)
        dbFile.parentFile?.mkdirs()

        val driverClassName = "org.sqlite.JDBC"
        val jdbcUrl = "jdbc:sqlite:$resolvedPath"

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

    private fun resolvePath(path: String): String {
        if (File(path).isAbsolute) return path

        val currentDir = File(System.getProperty("user.dir"))

        // Проверяем текущую директорию
        val inCurrent = File(currentDir, path)
        if (inCurrent.parentFile?.exists() == true) return inCurrent.absolutePath

        // Проверяем родительскую директорию (если запуск из подмодуля)
        val inParent = File(currentDir.parentFile, path)
        if (inParent.parentFile?.exists() == true) return inParent.absolutePath

        // Создаём в текущей директории
        inCurrent.parentFile?.mkdirs()
        return inCurrent.absolutePath
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
