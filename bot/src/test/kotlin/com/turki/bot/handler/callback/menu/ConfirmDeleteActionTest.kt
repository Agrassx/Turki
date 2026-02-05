package com.turki.bot.handler.callback.menu

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.UserDataService
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

class ConfirmDeleteActionTest {
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
    fun `user missing sends not registered`() = runBlocking {
        val userService = mockk<UserService>()
        val userDataService = mockk<UserDataService>()
        val query = mockCallbackQuery(telegramId = 80L)

        coEvery { userService.findByTelegramId(80L) } returns null

        val action = ConfirmDeleteAction(userService, userDataService)
        action(context, query, listOf("confirm_delete"))

        coVerify { context.sendHtml(any(), S.notRegistered) }
    }

    @Test
    fun `registered user deletes data`() = runBlocking {
        val userService = mockk<UserService>()
        val userDataService = mockk<UserDataService>()
        val query = mockCallbackQuery(telegramId = 81L)
        val user = coreUser(id = 1L, telegramId = 81L)

        coEvery { userService.findByTelegramId(81L) } returns user
        coEvery { userDataService.deleteUserData(user.id) } returns Unit

        val action = ConfirmDeleteAction(userService, userDataService)
        action(context, query, listOf("confirm_delete"))

        coVerify { context.sendHtml(any(), S.deleteDataSuccess) }
    }
}
