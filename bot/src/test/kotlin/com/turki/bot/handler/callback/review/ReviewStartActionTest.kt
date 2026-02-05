package com.turki.bot.handler.callback.review

import com.turki.bot.handler.coreUser
import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
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

class ReviewStartActionTest {
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
        val query = mockCallbackQuery(telegramId = 80L)

        coEvery { userService.findByTelegramId(80L) } returns null

        val action = ReviewStartAction(userService)
        action(context, query, listOf("review_start"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `shows difficulty selection`() = runBlocking {
        val userService = mockk<UserService>()
        val query = mockCallbackQuery(telegramId = 81L)
        val user = coreUser(id = 1L, telegramId = 81L)

        coEvery { userService.findByTelegramId(81L) } returns user

        val action = ReviewStartAction(userService)
        action(context, query, listOf("review_start"))

        coVerify { context.editOrSendHtml(query, S.reviewSelectDifficulty, any()) }
    }
}
