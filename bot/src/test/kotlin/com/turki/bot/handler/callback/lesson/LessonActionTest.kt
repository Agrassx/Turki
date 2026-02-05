package com.turki.bot.handler.callback.lesson

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
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

class LessonActionTest {
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
        val query = mockCallbackQuery(telegramId = 30L)

        val action = LessonAction(lessonService)
        action(context, query, listOf("lesson"))

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `lesson not found sends warning`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 31L)

        coEvery { lessonService.getLessonById(1) } returns null

        val action = LessonAction(lessonService)
        action(context, query, listOf("lesson", "1"))

        coVerify { context.sendHtml(any(), S.lessonNotFound) }
    }

    @Test
    fun `lesson found sends content`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 32L)
        val lesson = Lesson(
            id = 1,
            orderIndex = 1,
            targetLanguage = Language.TURKISH,
            title = "Intro",
            description = "desc",
            content = "content"
        )

        coEvery { lessonService.getLessonById(1) } returns lesson

        val action = LessonAction(lessonService)
        action(context, query, listOf("lesson", "1"))

        coVerify { context.sendHtml(any(), match { it.contains(lesson.title) }, any()) }
    }
}
