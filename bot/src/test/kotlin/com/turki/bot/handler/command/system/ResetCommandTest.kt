package com.turki.bot.handler.command.system

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
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

class ResetCommandTest {
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
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 70L
        val message = mockMessage("/reset", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = ResetCommand(userService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `registered user gets confirm`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 71L
        val user = coreUser(id = 50L, telegramId = telegramId)
        val message = mockMessage("/reset", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user

        val action = ResetCommand(userService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.resetProgressConfirm, any()) }
    }
}
