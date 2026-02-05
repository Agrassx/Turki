package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.HomeworkStateManager
import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.core.domain.Homework
import com.turki.core.domain.HomeworkQuestion
import com.turki.core.domain.QuestionType
import com.turki.core.domain.VocabularyItem
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeworkAddDictActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        mockkStatic("com.turki.bot.handler.callback.homework.HomeworkActionExtKt")
        stubEditAndSendHtml(context)
        mockkObject(HomeworkStateManager)
        coEvery { advanceHomework(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invalid parts returns`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val dictionaryService = mockk<DictionaryService>()
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 50L)

        val action = HomeworkAddDictAction(
            userService,
            homeworkService,
            dictionaryService,
            lessonService,
            userStateService,
            progressService,
            analytics
        )
        action(context, query, listOf("hw_add_dict"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `adds vocab when resolved`() = runBlocking {
        val userService = mockk<UserService>()
        val homeworkService = mockk<HomeworkService>()
        val dictionaryService = mockk<DictionaryService>()
        val lessonService = mockk<LessonService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val progressService = mockk<ProgressService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 51L)
        val user = coreUser(id = 1L, telegramId = 51L)
        val question = HomeworkQuestion(1, 1, QuestionType.TEXT_INPUT, "Q", correctAnswer = "hello")
        val homework = Homework(1, 1, listOf(question))
        val vocab = VocabularyItem(id = 10, lessonId = 1, word = "hello", translation = "merhaba")
        val entry = com.turki.core.domain.UserDictionaryEntry(
            userId = user.id,
            vocabularyId = 10,
            isFavorite = true,
            tags = "",
            addedAt = kotlinx.datetime.Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(51L) } returns user
        coEvery { homeworkService.getHomeworkById(1) } returns homework
        coEvery { lessonService.getVocabulary(1) } returns listOf(vocab)
        coEvery { dictionaryService.addFavorite(user.id, 10) } returns entry
        every { HomeworkStateManager.getAnswers(user.telegramId) } returns mutableMapOf(1 to "hello")
        every { HomeworkStateManager.clearState(user.telegramId) } just runs

        val action = HomeworkAddDictAction(
            userService,
            homeworkService,
            dictionaryService,
            lessonService,
            userStateService,
            progressService,
            analytics
        )
        action(context, query, listOf("hw_add_dict", "1", "1"))

        coVerify { dictionaryService.addFavorite(user.id, 10) }
    }
}
