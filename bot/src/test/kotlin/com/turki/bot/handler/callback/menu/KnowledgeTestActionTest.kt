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

class KnowledgeTestActionTest {
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
    fun `renders knowledge test`() = runBlocking {
        val query = mockCallbackQuery(telegramId = 150L)

        val action = KnowledgeTestAction()
        action(context, query, listOf("knowledge_test"))

        coVerify { context.editOrSendHtml(query, S.knowledgeTestTitle, any()) }
    }
}
