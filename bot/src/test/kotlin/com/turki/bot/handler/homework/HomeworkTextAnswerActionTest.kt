package com.turki.bot.handler.homework

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.HomeworkSubmission
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
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

class HomeworkTextAnswerActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        mockkStatic("dev.inmo.tgbotapi.extensions.utils.extensions.raw.MessageKt")
        mockkStatic("dev.inmo.tgbotapi.extensions.api.DeleteMessageKt")
        mockkObject(HomeworkStateManager)
        stubSendHtml(context)
        coEvery { context.delete(any<dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage>()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `no current question returns without sending`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 55L
        val message = mockMessage("answer", mockTelegramUser(telegramId))

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns null

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>(), any()) }
        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `user missing returns early`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 56L
        val message = mockMessage("answer", mockTelegramUser(telegramId))

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>(), any()) }
        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `homework missing returns early`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 57L
        val message = mockMessage("answer", mockTelegramUser(telegramId))
        val user = coreUser(id = 1L, telegramId = telegramId)

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns null

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>(), any()) }
        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `incorrect text answer sends feedback`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 58L
        val message = mockMessage("wrong", mockTelegramUser(telegramId))
        val user = coreUser(id = 2L, telegramId = telegramId)

        val question = HomeworkQuestion(
            id = 10,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q?",
            correctAnswer = "A"
        )
        val homework = Homework(id = 1, lessonId = 1, questions = listOf(question))

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null
        every { HomeworkStateManager.clearCurrentQuestion(telegramId) } just runs

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question, "wrong") } returns false
        coEvery { lessonService.getVocabulary(homework.lessonId) } returns emptyList()

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains(S.exerciseIncorrect) }, any()) }
    }

    @Test
    fun `correct answer sends next multiple choice question`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 59L
        val message = mockMessage("A", mockTelegramUser(telegramId))
        val user = coreUser(id = 3L, telegramId = telegramId)

        val question1 = HomeworkQuestion(
            id = 10,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q1",
            correctAnswer = "A"
        )
        val question2 = HomeworkQuestion(
            id = 11,
            homeworkId = 1,
            questionType = QuestionType.MULTIPLE_CHOICE,
            questionText = "Q2",
            options = listOf("1", "2"),
            correctAnswer = "1"
        )
        val homework = Homework(id = 1, lessonId = 1, questions = listOf(question1, question2))

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question1, "A") } returns true

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains(S.questionTitle(2)) }, any()) }
    }

    @Test
    fun `correct answer sends next text question and updates state`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 60L
        val message = mockMessage("A", mockTelegramUser(telegramId))
        val user = coreUser(id = 4L, telegramId = telegramId)

        val question1 = HomeworkQuestion(
            id = 10,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q1",
            correctAnswer = "A"
        )
        val question2 = HomeworkQuestion(
            id = 11,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q2",
            correctAnswer = "B"
        )
        val homework = Homework(id = 1, lessonId = 1, questions = listOf(question1, question2))

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null
        every { HomeworkStateManager.setCurrentQuestion(telegramId, 1, 11, any()) } just runs

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question1, "A") } returns true

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains(S.questionTitle(2)) }) }
        coVerify { userStateService.set(user.id, any(), any()) }
    }

    @Test
    fun `perfect score marks lesson completed`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 61L
        val message = mockMessage("A", mockTelegramUser(telegramId))
        val user = coreUser(id = 5L, telegramId = telegramId)

        val question = HomeworkQuestion(
            id = 10,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q1",
            correctAnswer = "A"
        )
        val homework = Homework(id = 1, lessonId = 1, questions = listOf(question))
        val submission = HomeworkSubmission(
            id = 1,
            userId = user.id,
            homeworkId = 1,
            answers = mapOf(10 to "A"),
            score = 1,
            maxScore = 1,
            submittedAt = Instant.fromEpochSeconds(0)
        )

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null
        every { HomeworkStateManager.clearState(telegramId) } just runs

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question, "A") } returns true
        coEvery { homeworkService.submitHomework(user.id, 1, any()) } returns submission
        coEvery { lessonService.getNextLesson(homework.lessonId, Language.TURKISH) } returns null

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify { progressService.markLessonCompleted(user.id, homework.lessonId) }
        coVerify { context.sendHtml(any(), match { it.contains(S.homeworkComplete(1, 1)) }, any()) }
    }

    @Test
    fun `non perfect score does not mark lesson completed`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analyticsService = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 62L
        val message = mockMessage("A", mockTelegramUser(telegramId))
        val user = coreUser(id = 6L, telegramId = telegramId)

        val question = HomeworkQuestion(
            id = 10,
            homeworkId = 1,
            questionType = QuestionType.TEXT_INPUT,
            questionText = "Q1",
            correctAnswer = "A"
        )
        val homework = Homework(id = 1, lessonId = 1, questions = listOf(question))
        val submission = HomeworkSubmission(
            id = 2,
            userId = user.id,
            homeworkId = 1,
            answers = mapOf(10 to "A"),
            score = 0,
            maxScore = 1,
            submittedAt = Instant.fromEpochSeconds(0)
        )

        every { HomeworkStateManager.getCurrentQuestion(telegramId) } returns (1 to 10)
        every { HomeworkStateManager.getAnswers(telegramId) } returns mutableMapOf()
        every { HomeworkStateManager.setAnswers(telegramId, any()) } just runs
        every { HomeworkStateManager.getQuestionMessageId(telegramId) } returns null
        every { HomeworkStateManager.clearState(telegramId) } just runs

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        every { homeworkService.isAnswerCorrect(question, "A") } returns true
        coEvery { homeworkService.submitHomework(user.id, 1, any()) } returns submission
        coEvery { lessonService.getNextLesson(homework.lessonId, Language.TURKISH) } returns null

        val action = HomeworkTextAnswerAction(
            homeworkService,
            userService,
            progressService,
            lessonService,
            userStateService,
            analyticsService
        )

        action(context, message)

        coVerify(exactly = 0) { progressService.markLessonCompleted(user.id, homework.lessonId) }
        coVerify { context.sendHtml(any(), match { it.contains(S.homeworkResult(0, 1)) }, any()) }
    }
}
