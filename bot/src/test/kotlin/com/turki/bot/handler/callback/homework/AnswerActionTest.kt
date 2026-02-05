package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
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

class AnswerActionTest {
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
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 60L)

        val action = AnswerAction(userService, homeworkService, lessonService, userStateService, progressService, analytics)
        action(context, query, listOf("answer"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `wrong answer shows incorrect`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val lessonService = mockk<LessonService>(relaxed = true)
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 61L)
        val user = coreUser(id = 1L, telegramId = 61L)
        val question = HomeworkQuestion(
            id = 1,
            homeworkId = 1,
            questionType = QuestionType.MULTIPLE_CHOICE,
            questionText = "Q",
            options = listOf("A", "B"),
            correctAnswer = "A"
        )
        val homework = Homework(1, 1, listOf(question))

        coEvery { userService.findByTelegramId(61L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question, "B") } returns false
        every { HomeworkStateManager.getAnswers(user.telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(user.telegramId, any()) } just runs

        val action = AnswerAction(userService, homeworkService, lessonService, userStateService, progressService, analytics)
        action(context, query, listOf("answer", "1", "1", "1"))

        coVerify { context.editOrSendHtml(query, match { it.contains(S.exerciseIncorrect) }, any()) }
    }

    @Test
    fun `correct answer advances to next question`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 62L)
        val user = coreUser(id = 2L, telegramId = 62L)
        val question1 = HomeworkQuestion(1, 1, QuestionType.MULTIPLE_CHOICE, "Q1", listOf("A"), "A")
        val question2 = HomeworkQuestion(2, 1, QuestionType.TEXT_INPUT, "Q2", correctAnswer = "B")
        val homework = Homework(1, 1, listOf(question1, question2))

        coEvery { userService.findByTelegramId(62L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question1, "A") } returns true
        every { HomeworkStateManager.getAnswers(user.telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(user.telegramId, any()) } just runs
        every { HomeworkStateManager.setCurrentQuestion(user.telegramId, 1, 2, any()) } just runs

        val action = AnswerAction(userService, homeworkService, lessonService, userStateService, progressService, analytics)
        action(context, query, listOf("answer", "1", "1", "0"))

        coVerify { context.replaceWithHtml(query, match { it.contains(S.questionTitle(2)) }, any()) }
    }

    @Test
    fun `correct answer on last question submits`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val lessonService = mockk<LessonService>(relaxed = true)
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 63L)
        val user = coreUser(id = 3L, telegramId = 63L)
        val question = HomeworkQuestion(1, 1, QuestionType.MULTIPLE_CHOICE, "Q1", listOf("A"), "A")
        val homework = Homework(1, 1, listOf(question))
        val submission = HomeworkSubmission(
            id = 1,
            userId = user.id,
            homeworkId = 1,
            answers = mapOf(1 to "A"),
            score = 1,
            maxScore = 1,
            submittedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(63L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question, "A") } returns true
        every { HomeworkStateManager.getAnswers(user.telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(user.telegramId, any()) } just runs
        every { HomeworkStateManager.clearState(user.telegramId) } just runs
        coEvery { homeworkService.submitHomework(user.id, 1, any()) } returns submission
        coEvery { lessonService.getNextLesson(1, com.turki.core.domain.Language.TURKISH) } returns null

        val action = AnswerAction(userService, homeworkService, lessonService, userStateService, progressService, analytics)
        action(context, query, listOf("answer", "1", "1", "0"))

        coVerify { context.editOrSendHtml(query, match { it.contains(S.homeworkComplete(1, 1)) }, any()) }
    }
}
