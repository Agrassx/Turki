package com.turki.bot

import com.turki.core.database.DatabaseFactory
import com.turki.core.database.HomeworkQuestionsTable
import com.turki.core.database.HomeworksTable
import com.turki.core.database.LessonsTable
import com.turki.core.database.VocabularyTable
import com.turki.core.domain.QuestionType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File

/**
 * Data import utility for loading CSV files into the database.
 *
 * This object provides functionality to import lessons, vocabulary, and homework
 * data from CSV files located in the specified data directory. The import process:
 * - Parses CSV files with proper quote handling
 * - Converts markdown formatting to HTML for lesson content
 * - Updates existing records or creates new ones
 * - Maintains referential integrity between lessons, vocabulary, and homework
 *
 * **CSV File Format:**
 *
 * - **lessons.csv**: `order_index,target_language,title,description,content`
 * - **vocabulary.csv**: `lesson_order_index,word,translation,pronunciation,example`
 * - **homework.csv**: `lesson_order_index,question_type,question_text,options,correct_answer`
 *
 * **Usage:**
 * ```
 * ./gradlew :bot:run --args="import"
 * ./gradlew :bot:run --args="import /path/to/data"
 * ```
 */
object ImportData {

    /**
     * Imports all data from CSV files in the specified directory.
     *
     * This function processes three CSV files in order:
     * 1. lessons.csv - Creates or updates lesson records
     * 2. vocabulary.csv - Imports vocabulary items linked to lessons
     * 3. homework.csv - Imports homework questions linked to lessons
     *
     * The function automatically:
     * - Initializes the database connection
     * - Converts markdown to HTML in lesson content
     * - Updates existing lessons instead of creating duplicates
     * - Handles errors gracefully and continues processing
     *
     * @param dataDir The directory containing CSV files (default: "data")
     *                Can be absolute or relative path
     *
     * @sample
     * ```
     * ImportData.importAll("data")
     * ImportData.importAll("/absolute/path/to/data")
     * ```
     */
    fun importAll(dataDir: String = "data") {
        println("🚀 Начинаем импорт данных...")

        val baseDir = findDataDir(dataDir)
        println("📁 Директория данных: ${baseDir.absolutePath}")

        val lessonsFile = File(baseDir, "lessons.csv")
        val vocabularyFile = File(baseDir, "vocabulary.csv")
        val homeworkFile = File(baseDir, "homework.csv")

        if (!lessonsFile.exists()) {
            println("❌ Файл lessons.csv не найден в $dataDir/")
            return
        }

        runBlocking {
            DatabaseFactory.init()
        }

        val lessonIdMap = mutableMapOf<Int, Int>()

        if (lessonsFile.exists()) {
            println("📚 Импорт уроков...")
            importLessons(lessonsFile, lessonIdMap)
        }

        if (vocabularyFile.exists()) {
            println("📖 Импорт словаря...")
            importVocabulary(vocabularyFile, lessonIdMap)
        }

        if (homeworkFile.exists()) {
            println("📝 Импорт домашних заданий...")
            importHomework(homeworkFile, lessonIdMap)
        }

        println("✅ Импорт завершён!")
    }

    private fun importLessons(file: File, lessonIdMap: MutableMap<Int, Int>) {
        val records = readCsvRecords(file).drop(1)
        var imported = 0
        var updated = 0

        transaction {
            for (fields in records) {
                try {
                    if (fields.size < 5) continue

                    val orderIndex = fields[0].toIntOrNull() ?: continue
                    val targetLanguage = fields[1]
                    val title = fields[2]
                    val description = fields[3] // Store raw markdown, convert at display time
                    val content = fields[4]
                    val level = fields.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "A1"
                    val contentVersion = fields.getOrNull(6)?.takeIf { it.isNotBlank() } ?: "v1"
                    val isActive = fields.getOrNull(7)?.toBooleanStrictOrNull() ?: true

                    val existing = LessonsTable.selectAll()
                        .where {
                            (LessonsTable.orderIndex eq orderIndex) and
                            (LessonsTable.targetLanguage eq targetLanguage)
                        }
                        .singleOrNull()

                    val id = if (existing != null) {
                        val existingId = existing[LessonsTable.id].value
                        LessonsTable.update(
                            where = {
                                (LessonsTable.orderIndex eq orderIndex) and
                                (LessonsTable.targetLanguage eq targetLanguage)
                            }
                        ) {
                            it[LessonsTable.title] = title
                            it[LessonsTable.description] = description
                            it[LessonsTable.content] = content
                            it[LessonsTable.level] = level
                            it[LessonsTable.contentVersion] = contentVersion
                            it[LessonsTable.isActive] = isActive
                        }
                        updated++
                        existingId
                    } else {
                        LessonsTable.insert {
                            it[LessonsTable.orderIndex] = orderIndex
                            it[LessonsTable.targetLanguage] = targetLanguage
                            it[LessonsTable.title] = title
                            it[LessonsTable.description] = description
                            it[LessonsTable.content] = content
                            it[LessonsTable.level] = level
                            it[LessonsTable.contentVersion] = contentVersion
                            it[LessonsTable.isActive] = isActive
                        }[LessonsTable.id].value
                    }

                    lessonIdMap[orderIndex] = id
                    imported++
                } catch (e: Exception) {
                    println("  ⚠️ Ошибка в строке: ${e.message}")
                }
            }
        }

        println("  ✓ Импортировано уроков: $imported (обновлено: $updated, создано: ${imported - updated})")
    }

