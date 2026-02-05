package com.turki.bot.handler.callback.menu

import com.turki.bot.util.editOrSendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
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

class BackToMenuActionTest {
    private val context = mockk<BehaviourContext>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic("com.turki.bot.util.BotExtensionsKt")
        stubEditAndSendHtml(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `user missing returns`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 50L)

        coEvery { userService.findByTelegramId(50L) } returns null

        val action = BackToMenuAction(userService, userStateService)
        action(context, query, listOf("back_to_menu"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `user exists renders menu`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 51L)
        val user = coreUser(id = 1L, telegramId = 51L)

        coEvery { userService.findByTelegramId(51L) } returns user
        coEvery { userStateService.get(user.id) } returns null

        val action = BackToMenuAction(userService, userStateService)
        action(context, query, listOf("back_to_menu"))

        coVerify { context.editOrSendHtml(query, com.turki.bot.i18n.S.menuTitle, any()) }
    }
}
