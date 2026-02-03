package com.turki.bot.util

import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.HTMLParseMode

/**
 * Converts various Telegram API types to [ChatIdentifier].
 *
 * This extension function provides a unified way to extract a chat identifier
 * from different Telegram API objects that represent a chat or user.
 *
 * @return [ChatIdentifier] extracted from the object
 * @throws IllegalArgumentException if the object type is not supported
 *
 * @sample
 * ```
 * val chatId = message.chat.toChatId()
 * val userId = user.toChatId()
 * val identifier = chatIdentifier.toChatId()
 * ```
 */
fun Any.toChatId(): ChatIdentifier = when (this) {
    is ChatIdentifier -> this
    is Chat -> this.id
    is User -> this.id
    is IdChatIdentifier -> this
    else -> throw IllegalArgumentException("Cannot convert $this to ChatIdentifier")
}

/**
 * Converts Markdown syntax to HTML format compatible with Telegram.
 *
 * This function processes various Markdown elements and converts them to HTML tags
 * that Telegram's HTML parse mode supports. The conversion handles:
 * - Headers (#, ##, ###)
 * - Bold text (**text**)
 * - Italic text (*text*)
 * - Markdown tables
 * - Unordered lists (- item)
 *
 * **Regular Expression Patterns:**
 *
 * 1. **Table separator removal**: `^\\|[-: ]+\\|$`
 *    - Matches markdown table separator rows (e.g., `|---|---|`)
 *    - Example: `|----|----|` → removed
 *
 * 2. **Table row conversion**: `^\\|(.+)\\|$`
 *    - Matches markdown table rows and converts to plain text
 *    - Example: `| Cell1 | Cell2 |` → `Cell1 | Cell2`
 *
 * 3. **Header conversion**: `^#{1,3}\\s+(.+)$`
 *    - Matches markdown headers (H1, H2, H3) and converts to bold
 *    - Example: `# Title` → `<b>Title</b>`
 *    - Example: `## Subtitle` → `<b>Subtitle</b>`
 *    - Example: `### Section` → `<b>Section</b>`
 *
 * 4. **Bold text**: `\\*\\*([^*]+?)\\*\\*`
 *    - Matches double asterisks for bold text (non-greedy)
 *    - Example: `**bold text**` → `<b>bold text</b>`
 *    - Example: `**Hello** world` → `<b>Hello</b> world`
 *
 * 5. **Italic text**: `(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)`
 *    - Matches single asterisks for italic (not part of bold)
 *    - Uses negative lookbehind/lookahead to avoid matching `**`
 *    - Example: `*italic text*` → `<i>italic text</i>`
 *    - Example: `*Hello* world` → `<i>Hello</i> world`
 *    - Does NOT match: `**bold**` (handled by pattern 4)
 *
 * 6. **Unordered list**: `^\\s*-\\s+(.+)$`
 *    - Matches markdown list items and converts to bullet points
 *    - Example: `- Item 1` → `• Item 1`
 *    - Example: `  - Indented item` → `• Indented item`
 *
 * @return HTML-formatted string compatible with Telegram's HTML parse mode
 *
 * @sample
 * ```
 * val markdown = """
 * # Title
 * **Bold text** and *italic text*
 * - List item 1
 * - List item 2
 * """
 * val html = markdown.markdownToHtml()
 * // Result: "<b>Title</b>\n<b>Bold text</b> and <i>italic text</i>\n• List item 1\n• List item 2"
 * ```
 */
fun String.markdownToHtml(): String {
    var result = this

    result = result.replace(Regex("^\\|[-: ]+\\|$", RegexOption.MULTILINE), "")

    result = result.replace(Regex("^\\|(.+)\\|$", RegexOption.MULTILINE)) { matchResult ->
        val cells = matchResult.groupValues[1].split("|").map { it.trim() }.filter { it.isNotEmpty() }
        cells.joinToString(" | ")
    }

    result = result.replace(Regex("^#{1,3}\\s+(.+)$", RegexOption.MULTILINE)) { matchResult ->
        "<b>${matchResult.groupValues[1].trim()}</b>"
    }

    result = result.replace(Regex("\\*\\*([^*]+?)\\*\\*")) { matchResult ->
        "<b>${matchResult.groupValues[1]}</b>"
    }

    result = result.replace(Regex("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)")) { matchResult ->
        "<i>${matchResult.groupValues[1]}</i>"
    }

    result = result.replace(Regex("^\\s*-\\s+(.+)$", RegexOption.MULTILINE)) { matchResult ->
        "• ${matchResult.groupValues[1]}"
    }

    return result
}

/**
 * Sends a message with HTML formatting to a Telegram chat.
 *
 * This extension function provides a convenient way to send HTML-formatted messages
 * through the Telegram Bot API. The message is automatically parsed as HTML,
 * allowing the use of HTML tags like `<b>`, `<i>`, `<code>`, etc.
 *
 * @param chat The chat or user to send the message to. Can be [Chat], [User],
 *              [ChatIdentifier], or [IdChatIdentifier]
 * @param text The message text with HTML formatting
 * @param replyMarkup Optional reply markup for interactive buttons
 *
 * @sample
 * ```
 * context.sendHtml(
 *     chat = message.chat,
 *     text = "<b>Hello</b> <i>world</i>!",
 *     replyMarkup = InlineKeyboardMarkup(listOf(...))
 * )
 * ```
 */
suspend fun BehaviourContext.sendHtml(
    chat: Any,
    text: String,
    replyMarkup: KeyboardMarkup? = null
) {
    val markup = replyMarkup ?: mainCommandKeyboard()
    sendMessage(
        chatId = chat.toChatId(),
        text = text,
        parseMode = HTMLParseMode,
        replyMarkup = markup
    )
}

fun mainCommandKeyboard(): ReplyKeyboardMarkup = ReplyKeyboardMarkup(
    keyboard = listOf(
        listOf(
            SimpleKeyboardButton("/start"),
            SimpleKeyboardButton("/menu"),
            SimpleKeyboardButton("/help")
        )
    ),
    resizeKeyboard = true,
    oneTimeKeyboard = false,
    selective = false,
    persistent = true
)
