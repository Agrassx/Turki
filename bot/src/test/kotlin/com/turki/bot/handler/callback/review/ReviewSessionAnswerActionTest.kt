package com.turki.bot.handler.callback.review

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
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
import com.turki.core.domain.UserState
import com.turki.core.domain.EventNames
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

class ReviewSessionAnswerActionTest {
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
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 100L)

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 101L)

        coEvery { userService.findByTelegramId(101L) } returns null

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "0"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `no state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 102L)
        val user = coreUser(id = 11L, telegramId = 102L)

        coEvery { userService.findByTelegramId(102L) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "0"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 103L)
        val user = coreUser(id = 12L, telegramId = 103L)
        val state = UserState(
            userId = user.id,
            state = "OTHER",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(103L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "0"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `invalid payload uses legacy handler`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 104L)
        val user = coreUser(id = 13L, telegramId = 104L)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
            payload = "not-json",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(104L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        mockkStatic("com.turki.bot.handler.callback.review.ReviewActionExtKt")
        coEvery {
            handleLegacyReviewAnswer(
                context,
                query,
                listOf("review_session_answer", "0"),
                userService,
                userStateService,
                reviewService,
                progressService,
                analytics,
                lessonService
            )
        } returns Unit

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "0"))

        coVerify {
            handleLegacyReviewAnswer(
                context,
                query,
                listOf("review_session_answer", "0"),
                userService,
                userStateService,
                reviewService,
                progressService,
                analytics,
                lessonService
            )
        }
    }

    @Test
    fun `correct answer on last question completes session`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 105L)
        val user = coreUser(id = 14L, telegramId = 105L)
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
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(session),
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(105L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { reviewService.updateCard(user.id, question.sourceId, true) } returns Unit
        coEvery { userStateService.clear(user.id) } returns mockk(relaxed = true)

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "0"))

        coVerify { progressService.recordReview(user.id) }
        coVerify { reviewService.updateCard(user.id, question.sourceId, true) }
        coVerify { userStateService.clear(user.id) }
        coVerify { analytics.log(EventNames.REVIEW_COMPLETED, user.id, any(), any()) }
    }

    @Test
    fun `incorrect answer advances to next`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 106L)
        val user = coreUser(id = 15L, telegramId = 106L)
        val question = ReviewQuestion(
            id = "q1",
            sourceType = ReviewSourceType.HOMEWORK,
            sourceId = 11,
            questionText = "soru",
            correctAnswer = "answer",
            options = listOf("answer", "wrong"),
            direction = TranslationDirection.RU_TO_TR
        )
        val session = ReviewSessionPayload(
            questions = listOf(question, question.copy(id = "q2")),
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

        coEvery { userService.findByTelegramId(106L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewSessionAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_session_answer", "1"))

        coVerify { progressService.recordReview(user.id) }
        coVerify(exactly = 0) { reviewService.updateCard(any(), any(), any()) }
        coVerify { userStateService.set(user.id, UserFlowState.REVIEW.name, any()) }
        coVerify { context.editOrSendHtml(query, match { it.contains("Неверно") }, any()) }
    }
}
