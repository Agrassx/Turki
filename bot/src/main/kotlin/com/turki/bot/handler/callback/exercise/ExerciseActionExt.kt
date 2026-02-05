package com.turki.bot.handler.callback.exercise

import com.turki.bot.handler.callback.CALLBACK_JSON
import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.EventNames
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

internal suspend fun startLessonPractice(
    context: BehaviourContext,
    query: DataCallbackQuery,
    lessonId: Int,
    lessonService: LessonService,
    exerciseService: ExerciseService,
    userService: UserService,
    userStateService: UserStateService,
    analyticsService: AnalyticsService
) {
    val lesson = lessonService.getLessonById(lessonId) ?: run {
        context.sendHtml(query.from, S.lessonNotFound)
        return
    }
    val exercises = exerciseService.buildLessonExercises(lesson.id)
    if (exercises.isEmpty()) {
        context.editOrSendHtml(
            query,
            S.exerciseNotReady,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        return
    }

    val payload = ExerciseFlowPayload(
        lessonId = lesson.id,
        exerciseIndex = 0,
        vocabularyIds = exercises.map { it.vocabularyId },
        optionsByVocabId = exercises.associate { it.vocabularyId to it.options },
        correctByVocabId = exercises.associate { it.vocabularyId to it.correctOption },
        explanationsByVocabId = exercises.associate { it.vocabularyId to it.explanation }
    )
    val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
    userStateService.set(user.id, UserFlowState.EXERCISE.name, CALLBACK_JSON.encodeToString(payload))
    sendExercise(context, query, payload, lessonService, userService, analyticsService)
}

internal suspend fun advanceExercise(
    context: BehaviourContext,
    query: DataCallbackQuery,
    userService: UserService,
    userStateService: UserStateService,
    progressService: ProgressService,
    lessonService: LessonService,
    analyticsService: AnalyticsService
) {
    val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
    val state = userStateService.get(user.id) ?: return
    if (state.state != UserFlowState.EXERCISE.name) {
        return
    }
    val payload = CALLBACK_JSON.decodeFromString<ExerciseFlowPayload>(state.payload)
    val nextIndex = payload.exerciseIndex + 1
    if (nextIndex >= payload.vocabularyIds.size) {
        userStateService.clear(user.id)
        progressService.recordPractice(user.id)
        context.editOrSendHtml(
            query,
            S.exerciseComplete,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        return
    }

    val nextPayload = payload.copy(exerciseIndex = nextIndex)
    userStateService.set(user.id, UserFlowState.EXERCISE.name, CALLBACK_JSON.encodeToString(nextPayload))
    sendExercise(context, query, nextPayload, lessonService, userService, analyticsService)
}

internal suspend fun sendExercise(
    context: BehaviourContext,
    query: DataCallbackQuery,
    payload: ExerciseFlowPayload,
    lessonService: LessonService,
    userService: UserService,
    analyticsService: AnalyticsService
) {
    val vocabId = payload.vocabularyIds.getOrNull(payload.exerciseIndex) ?: return
    val vocab = lessonService.findVocabularyById(vocabId) ?: return
    val options = payload.optionsByVocabId[vocabId] ?: return

    val buttons = options.mapIndexed { index, option ->
        listOf(dataInlineButton(option, "exercise_answer:${payload.lessonId}:$vocabId:$index"))
    } + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))

    analyticsService.log(
        EventNames.PRACTICE_STARTED,
        userService.findByTelegramId(query.from.id.chatId.long)?.id ?: return,
        props = mapOf("type" to "vocab_mcq")
    )

    context.editOrSendHtml(
        query,
        S.exercisePrompt(vocab.word),
        replyMarkup = InlineKeyboardMarkup(buttons)
    )
}
