package com.turki.bot.handler.command.lesson

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
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

class VocabularyCommandTest {
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
        val telegramId = 16L
        val message = mockMessage("/vocabulary", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = VocabularyCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `empty vocabulary shows empty text`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 17L
        val user = coreUser(id = 6L, telegramId = telegramId)
        val message = mockMessage("/vocabulary", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getVocabulary(user.currentLessonId) } returns emptyList()

        val action = VocabularyCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.vocabularyEmpty) }
    }

    @Test
    fun `non empty vocabulary sends list`() = runBlocking {
        val userService = mockk<UserService>()
        val lessonService = mockk<LessonService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 18L
        val user = coreUser(id = 7L, telegramId = telegramId)
        val message = mockMessage("/vocabulary", mockTelegramUser(telegramId))
        val vocab = listOf(
            VocabularyItem(
                id = 1,
                lessonId = user.currentLessonId,
                word = "merhaba",
                translation = "hello",
                pronunciation = null,
                example = null
            )
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { lessonService.getVocabulary(user.currentLessonId) } returns vocab

        val action = VocabularyCommand(userService, lessonService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains("merhaba") }) }
    }
}
