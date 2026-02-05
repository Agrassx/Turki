package com.turki.bot.handler.command.lesson

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

class HomeworkCommandTest {
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
        val telegramId = 11L
        val message = mockMessage("/homework", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = HomeworkCommand(userService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `registered user gets start prompt`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val telegramId = 12L
        val user = coreUser(id = 3L, telegramId = telegramId)
        val message = mockMessage("/homework", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user

        val action = HomeworkCommand(userService, analytics)
        action(context, message)

        coVerify { context.sendHtml(any(), S.homeworkStart, any()) }
    }
}
