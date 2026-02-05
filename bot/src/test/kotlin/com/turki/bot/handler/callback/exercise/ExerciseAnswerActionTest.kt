package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.EventNames
import com.turki.core.domain.Lesson
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExerciseAnswerActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        stubEditAndSendHtml(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invalid parts returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 90L)

        val action = ExerciseAnswerAction(userService, userStateService, analytics)
        action(context, query, listOf("exercise_answer"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 91L)
        val user = com.turki.bot.handler.coreUser(id = 1L, telegramId = 91L)
        val state = com.turki.core.domain.UserState(
            userId = user.id,
            state = "OTHER",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(91L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ExerciseAnswerAction(userService, userStateService, analytics)
        action(context, query, listOf("exercise_answer", "1", "10", "0"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `correct answer shows success`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 92L)
        val user = com.turki.bot.handler.coreUser(id = 2L, telegramId = 92L)
        val payload = ExerciseFlowPayload(
            lessonId = 1,
            exerciseIndex = 0,
            vocabularyIds = listOf(10),
            optionsByVocabId = mapOf(10 to listOf("a", "b")),
            correctByVocabId = mapOf(10 to "a"),
            explanationsByVocabId = emptyMap()
        )
        val state = com.turki.core.domain.UserState(
            userId = user.id,
            state = UserFlowState.EXERCISE.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(payload),
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(92L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ExerciseAnswerAction(userService, userStateService, analytics)
        action(context, query, listOf("exercise_answer", "1", "10", "0"))

        coVerify { context.editOrSendHtml(query, match { it.contains(S.exerciseCorrect) }, any()) }
    }
}
