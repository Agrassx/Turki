package com.turki.bot

import com.turki.core.database.DatabaseFactory
import com.turki.core.database.HomeworkQuestionsTable
import com.turki.core.database.HomeworksTable
import com.turki.core.database.LessonsTable
import com.turki.core.database.VocabularyTable
import com.turki.core.domain.QuestionType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * Скрипт для импорта данных из CSV файлов в базу данных.
 *
 * CSV файлы должны находиться в папке data/:
 * - data/lessons.csv
 * - data/vocabulary.csv
 * - data/homework.csv
 *
 * Запуск: ./gradlew :bot:run --args="import"
 */
object ImportData {

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

        val lessonIdMap = mutableMapOf<Int, Int>() // orderIndex -> database id

        // Import lessons
        if (lessonsFile.exists()) {
            println("📚 Импорт уроков...")
            importLessons(lessonsFile, lessonIdMap)
        }

        // Import vocabulary
        if (vocabularyFile.exists()) {
            println("📖 Импорт словаря...")
            importVocabulary(vocabularyFile, lessonIdMap)
        }

        // Import homework
        if (homeworkFile.exists()) {
            println("📝 Импорт домашних заданий...")
            importHomework(homeworkFile, lessonIdMap)
        }

        println("✅ Импорт завершён!")
    }

    private fun importLessons(file: File, lessonIdMap: MutableMap<Int, Int>) {
        val lines = file.readLines().drop(1) // skip header
        var imported = 0

        transaction {
            for (line in lines) {
                try {
                    val fields = parseCsvLine(line)
                    if (fields.size < 5) continue

                    val orderIndex = fields[0].toIntOrNull() ?: continue
                    val targetLanguage = fields[1]
                    val title = fields[2]
                    val description = fields[3]
                    val content = fields[4]

                    val id = LessonsTable.insert {
                        it[LessonsTable.orderIndex] = orderIndex
                        it[LessonsTable.targetLanguage] = targetLanguage
                        it[LessonsTable.title] = title
                        it[LessonsTable.description] = description
                        it[LessonsTable.content] = content
                    } get LessonsTable.id

                    lessonIdMap[orderIndex] = id.value
                    imported++
                } catch (e: Exception) {
                    println("  ⚠️ Ошибка в строке: ${e.message}")
                }
            }
        }

        println("  ✓ Импортировано уроков: $imported")
    }

    private fun importVocabulary(file: File, lessonIdMap: Map<Int, Int>) {
        val lines = file.readLines().drop(1)
        var imported = 0

        transaction {
            for (line in lines) {
                try {
                    val fields = parseCsvLine(line)
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
        val lines = file.readLines().drop(1)
        var importedQuestions = 0
        val homeworkIdMap = mutableMapOf<Int, Int>() // lessonId -> homeworkId

        transaction {
            // Create homework entries for each lesson
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

            for (line in lines) {
                try {
                    val fields = parseCsvLine(line)
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

    private fun findDataDir(dataDir: String): File {
        if (File(dataDir).isAbsolute) return File(dataDir)

        val currentDir = File(System.getProperty("user.dir"))

        // Проверяем разные варианты расположения
        val candidates = listOf(
            File(currentDir, dataDir),
            File(currentDir.parentFile, dataDir),
            File(currentDir, "../$dataDir"),
            File(System.getProperty("user.home"), "IdeaProjects/Turki/$dataDir")
        )

        return candidates.firstOrNull { it.exists() && File(it, "lessons.csv").exists() }
            ?: File(dataDir)
    }

    /**
     * Парсинг CSV строки с учётом кавычек
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]

            when {
                c == '"' && !inQuotes -> {
                    inQuotes = true
                }
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }

        result.add(current.toString())
        return result
    }
}

