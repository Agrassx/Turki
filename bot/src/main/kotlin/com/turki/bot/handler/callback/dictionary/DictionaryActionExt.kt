package com.turki.bot.handler.callback.dictionary

import com.turki.bot.i18n.S
import com.turki.bot.service.DictionaryService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

internal suspend fun renderDictionaryList(
    context: BehaviourContext,
    query: DataCallbackQuery,
    userId: Long,
    page: Int,
    dictionaryService: DictionaryService
) {
    val entries = dictionaryService.listUserDictionary(userId)
    val perPage = 15
    val totalPages = (entries.size + perPage - 1) / perPage
    val safePage = page.coerceIn(0, maxOf(totalPages - 1, 0))
    val pageEntries = entries.drop(safePage * perPage).take(perPage)

    if (pageEntries.isEmpty()) {
        context.editOrSendHtml(
            query,
            S.dictionaryEmpty,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
        return
    }

    val text = buildString {
        appendLine(S.vocabularyTitle)
        appendLine()
        pageEntries.forEach { entry ->
            appendLine(S.vocabularyItem(entry.word, entry.translation))
            entry.pronunciation?.let { appendLine(S.dictionaryPronunciation(it)) }
            entry.example?.let { appendLine(S.dictionaryExample(it)) }
            appendLine()
        }
        if (totalPages > 1) {
            appendLine("Страница ${safePage + 1}/$totalPages")
        }
    }

    val buttons = buildList {
        if (totalPages > 1) {
            val nav = buildList {
                if (safePage > 0) add(dataInlineButton("◀️", "dict_list:${safePage - 1}"))
                if (safePage < totalPages - 1) add(dataInlineButton("▶️", "dict_list:${safePage + 1}"))
            }
            if (nav.isNotEmpty()) {
                add(nav)
            }
        }
        add(listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom")))
        add(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
    }

    context.editOrSendHtml(
        query,
        text,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}
