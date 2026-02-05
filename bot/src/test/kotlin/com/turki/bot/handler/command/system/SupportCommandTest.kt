package com.turki.bot.handler.command.system

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SupportCommandTest {
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
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 80L
        val message = mockMessage("/support", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = SupportCommand(userService, userStateService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `registered user gets prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 81L
        val user = coreUser(id = 60L, telegramId = telegramId)
        val message = mockMessage("/support", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userStateService.set(user.id, any(), any()) } returns mockk(relaxed = true)

        val action = SupportCommand(userService, userStateService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.supportPrompt) }
    }
}
