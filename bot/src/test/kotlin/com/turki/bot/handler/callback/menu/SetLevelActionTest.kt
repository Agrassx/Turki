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

class SetLevelActionTest {
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
    fun `missing level returns`() = runBlocking {
        val query = mockCallbackQuery(telegramId = 120L)

        val action = SetLevelAction()
        action(context, query, listOf("set_level"))

        coVerify(exactly = 0) { context.editOrSendHtml(any(), any<String>(), any()) }
    }

    @Test
    fun `a1 level shows active message`() = runBlocking {
        val query = mockCallbackQuery(telegramId = 121L)

        val action = SetLevelAction()
        action(context, query, listOf("set_level", "A1"))

        coVerify { context.editOrSendHtml(query, S.levelA1Active, any()) }
    }

    @Test
    fun `locked level shows locked message`() = runBlocking {
        val query = mockCallbackQuery(telegramId = 122L)

        val action = SetLevelAction()
        action(context, query, listOf("set_level", "B1"))

        coVerify { context.editOrSendHtml(query, S.levelLocked("B1"), any()) }
    }
}
