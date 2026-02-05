package com.turki.bot.handler

import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import com.turki.core.domain.User
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BusinessChatId
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.business_connection.BusinessConnectionId
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.chat.User as TgUser
import dev.inmo.tgbotapi.types.chat.CommonUser
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant

fun stubSendHtml(context: BehaviourContext) {
    coEvery { context.sendHtml(any<Any>(), any<String>()) } returns mockk(relaxed = true)
    coEvery { context.sendHtml(any<Any>(), any<String>(), any()) } returns mockk(relaxed = true)
}

fun mockMessage(messageText: String, from: TgUser): CommonMessage<TextContent> {
    val message = mockk<CommonMessage<TextContent>>(
        relaxed = true,
        moreInterfaces = arrayOf(OptionallyFromUserMessage::class)
    )
    val content = mockk<TextContent> { every { text } returns messageText }
    every { message.content } returns content
    val chat = mockk<PreviewChat>(relaxed = true)
    val rawId = RawChatId(from.id.chatId.long)
    every { chat.id } returns BusinessChatId(rawId, BusinessConnectionId("test"))
    every { message.chat } returns chat
    every { message.from } returns from
    every { (message as OptionallyFromUserMessage).from } returns from
    return message
}

fun mockTelegramUser(telegramId: Long, firstName: String = "Test"): TgUser {
    return CommonUser(
        ChatId(RawChatId(telegramId)),
        firstName,
        "User",
        null,
        null,
        false,
        false
    )
}

fun coreUser(
    id: Long,
    telegramId: Long,
    firstName: String = "Test",
    subscriptionActive: Boolean = false,
    currentLessonId: Int = 1
): User {
    return User(
        id = id,
        telegramId = telegramId,
        username = null,
        firstName = firstName,
        lastName = null,
        language = Language.RUSSIAN,
        subscriptionActive = subscriptionActive,
        subscriptionExpiresAt = null,
        currentLessonId = currentLessonId,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0)
    )
}
