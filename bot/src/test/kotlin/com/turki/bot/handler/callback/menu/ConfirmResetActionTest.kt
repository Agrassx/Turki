package com.turki.bot.handler.callback.menu

import com.turki.bot.util.sendHtml

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
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

class ConfirmResetActionTest {
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
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val dictionaryService = mockk<DictionaryService>()
        val reviewService = mockk<ReviewService>()
        val query = mockCallbackQuery(telegramId = 70L)

        coEvery { userService.findByTelegramId(70L) } returns null

        val action = ConfirmResetAction(userService, userStateService, progressService, dictionaryService, reviewService)
        action(context, query, listOf("confirm_reset"))

        coVerify(exactly = 0) { context.sendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `registered user resets data`() = runBlocking {
        val userService = mockk<UserService>()
        val userStateService = mockk<UserStateService>()
        val progressService = mockk<ProgressService>()
        val dictionaryService = mockk<DictionaryService>()
        val reviewService = mockk<ReviewService>()
        val query = mockCallbackQuery(telegramId = 71L)
        val user = coreUser(id = 1L, telegramId = 71L)

        coEvery { userService.findByTelegramId(71L) } returns user
        coEvery { userService.resetProgress(user.id) } returns true
        coEvery { userStateService.clear(user.id) } returns true
        coEvery { progressService.resetProgress(user.id) } returns Unit
        coEvery { dictionaryService.clearUser(user.id) } returns Unit
        coEvery { reviewService.clearUser(user.id) } returns Unit

        val action = ConfirmResetAction(userService, userStateService, progressService, dictionaryService, reviewService)
        action(context, query, listOf("confirm_reset"))

        coVerify { context.sendHtml(any(), S.progressResetSuccess, any()) }
    }
}
