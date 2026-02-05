package com.turki.bot.handler.callback.review

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ReviewDifficulty
import com.turki.bot.service.ReviewQuestion
import com.turki.bot.service.ReviewService
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.ReviewSourceType
import com.turki.bot.service.TranslationDirection
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
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

class ReviewDifficultyActionTest {
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
        val reviewService = mockk<ReviewService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 82L)

        coEvery { userService.findByTelegramId(82L) } returns null

        val action = ReviewDifficultyAction(userService, reviewService, userStateService, analytics)
        action(context, query, listOf("review_difficulty", "TRAINING"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `invalid difficulty falls back and shows empty`() = runBlocking {
        val userService = mockk<UserService>()
        val reviewService = mockk<ReviewService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 83L)
        val user = coreUser(id = 2L, telegramId = 83L)
        val emptySession = ReviewSessionPayload(
            questions = emptyList(),
            currentIndex = 0,
            correctCount = 0,
            difficulty = ReviewDifficulty.WARMUP
        )

        coEvery { userService.findByTelegramId(83L) } returns user
        coEvery { reviewService.buildReviewSession(user.id, user.currentLessonId, ReviewDifficulty.WARMUP) } returns emptySession

        val action = ReviewDifficultyAction(userService, reviewService, userStateService, analytics)
        action(context, query, listOf("review_difficulty", "BAD"))

        coVerify { reviewService.buildReviewSession(user.id, user.currentLessonId, ReviewDifficulty.WARMUP) }
        coVerify { context.editOrSendHtml(query, S.reviewEmpty, any()) }
    }

    @Test
    fun `starts session when questions exist`() = runBlocking {
        val userService = mockk<UserService>()
        val reviewService = mockk<ReviewService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 84L)
        val user = coreUser(id = 3L, telegramId = 84L)
        val question = ReviewQuestion(
            id = "q1",
            sourceType = ReviewSourceType.VOCABULARY,
            sourceId = 10,
            questionText = "merhaba",
            correctAnswer = "hello",
            options = listOf("hello", "bye"),
            direction = TranslationDirection.TR_TO_RU
        )
        val session = ReviewSessionPayload(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            difficulty = ReviewDifficulty.WARMUP
        )

        coEvery { userService.findByTelegramId(84L) } returns user
        coEvery { reviewService.buildReviewSession(user.id, user.currentLessonId, ReviewDifficulty.WARMUP) } returns session

        val action = ReviewDifficultyAction(userService, reviewService, userStateService, analytics)
        action(context, query, listOf("review_difficulty", "WARMUP"))

        coVerify { userStateService.set(user.id, UserFlowState.REVIEW.name, any()) }
        coVerify { context.editOrSendHtml(query, match { it.contains(S.reviewProgress(1, 1)) }, any()) }
    }
}
