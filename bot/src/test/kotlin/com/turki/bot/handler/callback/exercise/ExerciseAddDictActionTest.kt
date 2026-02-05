package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExerciseAddDictActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        stubEditAndSendHtml(context)
        mockkStatic("com.turki.bot.handler.callback.exercise.ExerciseActionExtKt")
        coEvery { advanceExercise(any(), any(), any(), any(), any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invalid parts returns`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 60L)

        val action = ExerciseAddDictAction(userService, dictionaryService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_add_dict"))

        coVerify(exactly = 0) { dictionaryService.addFavorite(any(), any()) }
        coVerify(exactly = 0) { advanceExercise(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 61L)

        coEvery { userService.findByTelegramId(61L) } returns null

        val action = ExerciseAddDictAction(userService, dictionaryService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_add_dict", "12"))

        coVerify(exactly = 0) { dictionaryService.addFavorite(any(), any()) }
        coVerify(exactly = 0) { advanceExercise(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `adds favorite and advances`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 62L)
        val user = coreUser(id = 7L, telegramId = 62L)
        val entry = com.turki.core.domain.UserDictionaryEntry(
            userId = user.id,
            vocabularyId = 44,
            isFavorite = true,
            tags = "",
            addedAt = kotlinx.datetime.Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(62L) } returns user
        coEvery { dictionaryService.addFavorite(user.id, 44) } returns entry

        val action = ExerciseAddDictAction(userService, dictionaryService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_add_dict", "44"))

        coVerify { dictionaryService.addFavorite(user.id, 44) }
        coVerify { advanceExercise(context, query, userService, userStateService, progressService, lessonService, analytics) }
    }
}
