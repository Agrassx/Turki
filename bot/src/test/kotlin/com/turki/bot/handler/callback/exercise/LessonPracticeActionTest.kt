package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Lesson
import com.turki.core.domain.Language
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

class LessonPracticeActionTest {
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
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 80L)

        val action = LessonPracticeAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("lesson_practice"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `lesson not found shows warning`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 81L)

        coEvery { lessonService.getLessonById(1) } returns null

        val action = LessonPracticeAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("lesson_practice", "1"))

        coVerify { context.sendHtml(any(), S.lessonNotFound) }
    }

    @Test
    fun `empty exercises shows not ready`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 82L)
        val lesson = Lesson(1, 1, Language.TURKISH, title = "L", description = "d", content = "c")

        coEvery { lessonService.getLessonById(1) } returns lesson
        coEvery { exerciseService.buildLessonExercises(lesson.id) } returns emptyList()

        val action = LessonPracticeAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("lesson_practice", "1"))

        coVerify { context.editOrSendHtml(query, S.exerciseNotReady, any()) }
    }
}
