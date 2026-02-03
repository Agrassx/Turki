package com.turki.bot.service

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.LessonRepository

/**
 * Service for managing lessons and vocabulary.
 *
 * This service provides operations for retrieving lessons, vocabulary items,
 * and managing lesson progression. All operations are coroutine-based.
 */
class LessonService(private val lessonRepository: LessonRepository) {

    /**
     * Retrieves a lesson by its database ID.
     *
     * @param id The lesson's database ID
     * @return The [Lesson] if found, null otherwise
     */
    suspend fun getLessonById(id: Int): Lesson? = lessonRepository.findById(id)

    /**
     * Retrieves all lessons for a specific language, ordered by index.
     *
     * @param language The target language to filter lessons
     * @return List of [Lesson] objects sorted by orderIndex
     */
    suspend fun getLessonsByLanguage(language: Language, level: String? = "A1"): List<Lesson> =
        lessonRepository.findByLanguage(language, level)

    /**
     * Finds the next lesson after the current one for a given language.
     *
     * @param currentLessonId The ID of the current lesson
     * @param language The target language
     * @return The next [Lesson] if available, null if no more lessons
     */
    suspend fun getNextLesson(currentLessonId: Int, language: Language, level: String? = "A1"): Lesson? =
        lessonRepository.findNextLesson(currentLessonId, language, level)

    /**
     * Retrieves all vocabulary items for a specific lesson.
     *
     * @param lessonId The lesson's database ID
     * @return List of [VocabularyItem] objects for the lesson
     */
    suspend fun getVocabulary(lessonId: Int): List<VocabularyItem> =
        lessonRepository.getVocabularyItems(lessonId)

    /**
     * Searches vocabulary by word or translation.
     */
    suspend fun searchVocabulary(query: String, limit: Int = 5): List<VocabularyItem> =
        lessonRepository.searchVocabulary(query, limit)

    /**
     * Retrieves a vocabulary item by ID.
     */
    suspend fun findVocabularyById(id: Int): VocabularyItem? =
        lessonRepository.findVocabularyById(id)

    /**
     * Retrieves all lessons in the system, ordered by index.
     *
     * @return List of all [Lesson] objects
     */
    suspend fun getAllLessons(): List<Lesson> = lessonRepository.findAll()

    /**
     * Creates a new lesson in the database.
     *
     * @param lesson The lesson object to create
     * @return The created [Lesson] with assigned database ID
     */
    suspend fun createLesson(lesson: Lesson): Lesson = lessonRepository.create(lesson)
}
