package com.turki.bot.handler.callback.vocabulary

import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.LessonService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

internal suspend fun renderVocabularyList(
    context: BehaviourContext,
    query: DataCallbackQuery,
    lessonId: Int?,
    lessonService: LessonService,
    userService: UserService,
    dictionaryService: DictionaryService
) {
    if (lessonId == null) {
        return
    }

    val vocabulary = lessonService.getVocabulary(lessonId)
    if (vocabulary.isEmpty()) {
        context.editOrSendHtml(
            query,
            S.vocabularyEmpty,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        return
    }

    val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
    val favorites = dictionaryService.listFavoriteIds(user.id)

    val buttons = vocabulary.map { item ->
        val label = if (favorites.contains(item.id)) "⭐️ ${item.word}" else item.word
        listOf(dataInlineButton(label, "vocab_word:$lessonId:${item.id}"))
    }.toMutableList()

    buttons.add(listOf(dataInlineButton(S.btnAddAllToDictionary, "vocab_add_all:$lessonId")))
    buttons.add(listOf(dataInlineButton(S.btnBack, "lesson_start:$lessonId")))

    context.editOrSendHtml(
        query,
        S.vocabularyForLesson(lessonId),
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}

internal suspend fun renderVocabularyWord(
    context: BehaviourContext,
    query: DataCallbackQuery,
    lessonId: Int?,
    vocabId: Int?,
    lessonService: LessonService,
    userService: UserService,
    dictionaryService: DictionaryService
) {
    if (lessonId == null || vocabId == null) {
        return
    }
    val vocab = lessonService.findVocabularyById(vocabId) ?: return
    val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
    val entry = dictionaryService.getEntry(user.id, vocabId)
    val isFavorite = entry?.isFavorite == true

    val text = buildString {
        appendLine(S.dictionaryCardTitle(vocab.word, vocab.translation))
        vocab.pronunciation?.let { appendLine(S.dictionaryPronunciation(it)) }
        vocab.example?.let { appendLine(S.dictionaryExample(it)) }
    }

    val buttons = buildList {
        if (isFavorite) {
            add(listOf(dataInlineButton(S.btnRemoveFromDictionary, "vocab_remove:$lessonId:$vocabId")))
        } else {
            add(listOf(dataInlineButton(S.btnAddToDictionary, "vocab_add:$lessonId:$vocabId")))
        }
        add(listOf(dataInlineButton(S.btnBack, "vocab_list:$lessonId")))
    }

    context.editOrSendHtml(
        query,
        text,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}
