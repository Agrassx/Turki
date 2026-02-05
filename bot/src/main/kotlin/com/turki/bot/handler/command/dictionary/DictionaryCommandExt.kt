package com.turki.bot.handler.command.dictionary

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal val COMMAND_JSON: Json = Json { ignoreUnknownKeys = true }

internal suspend fun handleDictionaryQuery(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
    userId: Long,
    query: String,
    dictionaryService: DictionaryService,
    analyticsService: AnalyticsService
) {
    val results = dictionaryService.search(query, limit = 1)
    if (results.isEmpty()) {
        context.sendHtml(message.chat, S.dictionaryNoResults)
        return
    }

    val item = results.first()
    val entry = dictionaryService.getEntry(userId, item.id)
    val favLabel = if (entry?.isFavorite == true) "⭐️" else "☆"
    val tags = entry?.tags?.let { COMMAND_JSON.decodeFromString<List<String>>(it) } ?: emptyList()
    val tagsText = if (tags.isEmpty()) S.dictionaryTagsEmpty else tags.joinToString(", ")

    val card = buildString {
        appendLine(S.dictionaryCardTitle(item.word, item.translation))
        item.pronunciation?.let { appendLine(S.dictionaryPronunciation(it)) }
        item.example?.let { appendLine(S.dictionaryExample(it)) }
        appendLine(S.dictionaryTags(tagsText))
    }

    analyticsService.log(EventNames.DICTIONARY_SEARCH, userId, props = mapOf("queryLen" to query.length.toString()))

    context.sendHtml(
        message.chat,
        card,
        replyMarkup = InlineKeyboardMarkup(
            listOf(
                listOf(dataInlineButton(favLabel, "dict_fav:${item.id}")),
                listOf(dataInlineButton(S.btnEditTags, "dict_tags:${item.id}"))
            )
        )
    )
}

internal suspend fun sendDictionaryList(
    context: BehaviourContext,
    message: CommonMessage<TextContent>,
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
        context.sendHtml(
            message.chat,
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
            add(listOf(dataInlineButton("▶️", "dict_list:${safePage + 1}")))
        }
        add(listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom")))
        add(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
    }

    context.sendHtml(
        message.chat,
        text,
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}

internal fun parseCustomDictionaryInput(input: String): Pair<String, String>? {
    val separators = listOf(" - ", " — ", " : ", " , ")
    val sep = separators.firstOrNull { input.contains(it) } ?: return null
    val parts = input.split(sep, limit = 2)
    if (parts.size < 2) return null
    val word = parts[0].trim()
    val translation = parts[1].trim()
    if (word.isBlank() || translation.isBlank()) return null
    return word to translation
}
