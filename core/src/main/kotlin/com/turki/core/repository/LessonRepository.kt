package com.turki.core.repository

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.VocabularyItem

interface LessonRepository {
    suspend fun findById(id: Int): Lesson?
    suspend fun findByLanguage(language: Language): List<Lesson>
    suspend fun findAll(): List<Lesson>
    suspend fun findNextLesson(currentLessonId: Int, language: Language): Lesson?
    suspend fun getVocabularyItems(lessonId: Int): List<VocabularyItem>
    suspend fun create(lesson: Lesson): Lesson
    suspend fun update(lesson: Lesson): Lesson
    suspend fun delete(id: Int): Boolean
}
