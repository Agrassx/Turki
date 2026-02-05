package com.turki.bot.handler

import com.turki.bot.handler.homework.HomeworkTextAction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.slf4j.LoggerFactory

class HomeworkHandler(actions: List<HomeworkTextAction>) {
    private val logger = LoggerFactory.getLogger("HomeworkHandler")
    private val actionMap = actions.associateBy { it.action }

    suspend fun handleTextAnswer(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val handler = actionMap["text_answer"]
        if (handler != null) {
            handler(context, message)
        } else {
            logger.warn("No homework action registered for text answers")
        }
    }
}
