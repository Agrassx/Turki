package com.turki.bot.handler.callback.vocabulary

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class VocabWordAction(
    private val lessonService: LessonService,
    private val userService: UserService,
    private val dictionaryService: DictionaryService
) : CallbackAction {
    override val action: String = "vocab_word"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val lessonId = parts.getOrNull(1)?.toIntOrNull()
        val vocabId = parts.getOrNull(2)?.toIntOrNull()
        renderVocabularyWord(context, query, lessonId, vocabId, lessonService, userService, dictionaryService)
    }
}
