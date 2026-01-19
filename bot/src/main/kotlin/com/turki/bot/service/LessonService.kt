package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.LessonRepository

class LessonService(private val lessonRepository: LessonRepository) {

    suspend fun getLessonById(id: Int): Lesson? = lessonRepository.findById(id)

    suspend fun getLessonsByLanguage(language: Language): List<Lesson> =
        lessonRepository.findByLanguage(language)

    suspend fun getNextLesson(currentLessonId: Int, language: Language): Lesson? =
        lessonRepository.findNextLesson(currentLessonId, language)

    suspend fun getVocabulary(lessonId: Int): List<VocabularyItem> =
        lessonRepository.getVocabularyItems(lessonId)

    suspend fun getAllLessons(): List<Lesson> = lessonRepository.findAll()

    suspend fun createLesson(lesson: Lesson): Lesson = lessonRepository.create(lesson)
}
