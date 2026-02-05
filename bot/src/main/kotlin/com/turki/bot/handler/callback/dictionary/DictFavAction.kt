package com.turki.bot.handler.callback.dictionary

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.UserService
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class DictFavAction(
    private val userService: UserService,
    private val dictionaryService: DictionaryService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "dict_fav"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val vocabId = parts.getOrNull(1)?.toIntOrNull() ?: return
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        dictionaryService.toggleFavorite(user.id, vocabId)
        analyticsService.log(EventNames.WORD_ADDED_TO_DICTIONARY, user.id, props = mapOf("itemId" to vocabId.toString()))
        renderDictionaryList(context, query, user.id, 0, dictionaryService)
    }
}
