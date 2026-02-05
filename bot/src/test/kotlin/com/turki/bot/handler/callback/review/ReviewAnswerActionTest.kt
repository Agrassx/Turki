package com.turki.bot.handler.callback.review

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewFlowPayload
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.UserState
import com.turki.core.domain.VocabularyItem
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

class ReviewAnswerActionTest {
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
        val query = mockCallbackQuery(telegramId = 110L)

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `invalid vocab id returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 111L)

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "bad", "1"))

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
        val query = mockCallbackQuery(telegramId = 112L)

        coEvery { userService.findByTelegramId(112L) } returns null

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "1", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `no state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>(relaxed = true)
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 113L)
        val user = coreUser(id = 16L, telegramId = 113L)

        coEvery { userService.findByTelegramId(113L) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "1", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong state returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val reviewService = mockk<ReviewService>(relaxed = true)
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 114L)
        val user = coreUser(id = 17L, telegramId = 114L)
        val state = UserState(
            userId = user.id,
            state = "OTHER",
            payload = "{}",
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(114L) } returns user
        coEvery { userStateService.get(user.id) } returns state

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "1", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `completes review when last card`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 115L)
        val user = coreUser(id = 18L, telegramId = 115L)
        val payload = ReviewFlowPayload(vocabularyIds = listOf(10), index = 0)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
            payload = com.turki.bot.handler.callback.CALLBACK_JSON.encodeToString(payload),
            updatedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(115L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { reviewService.updateCard(user.id, 10, true) } returns Unit
        coEvery { userStateService.clear(user.id) } returns mockk(relaxed = true)

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "10", "1"))

        coVerify { progressService.recordReview(user.id) }
        coVerify { reviewService.updateCard(user.id, 10, true) }
        coVerify { analytics.log(EventNames.REVIEW_COMPLETED, user.id, any(), any()) }
        coVerify { context.editOrSendHtml(query, match { it.contains(S.reviewDone) }, any()) }
    }

    @Test
    fun `advances to next card`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val reviewService = mockk<ReviewService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 116L)
        val user = coreUser(id = 19L, telegramId = 116L)
        val payload = ReviewFlowPayload(vocabularyIds = listOf(10, 11), index = 0)
        val state = UserState(
            userId = user.id,
            state = UserFlowState.REVIEW.name,
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

        coEvery { userService.findByTelegramId(116L) } returns user
        coEvery { userStateService.get(user.id) } returns state
        coEvery { reviewService.updateCard(user.id, 10, false) } returns Unit
        coEvery { userStateService.set(user.id, UserFlowState.REVIEW.name, any()) } returns mockk(relaxed = true)
        coEvery { lessonService.findVocabularyById(11) } returns vocab

        val action = ReviewAnswerAction(userService, userStateService, reviewService, progressService, analytics, lessonService)
        action(context, query, listOf("review_answer", "10", "0"))

        coVerify { progressService.recordReview(user.id) }
        coVerify { reviewService.updateCard(user.id, 10, false) }
        coVerify { context.editOrSendHtml(query, match { it.contains(S.reviewCardTitle(vocab.word)) }, any()) }
    }
}