    private fun importVocabulary(file: File, lessonIdMap: Map<Int, Int>) {
        val records = readCsvRecords(file).drop(1)
        var imported = 0

        transaction {
            for (fields in records) {
                try {
                    if (fields.size < 3) continue

                    val lessonOrderIndex = fields[0].toIntOrNull() ?: continue
                    val lessonId = lessonIdMap[lessonOrderIndex] ?: continue
                    val word = fields[1]
                    val translation = fields[2]
                    val pronunciation = fields.getOrNull(3)?.takeIf { it.isNotBlank() }
                    val example = fields.getOrNull(4)?.takeIf { it.isNotBlank() }

                    VocabularyTable.insert {
                        it[VocabularyTable.lessonId] = lessonId
                        it[VocabularyTable.word] = word
                        it[VocabularyTable.translation] = translation
                        it[VocabularyTable.pronunciation] = pronunciation
                        it[VocabularyTable.example] = example
                    }
                    imported++
                } catch (e: Exception) {
                    println("  ⚠️ Ошибка в строке: ${e.message}")
                }
            }
        }

        println("  ✓ Импортировано слов: $imported")
    }

    private fun importHomework(file: File, lessonIdMap: Map<Int, Int>) {
        val records = readCsvRecords(file).drop(1)
        var importedQuestions = 0
        val homeworkIdMap = mutableMapOf<Int, Int>()

        transaction {
            for ((orderIndex, lessonId) in lessonIdMap) {
                val existingHomework = HomeworksTable.selectAll()
                    .where { HomeworksTable.lessonId eq lessonId }
                    .firstOrNull()

                if (existingHomework == null) {
                    val hwId = HomeworksTable.insert {
                        it[HomeworksTable.lessonId] = lessonId
                    } get HomeworksTable.id
                    homeworkIdMap[orderIndex] = hwId.value
                }
            }

            for (fields in records) {
                try {
                    if (fields.size < 5) continue

                    val lessonOrderIndex = fields[0].toIntOrNull() ?: continue
                    val homeworkId = homeworkIdMap[lessonOrderIndex] ?: continue
                    val questionType = fields[1]
                    val questionText = fields[2]
                    val options = fields[3]
                    val correctAnswer = fields[4]

                    val qType = when (questionType.uppercase()) {
                        "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE
                        "TRANSLATION" -> QuestionType.TRANSLATION
                        "TEXT_INPUT" -> QuestionType.TEXT_INPUT
                        else -> QuestionType.MULTIPLE_CHOICE
                    }

                    val optionsList = if (options.isNotBlank()) {
                        options.split("|").map { it.trim() }
                    } else {
                        emptyList()
                    }

                    HomeworkQuestionsTable.insert {
                        it[HomeworkQuestionsTable.homeworkId] = homeworkId
                        it[HomeworkQuestionsTable.questionType] = qType.name
                        it[HomeworkQuestionsTable.questionText] = questionText
                        it[HomeworkQuestionsTable.options] = optionsList.joinToString("|")
                        it[HomeworkQuestionsTable.correctAnswer] = correctAnswer
                    }
                    importedQuestions++
                } catch (e: Exception) {
                    println("  ⚠️ Ошибка в строке: ${e.message}")
                }
            }
        }

        println("  ✓ Импортировано вопросов: $importedQuestions")
    }

    /**
     * Finds the data directory by checking multiple possible locations.
     *
     * Searches for the data directory in the following order:
     * 1. Current working directory
     * 2. Parent directory
     * 3. Relative path "../data"
     * 4. User home directory (IdeaProjects/Turki/data)
     *
     * @param dataDir The data directory name or path to search for
     * @return The found [File] directory, or the original path if not found
     */
    private fun findDataDir(dataDir: String): File {
        if (File(dataDir).isAbsolute) {
            return File(dataDir)
        }

        val currentDir = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(currentDir, dataDir),
            File(currentDir.parentFile, dataDir),
            File(currentDir, "../$dataDir"),
            File(System.getProperty("user.home"), "IdeaProjects/Turki/$dataDir")
        )

        return candidates.firstOrNull { it.exists() && File(it, "lessons.csv").exists() }
            ?: File(dataDir)
    }

    private fun readCsvRecords(file: File): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        val text = file.readText()
        var i = 0
        while (i < text.length) {
            val char = text[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    currentRecord.add(currentField.toString())
                    currentField.clear()
                    if (currentRecord.isNotEmpty()) {
                        records.add(currentRecord.toList())
                        currentRecord.clear()
                    }
                    if (char == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                }
                else -> currentField.append(char)
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            records.add(currentRecord.toList())
        }

        return records
    }
}
