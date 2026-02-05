package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.coreUser
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
import com.turki.core.domain.VocabularyItem
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

class PracticeStartActionTest {
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
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 70L)

        coEvery { userService.findByTelegramId(70L) } returns null

        val action = PracticeStartAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("practice_start"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `lesson not found shows warning`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 71L)
        val user = coreUser(id = 1L, telegramId = 71L)

        coEvery { userService.findByTelegramId(71L) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns null

        val action = PracticeStartAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("practice_start"))

        coVerify { context.sendHtml(any(), S.lessonNotFound) }
    }

    @Test
    fun `empty exercises shows not ready`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 72L)
        val user = coreUser(id = 2L, telegramId = 72L)
        val lesson = Lesson(1, 1, Language.TURKISH, title = "L", description = "d", content = "c")

        coEvery { userService.findByTelegramId(72L) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns lesson
        coEvery { exerciseService.buildLessonExercises(lesson.id) } returns emptyList()

        val action = PracticeStartAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("practice_start"))

        coVerify { context.editOrSendHtml(query, S.exerciseNotReady, any()) }
    }

    @Test
    fun `starts practice sends first exercise`() = runBlocking {
        val lessonService = mockk<LessonService>()
        val exerciseService = mockk<ExerciseService>()
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 73L)
        val user = coreUser(id = 3L, telegramId = 73L)
        val lesson = Lesson(1, 1, Language.TURKISH, title = "L", description = "d", content = "c")
        val exercise = com.turki.bot.service.ExerciseItem(
            vocabularyId = 10,
            prompt = "prompt",
            options = listOf("a", "b"),
            correctOption = "a",
            explanation = ""
        )
        val vocab = VocabularyItem(id = 10, lessonId = 1, word = "a", translation = "b")

        coEvery { userService.findByTelegramId(73L) } returns user
        coEvery { lessonService.getLessonById(user.currentLessonId) } returns lesson
        coEvery { exerciseService.buildLessonExercises(lesson.id) } returns listOf(exercise)
        coEvery { lessonService.findVocabularyById(10) } returns vocab
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)

        val action = PracticeStartAction(lessonService, exerciseService, userService, userStateService, analytics)
        action(context, query, listOf("practice_start"))

        coVerify { context.editOrSendHtml(query, match { it.contains(vocab.word) }, any()) }
    }
}
