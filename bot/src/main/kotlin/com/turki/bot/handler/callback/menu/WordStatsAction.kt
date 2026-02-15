package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.i18n.S
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserService
import com.turki.bot.util.editOrSendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

private const val MAX_ITEMS_PER_TIER = 10
private const val WELL_KNOWN_THRESHOLD = 80
private const val MEDIUM_THRESHOLD = 40

class WordStatsAction(
    private val userService: UserService,
    private val reviewService: ReviewService,
    private val lessonService: LessonService
) : CallbackAction {
    override val action: String = "word_stats"

    @Suppress("LongMethod")
    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val cards = reviewService.getAllCards(user.id)
            .filter { it.totalAttempts > 0 }

        if (cards.isEmpty()) {
            context.editOrSendHtml(
                query,
                "${S.wordStatsTitle}\n\n${S.wordStatsEmpty}",
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        // Resolve vocabulary words
        data class WordStat(val word: String, val translation: String, val accuracy: Int)

        val stats = cards.mapNotNull { card ->
            val vocab = lessonService.findVocabularyById(card.vocabularyId) ?: return@mapNotNull null
            WordStat(vocab.word, vocab.translation, card.accuracyPercent ?: 0)
        }

        val wellKnown = stats.filter { it.accuracy >= WELL_KNOWN_THRESHOLD }.sortedByDescending { it.accuracy }
        val medium = stats.filter { it.accuracy in MEDIUM_THRESHOLD until WELL_KNOWN_THRESHOLD }
            .sortedByDescending { it.accuracy }
        val poor = stats.filter { it.accuracy < MEDIUM_THRESHOLD }.sortedBy { it.accuracy }

        val text = buildString {
            appendLine(S.wordStatsTitle)
            appendLine()

            if (poor.isNotEmpty()) {
                appendLine(S.wordStatsPoorlyKnown)
                poor.take(MAX_ITEMS_PER_TIER).forEach {
                    appendLine(S.wordStatsItem(it.word, it.translation, it.accuracy))
                }
                if (poor.size > MAX_ITEMS_PER_TIER) {
                    appendLine("  <i>...и ещё ${poor.size - MAX_ITEMS_PER_TIER}</i>")
                }
                appendLine()
            }

            if (medium.isNotEmpty()) {
                appendLine(S.wordStatsMedium)
                medium.take(MAX_ITEMS_PER_TIER).forEach {
                    appendLine(S.wordStatsItem(it.word, it.translation, it.accuracy))
                }
                if (medium.size > MAX_ITEMS_PER_TIER) {
                    appendLine("  <i>...и ещё ${medium.size - MAX_ITEMS_PER_TIER}</i>")
                }
                appendLine()
            }

            if (wellKnown.isNotEmpty()) {
                appendLine(S.wordStatsWellKnown)
                wellKnown.take(MAX_ITEMS_PER_TIER).forEach {
                    appendLine(S.wordStatsItem(it.word, it.translation, it.accuracy))
                }
                if (wellKnown.size > MAX_ITEMS_PER_TIER) {
                    appendLine("  <i>...и ещё ${wellKnown.size - MAX_ITEMS_PER_TIER}</i>")
                }
                appendLine()
            }

            append(S.wordStatsSummary(stats.size, wellKnown.size, medium.size, poor.size))
        }

        context.editOrSendHtml(
            query,
            text,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }
}
