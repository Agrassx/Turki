package com.turki.bot.handler.command.lesson

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.core.domain.Language
import com.turki.core.domain.Lesson
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LessonsCommandTest {
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
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 20L
        val message = mockMessage("/lessons", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = LessonsCommand(userService, lessonService, progressService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `single page lessons sends title`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 21L
        val user = coreUser(id = 8L, telegramId = telegramId)
        val message = mockMessage("/lessons", mockTelegramUser(telegramId))
        val lesson = Lesson(
            id = 1,
            orderIndex = 1,
            targetLanguage = Language.TURKISH,
            title = "1. Intro",
            description = "desc",
            content = "content"
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getLessonsByLanguage(Language.TURKISH) } returns listOf(lesson)
        coEvery { progressService.getCompletedLessonIds(user.id) } returns emptySet()

        val action = LessonsCommand(userService, lessonService, progressService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.lessonsTitle, any()) }
    }

    @Test
    fun `multi page lessons adds pagination button`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 22L
        val user = coreUser(id = 9L, telegramId = telegramId)
        val message = mockMessage("/lessons", mockTelegramUser(telegramId))
        val lessons = (1..6).map { index ->
            Lesson(
                id = index,
                orderIndex = index,
                targetLanguage = Language.TURKISH,
                title = "$index. Intro",
                description = "desc",
                content = "content"
            )
        }

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getLessonsByLanguage(Language.TURKISH) } returns lessons
        coEvery { progressService.getCompletedLessonIds(user.id) } returns emptySet()

        val action = LessonsCommand(userService, lessonService, progressService, analytics)
        action(context, message)

        coVerify {
            context.sendHtml(any(), match { it.contains("Страница 1/") }, match { markup ->
                (markup as InlineKeyboardMarkup).keyboard.flatten().any { it.text == "▶️" }
            })
        }
    }
}
