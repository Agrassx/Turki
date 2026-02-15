package com.turki.core.repository

import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import com.turki.core.domain.VocabularyItem

/**
 * Repository interface for lesson and vocabulary data access.
 *
 * This interface defines operations for managing lessons and their associated
 * vocabulary items. Implementations should provide database persistence
 * using Exposed ORM or similar frameworks.
 *
 * All operations are coroutine-based and should be executed within database transactions.
 */
interface LessonRepository {
    /**
     * Finds a lesson by its database ID.
     *
     * @param id The lesson's database ID
     * @return The [Lesson] if found, null otherwise
     */
    suspend fun findById(id: Int): Lesson?

    /**
     * Finds all lessons for a specific language, ordered by index.
     *
     * @param language The target language to filter lessons
     * @return List of [Lesson] objects sorted by orderIndex
     */
    suspend fun findByLanguage(language: Language, level: String? = null): List<Lesson>

    /**
     * Retrieves all lessons in the system, ordered by index.
     *
     * @return List of all [Lesson] objects
     */
    suspend fun findAll(): List<Lesson>

    /**
     * Finds the next lesson after the current one for a given language.
     *
     * @param currentLessonId The ID of the current lesson
     * @param language The target language
     * @return The next [Lesson] if available, null if no more lessons
     */
    suspend fun findNextLesson(currentLessonId: Int, language: Language, level: String? = null): Lesson?

    /**
     * Retrieves all vocabulary items for a specific lesson.
     *
     * @param lessonId The lesson's database ID
     * @return List of [VocabularyItem] objects for the lesson
     */
    suspend fun getVocabularyItems(lessonId: Int): List<VocabularyItem>

    /**
     * Searches vocabulary items by word or translation.
     *
     * @param query The search query
     * @param limit Max number of results
     * @return List of matching [VocabularyItem]
     */
    suspend fun searchVocabulary(query: String, limit: Int = 5): List<VocabularyItem>

    /**
     * Retrieves a vocabulary item by its database ID.
     */
    suspend fun findVocabularyById(id: Int): VocabularyItem?

    /**
     * Retrieves vocabulary items by their database IDs.
     */
    suspend fun findVocabularyByIds(ids: List<Int>): List<VocabularyItem>

    /**
     * Creates a new lesson in the database.
     *
     * @param lesson The lesson object to create
     * @return The created [Lesson] with assigned database ID
     */
    suspend fun create(lesson: Lesson): Lesson

    /**
     * Updates an existing lesson in the database.
     *
     * @param lesson The lesson object with updated data
     * @return The updated [Lesson]
     */
    suspend fun update(lesson: Lesson): Lesson

    /**
     * Deletes a lesson from the database.
     *
     * @param id The lesson's database ID
     * @return true if the lesson was deleted, false otherwise
     */
    suspend fun delete(id: Int): Boolean

    /**
     * Replaces all vocabulary items for a lesson.
     * Deletes existing items and inserts the new ones.
     */
    suspend fun replaceVocabulary(lessonId: Int, items: List<VocabularyItem>)
}
