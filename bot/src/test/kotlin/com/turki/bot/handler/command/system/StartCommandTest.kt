package com.turki.bot.handler.command.system

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
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

class StartCommandTest {
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
    fun `new user logs registration and sends welcome`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 42L
        val tgUser = mockTelegramUser(telegramId = telegramId, firstName = "Test")
        val message = mockMessage("/start", tgUser)
        val user = coreUser(id = 1L, telegramId = telegramId, firstName = "Test")

        coEvery { userService.findByTelegramId(telegramId) } returns null
        coEvery { userService.findOrCreateUser(any(), any(), any(), any()) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = StartCommand(userService, analytics, userStateService)
        action(context, message)

        coVerify { analytics.log(EventNames.USER_REGISTERED, user.id) }
        coVerify { context.sendHtml(any(), any()) }
    }

    @Test
    fun `returning user logs return event`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 43L
        val tgUser = mockTelegramUser(telegramId = telegramId)
        val message = mockMessage("/start", tgUser)
        val user = coreUser(id = 2L, telegramId = telegramId)

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userService.findOrCreateUser(any(), any(), any(), any()) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = StartCommand(userService, analytics, userStateService)
        action(context, message)

        coVerify { analytics.log(EventNames.USER_RETURNED, user.id) }
        coVerify { context.sendHtml(any(), any()) }
    }
}
