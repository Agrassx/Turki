package com.turki.bot.handler

import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BusinessChatId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.business_connection.BusinessConnectionId
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

fun stubEditAndSendHtml(context: BehaviourContext) {
    coEvery { context.editOrSendHtml(any(), any<String>(), any()) } returns Unit
    coEvery { context.replaceWithHtml(any(), any<String>(), any()) } returns mockk(relaxed = true)
    coEvery { context.sendHtml(any<Any>(), any<String>()) } returns mockk(relaxed = true)
    coEvery { context.sendHtml(any<Any>(), any<String>(), any()) } returns mockk(relaxed = true)
}

fun mockCallbackQuery(telegramId: Long, messageText: String = "msg"): DataCallbackQuery {
    val from = CommonUser(
        ChatId(RawChatId(telegramId)),
        "Test",
        "User",
        null,
        null,
        false,
        false
    )
    val chat = mockk<PreviewChat>(relaxed = true)
    every { chat.id } returns BusinessChatId(RawChatId(telegramId), BusinessConnectionId("test"))

    val message = mockk<CommonMessage<TextContent>>(
        relaxed = true,
        moreInterfaces = arrayOf(OptionallyFromUserMessage::class)
    )
    val content = mockk<TextContent> { every { text } returns messageText }
    every { message.content } returns content
    every { message.chat } returns chat
    every { message.from } returns from
    every { (message as OptionallyFromUserMessage).from } returns from
    every { message.messageId } returns MessageId(1L)

    val query = mockk<MessageDataCallbackQuery>(relaxed = true)
    every { query.from } returns from
    every { query.message } returns message
    return query
}
