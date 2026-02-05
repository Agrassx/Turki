package com.turki.bot.handler.command.system

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.Request
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExportCommandTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        mockkStatic("dev.inmo.tgbotapi.extensions.utils.extensions.raw.MessageKt")
        stubSendHtml(context)
        coEvery { context.execute(any<Request<*>>()) } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user not registered sends warning`() = runBlocking {
        val userService = mockk<UserService>()
        val userDataService = mockk<UserDataService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 90L
        val message = mockMessage("/export", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = ExportCommand(userService, userDataService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `registered user gets export document`() = runBlocking {
        val userService = mockk<UserService>()
        val userDataService = mockk<UserDataService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 91L
        val user = coreUser(id = 70L, telegramId = telegramId)
        val message = mockMessage("/export", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userDataService.exportUserData(user) } returns "{}"

        val action = ExportCommand(userService, userDataService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.exportDataPreparing) }
        coVerify { context.execute(any()) }
    }
}
