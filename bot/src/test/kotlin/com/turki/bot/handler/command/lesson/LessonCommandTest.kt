package com.turki.bot.handler.command.lesson

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.i18n.S
import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
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

class LessonCommandTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        mockkStatic("dev.inmo.tgbotapi.extensions.utils.extensions.raw.MessageKt")
        stubSendHtml(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user not registered sends warning`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 7L
        val message = mockMessage("/lesson", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = LessonCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `no lesson sends completion text`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 8L
        val user = coreUser(id = 1L, telegramId = telegramId)
        val message = mockMessage("/lesson", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns null

        val action = LessonCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.allLessonsCompleted) }
    }

    @Test
    fun `lesson found sends content`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 9L
        val user = coreUser(id = 2L, telegramId = telegramId)
        val message = mockMessage("/lesson", mockTelegramUser(telegramId))
        val lesson = Lesson(
            id = 1,
            orderIndex = 1,
            targetLanguage = Language.TURKISH,
            title = "Intro",
            description = "desc",
            content = "content"
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns lesson

        val action = LessonCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains(lesson.title) }, any()) }
    }
}
