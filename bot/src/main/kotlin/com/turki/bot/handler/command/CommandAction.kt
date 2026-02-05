package com.turki.bot.handler.command

import com.turki.bot.service.UserFlowState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

interface CommandAction {
    val command: String

    suspend operator fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>)
}

interface CommandTextAction {
    val state: UserFlowState

    suspend operator fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>)
}
