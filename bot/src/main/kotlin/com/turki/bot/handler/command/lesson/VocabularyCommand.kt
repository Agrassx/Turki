package com.turki.bot.handler.command.lesson

import com.turki.bot.handler.command.CommandAction
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from

class VocabularyCommand(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val analyticsService: AnalyticsService
) : CommandAction {
    override val command: String = "vocabulary"

    override suspend fun invoke(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "vocabulary"))

        val vocabulary = lessonService.getVocabulary(user.currentLessonId)

        if (vocabulary.isEmpty()) {
            context.sendHtml(message.chat, S.vocabularyEmpty)
            return
        }

        val vocabText = buildString {
            appendLine(S.vocabularyForLesson(user.currentLessonId))
            appendLine()
            vocabulary.forEach { item ->
                appendLine(S.vocabularyItem(item.word, item.translation))
                item.pronunciation?.let { appendLine(S.dictionaryPronunciation(it)) }
                item.example?.let { appendLine(S.dictionaryExample(it)) }
                appendLine()
            }
        }

        context.sendHtml(message.chat, vocabText)
    }
}
