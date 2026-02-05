package com.turki.bot.handler.callback.lesson

import com.turki.bot.util.editOrSendHtml

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.handler.coreUser
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

class LessonsListActionTest {
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
    fun `single page renders title`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val lesson = Lesson(
            id = 1,
            orderIndex = 1,
            targetLanguage = Language.TURKISH,
            title = "Intro",
            description = "desc",
            content = "content"
        )
        val user = coreUser(id = 1L, telegramId = 10L)
        val query = mockCallbackQuery(telegramId = 10L)

        coEvery { lessonService.getLessonsByLanguage(Language.TURKISH) } returns listOf(lesson)
        coEvery { userService.findByTelegramId(10L) } returns user
        coEvery { progressService.getCompletedLessonIds(user.id) } returns emptySet()

        val action = LessonsListAction(lessonService, userService, progressService, analytics)
        action(context, query, listOf("lessons_list"))

        coVerify { context.editOrSendHtml(query, S.lessonsTitle, any()) }
    }

    @Test
    fun `multi page includes navigation`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val userService = mockk<UserService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
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
        val user = coreUser(id = 1L, telegramId = 11L)
        val query = mockCallbackQuery(telegramId = 11L)

        coEvery { lessonService.getLessonsByLanguage(Language.TURKISH) } returns lessons
        coEvery { userService.findByTelegramId(11L) } returns user
        coEvery { progressService.getCompletedLessonIds(user.id) } returns emptySet()

        val action = LessonsListAction(lessonService, userService, progressService, analytics)
        action(context, query, listOf("lessons_list", "0"))

        coVerify {
            context.editOrSendHtml(query, match { it.contains("Страница 1/") }, match { markup ->
                (markup as InlineKeyboardMarkup).keyboard.flatten().any { it.text == "▶️" }
            })
        }
    }
}
