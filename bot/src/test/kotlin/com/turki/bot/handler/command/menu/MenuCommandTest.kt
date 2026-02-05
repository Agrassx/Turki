package com.turki.bot.handler.command.menu

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockMessage
import com.turki.bot.handler.mockTelegramUser
import com.turki.bot.handler.stubSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.core.domain.UserState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MenuCommandTest {
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
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 18L
        val message = mockMessage("/menu", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns null

        val action = MenuCommand(userService, analytics, userStateService)
        action(context, message)

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `menu without state omits continue button`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 19L
        val user = coreUser(id = 7L, telegramId = telegramId)
        val message = mockMessage("/menu", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = MenuCommand(userService, analytics, userStateService)
        action(context, message)

        coVerify {
            context.sendHtml(any(), S.menuTitle, match { markup ->
                val buttons = (markup as InlineKeyboardMarkup).keyboard.flatten()
                buttons.none { it.text == com.turki.bot.i18n.S.btnContinue }
            })
        }
    }

    @Test
    fun `menu with state includes continue button`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val userStateService = mockk<UserStateService>(relaxed = true)
        val telegramId = 20L
        val user = coreUser(id = 8L, telegramId = telegramId)
        val message = mockMessage("/menu", mockTelegramUser(telegramId))

        coEvery { userService.findByTelegramId(telegramId) } returns user
        coEvery { userStateService.get(user.id) } returns UserState(
            userId = user.id,
            state = "state",
            payload = "{}",
            updatedAt = kotlinx.datetime.Instant.fromEpochSeconds(0)
        )

        val action = MenuCommand(userService, analytics, userStateService)
        action(context, message)

        coVerify {
            context.sendHtml(any(), S.menuTitle, match { markup ->
                val buttons = (markup as InlineKeyboardMarkup).keyboard.flatten()
                buttons.any { it.text == com.turki.bot.i18n.S.btnContinue }
            })
        }
    }
}
