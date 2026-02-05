package com.turki.bot.handler.homework

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

interface HomeworkTextAction {
    val action: String

    suspend operator fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>)
}
