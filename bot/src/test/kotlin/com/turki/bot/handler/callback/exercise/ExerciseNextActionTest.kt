package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.i18n.S
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.UserState
import com.turki.core.domain.VocabularyItem
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

class ExerciseNextActionTest {
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
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 70L)

        coEvery { userService.findByTelegramId(70L) } returns null

        val action = ExerciseNextAction(userService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 71L)
        val user = coreUser(id = 8L, telegramId = 71L)
        val state = UserState(
            userId = user.id,
            state = "OTHER",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(71L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ExerciseNextAction(userService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `completes exercise when last question`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 72L)
        val user = coreUser(id = 9L, telegramId = 72L)
        val payload = ExerciseFlowPayload(
            lessonId = 1,
            exerciseIndex = 0,
            vocabularyIds = listOf(10),
            optionsByVocabId = mapOf(10 to listOf("a", "b")),
            correctByVocabId = mapOf(10 to "a"),
            explanationsByVocabId = emptyMap()
        )
        val state = UserState(
            userId = user.id,
            state = UserFlowState.EXERCISE.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(payload),
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(72L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { userStateService.clear(user.id) } returns mockk(relaxed = true)

        val action = ExerciseNextAction(userService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_next"))

        coVerify { progressService.recordPractice(user.id) }
        coVerify { context.editOrSendHtml(query, S.exerciseComplete, any()) }
    }

    @Test
    fun `advances to next exercise`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 73L)
        val user = coreUser(id = 10L, telegramId = 73L)
        val payload = ExerciseFlowPayload(
            lessonId = 1,
            exerciseIndex = 0,
            vocabularyIds = listOf(10, 11),
            optionsByVocabId = mapOf(
                10 to listOf("a", "b"),
                11 to listOf("c", "d")
            ),
            correctByVocabId = mapOf(10 to "a", 11 to "c"),
            explanationsByVocabId = emptyMap()
        )
        val state = UserState(
            userId = user.id,
            state = UserFlowState.EXERCISE.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(payload),
            updatedAt = Instant.fromEpochSeconds(0)
        )
        val vocab = VocabularyItem(
            id = 11,
            lessonId = 1,
            word = "kalem",
            translation = "pen",
            pronunciation = null,
            example = null
        )

        coEvery { userService.findByTelegramId(73L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)
        coEvery { lessonService.findVocabularyById(11) } returns vocab

        val action = ExerciseNextAction(userService, userStateService, progressService, lessonService, analytics)
        action(context, query, listOf("exercise_next"))

        coVerify { context.editOrSendHtml(query, match { it.contains(S.exercisePrompt(vocab.word)) }, any()) }
    }
}
