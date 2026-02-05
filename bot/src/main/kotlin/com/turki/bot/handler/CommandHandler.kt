package com.turki.bot.handler

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.handler.command.CommandTextAction
import com.turki.bot.service.UserFlowState
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import org.slf4j.LoggerFactory

class CommandHandler(
    actions: List<CommandAction>,
    textActions: List<CommandTextAction>
) {
    private val logger = LoggerFactory.getLogger("CommandHandler")
    private val actionMap = actions.associateBy { it.command }
    private val textActionMap = textActions.associateBy { it.state }

    suspend fun handleCommand(command: String, context: BehaviourContext, message: CommonMessage<TextContent>) {
        val handler = actionMap[command]
        if (handler != null) {
            handler(context, message)
        } else {
            logger.warn("Unknown command: '$command'")
        }
    }

    suspend fun handleTextState(state: UserFlowState, context: BehaviourContext, message: CommonMessage<TextContent>) {
        val handler = textActionMap[state]
        if (handler != null) {
            handler(context, message)
        } else {
            logger.warn("No text handler for state '${state.name}'")
        }
    }
}
