package com.turki.bot.handler.command.dictionary

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryEntryView
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
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DictionaryCommandTest {
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
        val telegramId = 25L
        val message = mockMessage("/dictionary", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = DictionaryCommand(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `empty list shows empty state`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 26L
        val user = coreUser(id = 10L, telegramId = telegramId)
        val message = mockMessage("/dictionary", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.listUserDictionary(user.id) } returns emptyList()

        val action = DictionaryCommand(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.dictionaryEmpty, any()) }
    }

    @Test
    fun `non empty list renders dictionary`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 27L
        val user = coreUser(id = 11L, telegramId = telegramId)
        val message = mockMessage("/dictionary", mockTelegramUser(telegramId))
        val entry = DictionaryEntryView(
            word = "merhaba",
            translation = "hello",
            pronunciation = null,
            example = null,
            addedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.listUserDictionary(user.id) } returns listOf(entry)

        val action = DictionaryCommand(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains("merhaba") }, any()) }
    }

    @Test
    fun `query with no results shows no results`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 28L
        val user = coreUser(id = 12L, telegramId = telegramId)
        val message = mockMessage("/dictionary test", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.search("test", limit = 1) } returns emptyList()

        val action = DictionaryCommand(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.dictionaryNoResults) }
    }

    @Test
    fun `query with result shows card`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 29L
        val user = coreUser(id = 13L, telegramId = telegramId)
        val message = mockMessage("/dictionary merhaba", mockTelegramUser(telegramId))
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

        val action = DictionaryCommand(userService, dictionaryService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains("merhaba") }, any()) }
    }
}
