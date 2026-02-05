package com.turki.bot.handler.callback.lesson

import com.turki.bot.util.sendHtml
import com.turki.bot.util.replaceWithHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
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

class LessonStartActionTest {
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
    fun `missing lesson id does nothing`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 20L)

        val action = LessonStartAction(lessonService, userService, progressService, analytics)
        action(context, query, listOf("lesson_start"))

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `lesson not found sends warning`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 21L)

        coEvery { lessonService.getLessonById(1) } returns null

        val action = LessonStartAction(lessonService, userService, progressService, analytics)
        action(context, query, listOf("lesson_start", "1"))

        coVerify { context.sendHtml(any(), S.lessonNotFound) }
    }

    @Test
    fun `lesson found and user exists marks started`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 22L)
        val lesson = Lesson(
            id = 1,
            orderIndex = 1,
            targetLanguage = Language.TURKISH,
            title = "Intro",
            description = "desc",
            content = "content"
        )
        val user = coreUser(id = 2L, telegramId = 22L)

        coEvery { lessonService.getLessonById(1) } returns lesson
        coEvery { userService.findByTelegramId(22L) } returns user

        val action = LessonStartAction(lessonService, userService, progressService, analytics)
        action(context, query, listOf("lesson_start", "1"))

        coVerify { context.replaceWithHtml(query, match { it.contains(lesson.title) }, any()) }
        coVerify { progressService.markLessonStarted(user.id, lesson.id) }
    }
}
