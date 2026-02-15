package com.turki.bot.handler.callback.menu

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.handler.callback.exercise.sendExercise
import com.turki.bot.handler.callback.learn.sendLearnQuestion
import com.turki.bot.handler.callback.review.sendReviewCard
import com.turki.bot.handler.callback.review.sendReviewSessionQuestion
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.LearnSessionPayload
import com.turki.bot.service.LessonService
import com.turki.bot.service.ReviewFlowPayload
import com.turki.bot.service.ReviewSessionPayload
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.sendHtml
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.decodeFromString



class ContinueAction(
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val lessonService: LessonService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "continue"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val state = userStateService.get(user.id)
        if (state == null) {
            context.editOrSendHtml(
                query,
                S.continueNothing,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        when (state.state) {
            UserFlowState.EXERCISE.name -> {
                val payload = CALLBACK_JSON.decodeFromString<ExerciseFlowPayload>(state.payload)
                sendExercise(context, query, payload, lessonService, userService, analyticsService)
            }
            UserFlowState.REVIEW.name -> {
                try {
                    val payload = CALLBACK_JSON.decodeFromString<ReviewSessionPayload>(state.payload)
                    sendReviewSessionQuestion(context, query, payload)
                } catch (e: Exception) {
                    menuLogger.debug("ContinueAction: fallback to legacy review payload: ${e.message}")
                    val payload = CALLBACK_JSON.decodeFromString<ReviewFlowPayload>(state.payload)
                    sendReviewCard(context, query, payload, lessonService)
                }
            }
            UserFlowState.LEARN_WORDS.name -> {
                val payload = CALLBACK_JSON.decodeFromString<LearnSessionPayload>(state.payload)
                sendLearnQuestion(context, query, payload)
            }
            UserFlowState.DICT_SEARCH.name -> {
                context.sendHtml(query.from, S.dictionaryPrompt)
            }
            UserFlowState.HOMEWORK_TEXT.name -> {
                context.sendHtml(query.from, S.homeworkContinue)
            }
            UserFlowState.SUPPORT_MESSAGE.name -> {
                context.sendHtml(query.from, S.supportPrompt)
            }
            else -> {
                userStateService.clear(user.id)
                renderMainMenu(context, query, user.id, user.currentLessonId, userStateService)
            }
        }
    }
}
