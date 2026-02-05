package com.turki.bot.handler.command.menu

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.core.domain.EventNames
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

class HelpCommandTest {
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
    fun `help always sends text even if user missing`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 15L
        val message = mockMessage("/help", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = HelpCommand(userService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.help) }
    }

    @Test
    fun `help logs command when user exists`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 16L
        val user = coreUser(id = 10L, telegramId = telegramId)
        val message = mockMessage("/help", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user

        val action = HelpCommand(userService, analytics)
        action(context, message)

        coVerify { analytics.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "help")) }
        coVerify { context.sendHtml(any(), S.help) }
    }
}
