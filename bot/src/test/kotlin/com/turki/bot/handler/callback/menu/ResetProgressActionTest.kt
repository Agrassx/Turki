package com.turki.bot.handler.callback.menu

import com.turki.bot.util.editOrSendHtml

import com.turki.bot.handler.mockCallbackQuery
import com.turki.bot.handler.stubEditAndSendHtml
import com.turki.bot.i18n.S
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ResetProgressActionTest {
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
    fun `renders confirm reset`() = runBlocking {
        val query = mockCallbackQuery(telegramId = 100L)

        val action = ResetProgressAction()
        action(context, query, listOf("reset_progress"))

        coVerify { context.editOrSendHtml(query, S.resetProgressConfirm, any()) }
    }
}
