package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeworkNextActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        stubEditAndSendHtml(context)
        mockkObject(HomeworkStateManager)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invalid parts returns`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 30L)

        val action = HomeworkNextAction(userService, homeworkService, userStateService, progressService, analytics, lessonService)
        action(context, query, listOf("hw_next"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 31L)

        coEvery { userService.findByTelegramId(31L) } returns null

        val action = HomeworkNextAction(userService, homeworkService, userStateService, progressService, analytics, lessonService)
        action(context, query, listOf("hw_next", "1", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `homework missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 32L)
        val user = coreUser(id = 1L, telegramId = 32L)

        coEvery { userService.findByTelegramId(32L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns null

        val action = HomeworkNextAction(userService, homeworkService, userStateService, progressService, analytics, lessonService)
        action(context, query, listOf("hw_next", "1", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `advances to next question`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 33L)
        val user = coreUser(id = 2L, telegramId = 33L)
        val homework = Homework(
            id = 1,
            lessonId = 1,
            questions = listOf(
                HomeworkQuestion(1, 1, QuestionType.TEXT_INPUT, "Q1", correctAnswer = "A"),
                HomeworkQuestion(2, 1, QuestionType.TEXT_INPUT, "Q2", correctAnswer = "B")
            )
        )

        coEvery { userService.findByTelegramId(33L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)
        every { HomeworkStateManager.setCurrentQuestion(user.telegramId, 1, 2, any()) } just runs

        val action = HomeworkNextAction(userService, homeworkService, userStateService, progressService, analytics, lessonService)
        action(context, query, listOf("hw_next", "1", "1"))

        coVerify { context.replaceWithHtml(query, match { it.contains(com.turki.bot.i18n.S.questionTitle(2)) }, any()) }
    }

    @Test
    fun `last question submits homework`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 34L)
        val user = coreUser(id = 3L, telegramId = 34L)
        val homework = Homework(
            id = 1,
            lessonId = 1,
            questions = listOf(
                HomeworkQuestion(1, 1, QuestionType.TEXT_INPUT, "Q1", correctAnswer = "A")
            )
        )
        val submission = HomeworkSubmission(
            id = 1,
            userId = user.id,
            homeworkId = 1,
            answers = mapOf(1 to "A"),
            score = 1,
            maxScore = 1,
            submittedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(34L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(any(), any()) } returns true
        every { HomeworkStateManager.getAnswers(user.telegramId) } returns mutableMapOf(1 to "A")
        every { HomeworkStateManager.clearState(user.telegramId) } just runs
        coEvery { homeworkService.submitHomework(user.id, 1, any()) } returns submission
        coEvery { lessonService.getNextLesson(1, com.turki.core.domain.Language.TURKISH) } returns null

        val action = HomeworkNextAction(userService, homeworkService, userStateService, progressService, analytics, lessonService)
        action(context, query, listOf("hw_next", "1", "1"))

        coVerify { context.editOrSendHtml(query, match { it.contains(com.turki.bot.i18n.S.homeworkComplete(1, 1)) }, any()) }
    }
}
