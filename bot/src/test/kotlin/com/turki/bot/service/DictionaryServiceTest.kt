package com.turki.bot.service

import com.turki.core.domain.UserCustomDictionaryEntry
import com.turki.core.domain.UserDictionaryEntry
import com.turki.core.domain.VocabularyItem
import com.turki.core.repository.UserCustomDictionaryRepository
import com.turki.core.repository.UserDictionaryRepository
import com.turki.bot.testutil.TestClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryServiceTest {
    private val lessonService = mockk<LessonService>()
    private val userDictionaryRepository = mockk<UserDictionaryRepository>()
    private val userCustomDictionaryRepository = mockk<UserCustomDictionaryRepository>()
    private val fixedInstant = Instant.fromEpochMilliseconds(1_000)
    private val clock = TestClock(fixedInstant)
    private val service = DictionaryService(lessonService, userDictionaryRepository, userCustomDictionaryRepository, clock)

    @Test
    fun `toggleFavorite flips flag`() = runTest {
        val existing = UserDictionaryEntry(
            userId = 1,
            vocabularyId = 10,
            isFavorite = true,
            tags = "[]",
            addedAt = Instant.fromEpochMilliseconds(1)
        )
        coEvery { userDictionaryRepository.findByUserAndVocabulary(1, 10) } returns existing
        val slot = slot<UserDictionaryEntry>()
        coEvery { userDictionaryRepository.upsert(capture(slot)) } answers { slot.captured }

        val result = service.toggleFavorite(1, 10)

        assertFalse(result.isFavorite)
        assertEquals(false, slot.captured.isFavorite)
        assertEquals(fixedInstant, slot.captured.addedAt)
    }

    @Test
    fun `addFavorite sets favorite true and preserves addedAt`() = runTest {
        val existing = UserDictionaryEntry(
            userId = 1,
            vocabularyId = 11,
            isFavorite = false,
            tags = "[]",
            addedAt = Instant.fromEpochMilliseconds(123)
        )
        coEvery { userDictionaryRepository.findByUserAndVocabulary(1, 11) } returns existing
        val slot = slot<UserDictionaryEntry>()
        coEvery { userDictionaryRepository.upsert(capture(slot)) } answers { slot.captured }

        val result = service.addFavorite(1, 11)

        assertTrue(result.isFavorite)
        assertEquals(existing.addedAt, slot.captured.addedAt)
    }

    @Test
    fun `listFavoriteIds returns only favorite entries`() = runTest {
        coEvery { userDictionaryRepository.listFavorites(1, 1000) } returns listOf(
            UserDictionaryEntry(1, 1, true, "[]", Instant.fromEpochMilliseconds(1)),
            UserDictionaryEntry(1, 2, false, "[]", Instant.fromEpochMilliseconds(2)),
            UserDictionaryEntry(1, 3, true, "[]", Instant.fromEpochMilliseconds(3))
        )

        val result = service.listFavoriteIds(1)

        assertEquals(setOf(1, 3), result)
    }

    @Test
    fun `listUserDictionary merges favorites and custom and sorts by addedAt`() = runTest {
        val favEntry = UserDictionaryEntry(1, 10, true, "[]", Instant.fromEpochMilliseconds(5))
        val customEntry = UserCustomDictionaryEntry(1, 1, "word", "слово", null, null, Instant.fromEpochMilliseconds(10))

        coEvery { userDictionaryRepository.listFavorites(1, 1000) } returns listOf(favEntry)
        coEvery { lessonService.findVocabularyById(10) } returns VocabularyItem(10, 1, "Merhaba", "Привет")
        coEvery { userCustomDictionaryRepository.listByUser(1) } returns listOf(customEntry)

        val result = service.listUserDictionary(1)

        assertEquals(2, result.size)
        assertEquals("word", result.first().word)
    }

    @Test
    fun `clearUser deletes both dictionary sources`() = runTest {
        coEvery { userDictionaryRepository.deleteByUser(1) } returns true
        coEvery { userCustomDictionaryRepository.deleteByUser(1) } returns true

        service.clearUser(1)

        coVerify { userDictionaryRepository.deleteByUser(1) }
        coVerify { userCustomDictionaryRepository.deleteByUser(1) }
    }
}
