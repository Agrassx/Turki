package com.turki.bot.handler.callback.dictionary

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.decodeFromString

class DictTagAction(
    private val userService: UserService,
    private val dictionaryService: DictionaryService
) : CallbackAction {
    override val action: String = "dict_tag"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        if (parts.size < 3) {
            return
        }
        val vocabId = parts[1].toIntOrNull() ?: return
        val tag = parts[2]
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val entry = dictionaryService.getEntry(user.id, vocabId)
        val tags = entry?.tags?.let { CALLBACK_JSON.decodeFromString<List<String>>(it) } ?: emptyList()
        val nextTags = if (tags.contains(tag)) tags - tag else tags + tag
        dictionaryService.setTags(user.id, vocabId, nextTags)
        renderDictionaryList(context, query, user.id, 0, dictionaryService)
    }
}
