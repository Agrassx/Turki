package com.turki.bot.handler.callback.review

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.ReviewDifficulty
import com.turki.bot.service.ReviewQuestion
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.ReviewSourceType
import com.turki.bot.service.TranslationDirection
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.UserState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReviewSessionNextActionTest {
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
        val query = mockCallbackQuery(telegramId = 90L)

        coEvery { userService.findByTelegramId(90L) } returns null

        val action = ReviewSessionNextAction(userService, userStateService)
        action(context, query, listOf("review_session_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `no state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val query = mockCallbackQuery(telegramId = 91L)
        val user = coreUser(id = 4L, telegramId = 91L)

        coEvery { userService.findByTelegramId(91L) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = ReviewSessionNextAction(userService, userStateService)
        action(context, query, listOf("review_session_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val query = mockCallbackQuery(telegramId = 92L)
        val user = coreUser(id = 5L, telegramId = 92L)
        val state = UserState(
            userId = user.id,
            state = "OTHER",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(92L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewSessionNextAction(userService, userStateService)
        action(context, query, listOf("review_session_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `invalid payload returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val query = mockCallbackQuery(telegramId = 93L)
        val user = coreUser(id = 6L, telegramId = 93L)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
            payload = "not-json",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(93L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewSessionNextAction(userService, userStateService)
        action(context, query, listOf("review_session_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `sends next question`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val query = mockCallbackQuery(telegramId = 94L)
        val user = coreUser(id = 7L, telegramId = 94L)
        val question = ReviewQuestion(
            id = "q1",
            sourceType = ReviewSourceType.VOCABULARY,
            sourceId = 10,
            questionText = "merhaba",
            correctAnswer = "hello",
            options = listOf("hello", "bye"),
            direction = TranslationDirection.RU_TO_TR
        )
        val session = ReviewSessionPayload(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            difficulty = ReviewDifficulty.WARMUP
        )
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(session),
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(94L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewSessionNextAction(userService, userStateService)
        action(context, query, listOf("review_session_next"))

        coVerify { context.editOrSendHtml(query, match { it.contains(com.turki.bot.i18n.S.reviewProgress(1, 1)) }, any()) }
    }
}
