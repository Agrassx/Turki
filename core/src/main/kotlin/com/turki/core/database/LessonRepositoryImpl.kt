package com.turki.core.database

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.LessonRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.or

class LessonRepositoryImpl : LessonRepository {

    override suspend fun findById(id: Int): Lesson? = DatabaseFactory.dbQuery {
        val lesson = LessonsTable.selectAll().where { LessonsTable.id eq id }
            .map(::toLesson)
            .singleOrNull() ?: return@dbQuery null

        val vocabulary = getVocabularyItems(id)
        lesson.copy(vocabularyItems = vocabulary)
    }

    override suspend fun findByLanguage(language: Language, level: String?): List<Lesson> = DatabaseFactory.dbQuery {
        LessonsTable.selectAll()
            .where {
                val base = (LessonsTable.targetLanguage eq language.code) and (LessonsTable.isActive eq true)
                if (level == null) base else base and (LessonsTable.level eq level)
            }
            .orderBy(LessonsTable.orderIndex)
            .map(::toLesson)
    }

    override suspend fun findAll(): List<Lesson> = DatabaseFactory.dbQuery {
        LessonsTable.selectAll()
            .orderBy(LessonsTable.orderIndex)
            .map(::toLesson)
    }

    override suspend fun findNextLesson(currentLessonId: Int, language: Language, level: String?): Lesson? =
        DatabaseFactory.dbQuery {
        val current = LessonsTable.selectAll().where { LessonsTable.id eq currentLessonId }
            .singleOrNull() ?: return@dbQuery null

        val currentOrder = current[LessonsTable.orderIndex]

        LessonsTable.selectAll()
            .where {
                val base = (LessonsTable.targetLanguage eq language.code) and
                    (LessonsTable.orderIndex greater currentOrder) and
                    (LessonsTable.isActive eq true)
                if (level == null) base else base and (LessonsTable.level eq level)
            }
            .orderBy(LessonsTable.orderIndex)
            .limit(1)
            .map(::toLesson)
            .singleOrNull()
        }

    override suspend fun getVocabularyItems(lessonId: Int): List<VocabularyItem> = DatabaseFactory.dbQuery {
        VocabularyTable.selectAll().where { VocabularyTable.lessonId eq lessonId }
            .map(::toVocabulary)
    }

    override suspend fun searchVocabulary(query: String, limit: Int): List<VocabularyItem> = DatabaseFactory.dbQuery {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@dbQuery emptyList()
        }

        VocabularyTable.selectAll()
            .where {
                (VocabularyTable.word like "%$trimmed%") or
                    (VocabularyTable.translation like "%$trimmed%")
            }
            .limit(limit)
            .map(::toVocabulary)
    }

    override suspend fun findVocabularyById(id: Int): VocabularyItem? = DatabaseFactory.dbQuery {
        VocabularyTable.selectAll()
            .where { VocabularyTable.id eq id }
            .map(::toVocabulary)
            .singleOrNull()
    }

    override suspend fun findVocabularyByIds(ids: List<Int>): List<VocabularyItem> = DatabaseFactory.dbQuery {
        if (ids.isEmpty()) return@dbQuery emptyList()
        VocabularyTable.selectAll()
            .where { VocabularyTable.id inList ids }
            .map(::toVocabulary)
    }

    override suspend fun create(lesson: Lesson): Lesson = DatabaseFactory.dbQuery {
        val id = LessonsTable.insert {
            it[orderIndex] = lesson.orderIndex
            it[targetLanguage] = lesson.targetLanguage.code
            it[title] = lesson.title
            it[description] = lesson.description
            it[content] = lesson.content
            it[LessonsTable.level] = lesson.level
            it[LessonsTable.contentVersion] = lesson.contentVersion
            it[LessonsTable.isActive] = lesson.isActive
        }[LessonsTable.id].value

        lesson.vocabularyItems.forEach { vocab ->
            VocabularyTable.insert {
                it[lessonId] = id
                it[word] = vocab.word
                it[translation] = vocab.translation
                it[pronunciation] = vocab.pronunciation
                it[example] = vocab.example
            }
        }

        lesson.copy(id = id)
    }

    override suspend fun update(lesson: Lesson): Lesson = DatabaseFactory.dbQuery {
        LessonsTable.update({ LessonsTable.id eq lesson.id }) {
            it[orderIndex] = lesson.orderIndex
            it[targetLanguage] = lesson.targetLanguage.code
            it[title] = lesson.title
            it[description] = lesson.description
            it[content] = lesson.content
            it[level] = lesson.level
            it[contentVersion] = lesson.contentVersion
            it[isActive] = lesson.isActive
        }
        lesson
    }

    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        VocabularyTable.deleteWhere { lessonId eq id }
        LessonsTable.deleteWhere { LessonsTable.id eq id } > 0
    }

    private fun toLesson(row: ResultRow): Lesson = Lesson(
        id = row[LessonsTable.id].value,
        orderIndex = row[LessonsTable.orderIndex],
        targetLanguage = Language.fromCode(row[LessonsTable.targetLanguage]) ?: Language.TURKISH,
        title = row[LessonsTable.title],
        description = row[LessonsTable.description],
        content = row[LessonsTable.content],
        level = row[LessonsTable.level],
        contentVersion = row[LessonsTable.contentVersion],
        isActive = row[LessonsTable.isActive]
    )

    private fun toVocabulary(row: ResultRow): VocabularyItem = VocabularyItem(
        id = row[VocabularyTable.id].value,
        lessonId = row[VocabularyTable.lessonId].value,
        word = row[VocabularyTable.word],
        translation = row[VocabularyTable.translation],
        pronunciation = row[VocabularyTable.pronunciation],
        example = row[VocabularyTable.example]
    )
}
