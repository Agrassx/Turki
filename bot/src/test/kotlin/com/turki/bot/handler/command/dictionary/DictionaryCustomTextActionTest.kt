package com.turki.bot.handler.command.dictionary

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryEntryView
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DictionaryCustomTextActionTest {
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
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 50L
        val message = mockMessage("word - translation", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = DictionaryCustomTextAction(userService, dictionaryService, userStateService)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `invalid format repeats prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 51L
        val user = coreUser(id = 30L, telegramId = telegramId)
        val message = mockMessage("invalid", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)

        val action = DictionaryCustomTextAction(userService, dictionaryService, userStateService)
        action(context, message)

        coVerify { context.sendHtml(any(), S.dictionaryAddFormatError) }
    }

    @Test
    fun `valid format adds word and lists dictionary`() = runBlocking {
        val userService = mockk<UserService>()
        val dictionaryService = mockk<DictionaryService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 52L
        val user = coreUser(id = 31L, telegramId = telegramId)
        val message = mockMessage("merhaba - hello", mockTelegramUser(telegramId))
        val entry = DictionaryEntryView(
            word = "merhaba",
            translation = "hello",
            pronunciation = null,
            example = null,
            addedAt = Instant.fromEpochSeconds(0)
        )

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { dictionaryService.addCustomWord(user.id, "merhaba", "hello", any(), any()) } returns mockk(relaxed = true)
        coEvery { dictionaryService.listUserDictionary(user.id) } returns listOf(entry)

        val action = DictionaryCustomTextAction(userService, dictionaryService, userStateService)
        action(context, message)

        coVerify { context.sendHtml(any(), match { it.contains("merhaba") }, any()) }
    }
}
