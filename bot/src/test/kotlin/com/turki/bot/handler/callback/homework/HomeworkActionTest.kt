package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeworkActionTest {
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
    fun `missing lesson id returns`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 20L)

        val action = HomeworkAction(userService, analytics)
        action(context, query, listOf("homework"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `renders homework start`() = runBlocking {
        val userService = mockk<UserService>()
        val analytics = mockk<AnalyticsService>(relaxed = true)
        val query = mockCallbackQuery(telegramId = 21L)

        io.mockk.coEvery { userService.findByTelegramId(21L) } returns null

        val action = HomeworkAction(userService, analytics)
        action(context, query, listOf("homework", "1"))

        coVerify { context.editOrSendHtml(query, com.turki.bot.i18n.S.homeworkStart, any()) }
    }
}
