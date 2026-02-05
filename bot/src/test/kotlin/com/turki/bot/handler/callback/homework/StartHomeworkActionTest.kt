package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.QuestionType
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

class StartHomeworkActionTest {
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
    fun `missing lesson id returns`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 10L)

        val action = StartHomeworkAction(homeworkService, userService, userStateService, analytics)
        action(context, query, listOf("start_homework"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `no homework shows not ready`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 11L)

        coEvery { homeworkService.getHomeworkForLesson(1) } returns null

        val action = StartHomeworkAction(homeworkService, userService, userStateService, analytics)
        action(context, query, listOf("start_homework", "1"))

        coVerify { context.editOrSendHtml(query, S.homeworkNotReady, any()) }
    }

    @Test
    fun `user missing returns`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 12L)
        val homework = Homework(
            id = 1,
            lessonId = 1,
            questions = listOf(
                HomeworkQuestion(
                    id = 1,
                    homeworkId = 1,
                    questionType = QuestionType.TEXT_INPUT,
                    questionText = "Q?",
                    correctAnswer = "A"
                )
            )
        )

        coEvery { homeworkService.getHomeworkForLesson(1) } returns homework
        coEvery { userService.findByTelegramId(12L) } returns null

        val action = StartHomeworkAction(homeworkService, userService, userStateService, analytics)
        action(context, query, listOf("start_homework", "1"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `already completed shows completed message`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 13L)
        val user = coreUser(id = 1L, telegramId = 13L)
        val homework = Homework(
            id = 1,
            lessonId = 1,
            questions = listOf(
                HomeworkQuestion(
                    id = 1,
                    homeworkId = 1,
                    questionType = QuestionType.TEXT_INPUT,
                    questionText = "Q?",
                    correctAnswer = "A"
                )
            )
        )

        coEvery { homeworkService.getHomeworkForLesson(1) } returns homework
        coEvery { userService.findByTelegramId(13L) } returns user
        coEvery { homeworkService.hasCompletedHomework(user.id, homework.id) } returns true

        val action = StartHomeworkAction(homeworkService, userService, userStateService, analytics)
        action(context, query, listOf("start_homework", "1"))

        coVerify { context.editOrSendHtml(query, S.homeworkAlreadyCompleted, any()) }
    }

    @Test
    fun `starts homework sends first question`() = runBlocking {
        val homeworkService = mockk<HomeworkService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 14L)
        val user = coreUser(id = 2L, telegramId = 14L)
        val homework = Homework(
            id = 1,
            lessonId = 1,
            questions = listOf(
                HomeworkQuestion(
                    id = 1,
                    homeworkId = 1,
                    questionType = QuestionType.TEXT_INPUT,
                    questionText = "Q?",
                    correctAnswer = "A"
                )
            )
        )

        coEvery { homeworkService.getHomeworkForLesson(1) } returns homework
        coEvery { userService.findByTelegramId(14L) } returns user
        coEvery { homeworkService.hasCompletedHomework(user.id, homework.id) } returns false
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)

        val action = StartHomeworkAction(homeworkService, userService, userStateService, analytics)
        action(context, query, listOf("start_homework", "1"))

        coVerify { context.replaceWithHtml(query, match { it.contains(S.questionTitle(1)) }, any()) }
    }
}
