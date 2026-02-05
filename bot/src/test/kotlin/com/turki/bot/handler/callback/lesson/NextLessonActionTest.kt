package com.turki.bot.handler.callback.lesson

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
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

class NextLessonActionTest {
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
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 40L)

        coEvery { userService.findByTelegramId(40L) } returns null

        val action = NextLessonAction(userService, lessonService)
        action(context, query, listOf("next_lesson"))

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>()) }
    }

    @Test
    fun `no next lesson shows completion`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 41L)
        val user = coreUser(id = 1L, telegramId = 41L)

        coEvery { userService.findByTelegramId(41L) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns null

        val action = NextLessonAction(userService, lessonService)
        action(context, query, listOf("next_lesson"))

        coVerify { context.sendHtml(any(), S.allLessonsCompleted) }
    }

    @Test
    fun `next lesson sends content`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val query = mockCallbackQuery(telegramId = 42L)
        val user = coreUser(id = 2L, telegramId = 42L)
        val lesson = Lesson(
            id = 2,
            orderIndex = 2,
            targetLanguage = Language.TURKISH,
            title = "Next",
            description = "desc",
            content = "content"
        )

        coEvery { userService.findByTelegramId(42L) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns lesson

        val action = NextLessonAction(userService, lessonService)
        action(context, query, listOf("next_lesson"))

        coVerify { context.sendHtml(any(), match { it.contains(lesson.title) }, any()) }
    }
}
