package com.turki.bot.handler.command.dictionary

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
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

class DictionaryQueryTextActionTest {
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
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 40L
        val message = mockMessage("query", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = DictionaryQueryTextAction(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `query with no results sends empty state`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 41L
        val user = coreUser(id = 20L, telegramId = telegramId)
        val message = mockMessage("test", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.search("test", limit = 1) } returns emptyList()

        val action = DictionaryQueryTextAction(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.dictionaryNoResults) }
    }

    @Test
    fun `query with result sends card`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 42L
        val user = coreUser(id = 21L, telegramId = telegramId)
        val message = mockMessage("merhaba", mockTelegramUser(telegramId))
        val vocab = VocabularyItem(
            id = 1,
            lessonId = 1,
            word = "merhaba",
            translation = "hello",
            pronunciation = null,
            example = null
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.search("merhaba", limit = 1) } returns listOf(vocab)
        coEvery { dictionaryService.getEntry(user.id, vocab.id) } returns null

        val action = DictionaryQueryTextAction(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains("merhaba") }, any()) }
    }
}
