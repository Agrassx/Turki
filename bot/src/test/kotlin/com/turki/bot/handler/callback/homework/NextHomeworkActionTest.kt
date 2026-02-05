package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.sendHtml
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

class NextHomeworkActionTest {
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
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 40L)

        val action = NextHomeworkAction(lessonService, userService, analytics)
        action(context, query, listOf("next_homework"))

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `no next lesson shows no next`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 41L)

        coEvery { lessonService.getNextLesson(1, Language.TURKISH) } returns null

        val action = NextHomeworkAction(lessonService, userService, analytics)
        action(context, query, listOf("next_homework", "1"))

        coVerify { context.sendHtml(any(), S.homeworkNoNext) }
    }

    @Test
    fun `next lesson renders homework start`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 42L)
        val nextLesson = Lesson(
            id = 2,
            orderIndex = 2,
            targetLanguage = Language.TURKISH,
            title = "Next",
            description = "desc",
            content = "content"
        )

        coEvery { userService.findByTelegramId(42L) } returns null
        coEvery { lessonService.getNextLesson(1, Language.TURKISH) } returns nextLesson

        val action = NextHomeworkAction(lessonService, userService, analytics)
        action(context, query, listOf("next_homework", "1"))

        coVerify { context.editOrSendHtml(query, S.homeworkStart, any()) }
    }
}
