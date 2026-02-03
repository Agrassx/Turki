package com.turki.core.service

import com.turki.core.database.DatabaseFactory
import com.turki.core.database.HomeworkQuestionsTable
import com.turki.core.database.HomeworksTable
import com.turki.core.database.LessonsTable
import com.turki.core.database.VocabularyTable
import com.turki.core.domain.QuestionType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

/**
 * Service for importing lessons, vocabulary and homework data from CSV content.
 * Can be used via API endpoints or CLI.
 */
class DataImportService {
    private val logger = LoggerFactory.getLogger(DataImportService::class.java)

    data class ImportResult(
        val success: Boolean,
        val imported: Int,
        val updated: Int,
        val errors: List<String>
    )

    /**
     * Import lessons from CSV content.
     * CSV format: order_index,target_language,title,description,content[,level,content_version,is_active]
     */
    suspend fun importLessons(csvContent: String, clearExisting: Boolean = false): ImportResult {
        val records = parseCsv(csvContent).drop(1) // Skip header
        val errors = mutableListOf<String>()
        var imported = 0
        var updated = 0

        DatabaseFactory.dbQuery {
            if (clearExisting) {
                VocabularyTable.deleteAll()
                HomeworkQuestionsTable.deleteAll()
                HomeworksTable.deleteAll()
                LessonsTable.deleteAll()
                logger.info("Cleared existing lessons data")
            }

            for ((lineNum, fields) in records.withIndex()) {
                try {
                    if (fields.size < 5) {
                        errors.add("Line ${lineNum + 2}: insufficient fields (need 5, got ${fields.size})")
                        continue
                    }

                    val orderIndex = fields[0].toIntOrNull()
                    if (orderIndex == null) {
                        errors.add("Line ${lineNum + 2}: invalid order_index")
                        continue
                    }

                    val targetLanguage = fields[1]
                    val title = fields[2]
                    val description = fields[3]
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

                    if (existing != null) {
                        LessonsTable.update({
                            (LessonsTable.orderIndex eq orderIndex) and
                                (LessonsTable.targetLanguage eq targetLanguage)
                        }) {
                            it[LessonsTable.title] = title
                            it[LessonsTable.description] = description
                            it[LessonsTable.content] = content
                            it[LessonsTable.level] = level
                            it[LessonsTable.contentVersion] = contentVersion
                            it[LessonsTable.isActive] = isActive
                        }
                        updated++
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
                        }
                    }
                    imported++
                } catch (e: Exception) {
                    errors.add("Line ${lineNum + 2}: ${e.message}")
                }
            }
        }

        logger.info("Lessons import: $imported total ($updated updated, ${imported - updated} created)")
        return ImportResult(errors.isEmpty(), imported, updated, errors)
    }

    /**
     * Import vocabulary from CSV content.
     * CSV format: lesson_order_index,word,translation[,pronunciation,example]
     */
    suspend fun importVocabulary(csvContent: String, clearExisting: Boolean = false): ImportResult {
        val records = parseCsv(csvContent).drop(1)
        val errors = mutableListOf<String>()
        var imported = 0

        DatabaseFactory.dbQuery {
            if (clearExisting) {
                VocabularyTable.deleteAll()
                logger.info("Cleared existing vocabulary data")
            }

            // Build lesson order -> id map
            val lessonIdMap = LessonsTable.selectAll()
                .associate { it[LessonsTable.orderIndex] to it[LessonsTable.id].value }

            for ((lineNum, fields) in records.withIndex()) {
                try {
                    if (fields.size < 3) {
                        errors.add("Line ${lineNum + 2}: insufficient fields (need 3, got ${fields.size})")
                        continue
                    }

                    val lessonOrderIndex = fields[0].toIntOrNull()
                    if (lessonOrderIndex == null) {
                        errors.add("Line ${lineNum + 2}: invalid lesson_order_index")
                        continue
                    }

                    val lessonId = lessonIdMap[lessonOrderIndex]
                    if (lessonId == null) {
                        errors.add("Line ${lineNum + 2}: lesson $lessonOrderIndex not found")
                        continue
                    }

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
                    errors.add("Line ${lineNum + 2}: ${e.message}")
                }
            }
        }

        logger.info("Vocabulary import: $imported words imported")
        return ImportResult(errors.isEmpty(), imported, 0, errors)
    }

    /**
     * Import homework from CSV content.
     * CSV format: lesson_order_index,question_type,question_text,options,correct_answer
     */
    suspend fun importHomework(csvContent: String, clearExisting: Boolean = false): ImportResult {
        val records = parseCsv(csvContent).drop(1)
        val errors = mutableListOf<String>()
        var imported = 0

        DatabaseFactory.dbQuery {
            if (clearExisting) {
                HomeworkQuestionsTable.deleteAll()
                HomeworksTable.deleteAll()
                logger.info("Cleared existing homework data")
            }

            // Build lesson order -> id map
            val lessonIdMap = LessonsTable.selectAll()
                .associate { it[LessonsTable.orderIndex] to it[LessonsTable.id].value }

            // Create homework entries for each lesson if not exist
            val homeworkIdMap = mutableMapOf<Int, Int>()
            for ((orderIndex, lessonId) in lessonIdMap) {
                val existing = HomeworksTable.selectAll()
                    .where { HomeworksTable.lessonId eq lessonId }
                    .firstOrNull()

                if (existing != null) {
                    homeworkIdMap[orderIndex] = existing[HomeworksTable.id].value
                } else {
                    val hwId = HomeworksTable.insert {
                        it[HomeworksTable.lessonId] = lessonId
                    } get HomeworksTable.id
                    homeworkIdMap[orderIndex] = hwId.value
                }
            }

            for ((lineNum, fields) in records.withIndex()) {
                try {
                    if (fields.size < 5) {
                        errors.add("Line ${lineNum + 2}: insufficient fields (need 5, got ${fields.size})")
                        continue
                    }

                    val lessonOrderIndex = fields[0].toIntOrNull()
                    if (lessonOrderIndex == null) {
                        errors.add("Line ${lineNum + 2}: invalid lesson_order_index")
                        continue
                    }

                    val homeworkId = homeworkIdMap[lessonOrderIndex]
                    if (homeworkId == null) {
                        errors.add("Line ${lineNum + 2}: homework for lesson $lessonOrderIndex not found")
                        continue
                    }

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
                    imported++
                } catch (e: Exception) {
                    errors.add("Line ${lineNum + 2}: ${e.message}")
                }
            }
        }

        logger.info("Homework import: $imported questions imported")
        return ImportResult(errors.isEmpty(), imported, 0, errors)
    }

    private fun parseCsv(content: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < content.length) {
            val char = content[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
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
                    if (char == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
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
