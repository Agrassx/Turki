package com.turki.bot.service

import com.turki.core.domain.UserDictionaryEntry
import com.turki.core.domain.UserCustomDictionaryEntry
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.UserCustomDictionaryRepository
import com.turki.core.repository.UserDictionaryRepository
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DictionaryService(
    private val lessonService: LessonService,
    private val userDictionaryRepository: UserDictionaryRepository,
    private val userCustomDictionaryRepository: UserCustomDictionaryRepository
) {
    private val json = Json { encodeDefaults = true }

    suspend fun search(query: String, limit: Int = 5): List<VocabularyItem> =
        lessonService.searchVocabulary(query, limit)

    suspend fun toggleFavorite(userId: Long, vocabularyId: Int): UserDictionaryEntry {
        val existing = userDictionaryRepository.findByUserAndVocabulary(userId, vocabularyId)
        val next = UserDictionaryEntry(
            userId = userId,
            vocabularyId = vocabularyId,
            isFavorite = existing?.isFavorite?.not() ?: true,
            tags = existing?.tags ?: json.encodeToString(emptyList<String>()),
            addedAt = Clock.System.now()
        )
        return userDictionaryRepository.upsert(next)
    }

    suspend fun addFavorite(userId: Long, vocabularyId: Int): UserDictionaryEntry {
        val existing = userDictionaryRepository.findByUserAndVocabulary(userId, vocabularyId)
        val next = UserDictionaryEntry(
            userId = userId,
            vocabularyId = vocabularyId,
            isFavorite = true,
            tags = existing?.tags ?: json.encodeToString(emptyList<String>()),
            addedAt = existing?.addedAt ?: Clock.System.now()
        )
        return userDictionaryRepository.upsert(next)
    }

    suspend fun removeFavorite(userId: Long, vocabularyId: Int): UserDictionaryEntry {
        val existing = userDictionaryRepository.findByUserAndVocabulary(userId, vocabularyId)
        val next = UserDictionaryEntry(
            userId = userId,
            vocabularyId = vocabularyId,
            isFavorite = false,
            tags = existing?.tags ?: json.encodeToString(emptyList<String>()),
            addedAt = existing?.addedAt ?: Clock.System.now()
        )
        return userDictionaryRepository.upsert(next)
    }

    suspend fun addAllFavorites(userId: Long, vocabularyIds: List<Int>): Int {
        var added = 0
        for (vocabId in vocabularyIds) {
            val entry = addFavorite(userId, vocabId)
            if (entry.isFavorite) {
                added++
            }
        }
        return added
    }

    suspend fun listFavoriteIds(userId: Long): Set<Int> {
        return userDictionaryRepository.listFavorites(userId, limit = 1000)
            .filter { it.isFavorite }
            .map { it.vocabularyId }
            .toSet()
    }

    suspend fun addCustomWord(
        userId: Long,
        word: String,
        translation: String,
        pronunciation: String? = null,
        example: String? = null
    ): UserCustomDictionaryEntry {
        val entry = UserCustomDictionaryEntry(
            id = 0,
            userId = userId,
            word = word,
            translation = translation,
            pronunciation = pronunciation,
            example = example,
            addedAt = Clock.System.now()
        )
        return userCustomDictionaryRepository.create(entry)
    }

    suspend fun listUserDictionary(userId: Long): List<DictionaryEntryView> {
        val favorites = userDictionaryRepository.listFavorites(userId, limit = 1000)
        val vocabItems = favorites.mapNotNull { entry ->
            lessonService.findVocabularyById(entry.vocabularyId)?.let { vocab ->
                DictionaryEntryView(
                    word = vocab.word,
                    translation = vocab.translation,
                    pronunciation = vocab.pronunciation,
                    example = vocab.example,
                    addedAt = entry.addedAt
                )
            }
        }

        val custom = userCustomDictionaryRepository.listByUser(userId).map {
            DictionaryEntryView(
                word = it.word,
                translation = it.translation,
                pronunciation = it.pronunciation,
                example = it.example,
                addedAt = it.addedAt
            )
        }

        return (vocabItems + custom).sortedByDescending { it.addedAt }
    }

    suspend fun setTags(userId: Long, vocabularyId: Int, tags: List<String>): UserDictionaryEntry {
        val existing = userDictionaryRepository.findByUserAndVocabulary(userId, vocabularyId)
        val next = UserDictionaryEntry(
            userId = userId,
            vocabularyId = vocabularyId,
            isFavorite = existing?.isFavorite ?: true,
            tags = json.encodeToString(tags),
            addedAt = existing?.addedAt ?: Clock.System.now()
        )
        return userDictionaryRepository.upsert(next)
    }

    suspend fun getEntry(userId: Long, vocabularyId: Int): UserDictionaryEntry? =
        userDictionaryRepository.findByUserAndVocabulary(userId, vocabularyId)

    suspend fun clearUser(userId: Long) {
        userDictionaryRepository.deleteByUser(userId)
        userCustomDictionaryRepository.deleteByUser(userId)
    }
}

data class DictionaryEntryView(
    val word: String,
    val translation: String,
    val pronunciation: String?,
    val example: String?,
    val addedAt: kotlinx.datetime.Instant
)
