package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
import com.turki.bot.service.UserDataService
import com.turki.bot.service.ExerciseFlowPayload
import com.turki.bot.service.ReviewFlowPayload
import com.turki.bot.util.editOrSendHtml
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.replaceWithHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import com.turki.core.domain.QuestionType
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CallbackHandler")

/**
 * Handler for Telegram bot inline callback queries.
 *
 * This class processes all inline button callbacks sent by users, including:
 * - Lesson navigation (lesson, next_lesson)
 * - Vocabulary display (vocabulary)
 * - Homework management (homework, start_homework, answer)
 * - Progress tracking (progress)
 * - Settings and configuration (settings, reset_progress, select_level)
 * - Reminders (set_reminder)
 * - Menu navigation (back_to_menu)
 *
 * Callback data format: `action:param1:param2:...`
 * Examples:
 * - `lesson:1` - Show lesson with ID 1
 * - `answer:5:10:2:Option` - Submit answer for homework 5, question 10, option index 2
 * - `vocabulary:3` - Show vocabulary for lesson 3
 *
 * All callbacks are processed asynchronously and send HTML-formatted responses.
 */
class CallbackHandler(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val homeworkService: HomeworkService,
    private val reminderService: ReminderService,
    private val userStateService: UserStateService,
    private val exerciseService: ExerciseService,
    private val progressService: ProgressService,
    private val dictionaryService: DictionaryService,
    private val reviewService: ReviewService,
    private val reminderPreferenceService: ReminderPreferenceService,
    private val analyticsService: AnalyticsService,
    private val userDataService: UserDataService
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    suspend fun handleCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val data = query.data
        val parts = data.split(":")
        val action = parts[0]

        context.answer(query)

        when (action) {
            "lesson" -> handleLessonCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "lesson_start" -> handleLessonStart(context, query, parts.getOrNull(1)?.toIntOrNull())
            "lesson_practice" -> handleLessonPractice(context, query, parts.getOrNull(1)?.toIntOrNull())
            "lessons_list" -> handleLessonsList(context, query, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            "vocabulary" -> handleVocabularyCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "vocab_list" -> handleVocabularyList(context, query, parts.getOrNull(1)?.toIntOrNull())
            "vocab_word" -> handleVocabularyWord(context, query, parts.getOrNull(1)?.toIntOrNull(), parts.getOrNull(2)?.toIntOrNull())
            "vocab_add_all" -> handleVocabularyAddAll(context, query, parts.getOrNull(1)?.toIntOrNull())
            "vocab_add" -> handleVocabularyAdd(context, query, parts.getOrNull(1)?.toIntOrNull(), parts.getOrNull(2)?.toIntOrNull())
            "vocab_remove" -> handleVocabularyRemove(context, query, parts.getOrNull(1)?.toIntOrNull(), parts.getOrNull(2)?.toIntOrNull())
            "homework" -> handleHomeworkCallback(context, query, parts.getOrNull(1)?.toIntOrNull())
            "start_homework" -> handleStartHomework(context, query, parts.getOrNull(1)?.toIntOrNull())
            "answer" -> handleAnswer(context, query, parts)
            "progress" -> handleProgressCallback(context, query)
            "next_lesson" -> handleNextLessonCallback(context, query)
            "set_reminder" -> handleSetReminder(context, query)
            "settings" -> handleSettings(context, query)
            "reset_progress" -> handleResetProgress(context, query)
            "confirm_reset" -> handleConfirmReset(context, query)
            "confirm_delete" -> handleConfirmDelete(context, query)
            "select_level" -> handleSelectLevel(context, query)
            "set_level" -> handleSetLevel(context, query, parts.getOrNull(1))
            "knowledge_test" -> handleKnowledgeTest(context, query)
            "back_to_menu" -> handleBackToMenu(context, query)
            "continue" -> handleContinue(context, query)
            "practice_start" -> handlePracticeStart(context, query)
            "exercise_answer" -> handleExerciseAnswer(context, query, parts)
            "exercise_add_dict" -> handleExerciseAddDictionary(context, query, parts.getOrNull(1)?.toIntOrNull())
            "exercise_next" -> handleExerciseNext(context, query)
            "dictionary_prompt" -> handleDictionaryPrompt(context, query)
            "dict_list" -> handleDictionaryList(context, query, parts.getOrNull(1)?.toIntOrNull() ?: 0)
            "dict_fav" -> handleDictionaryFavorite(context, query, parts.getOrNull(1)?.toIntOrNull())
            "dict_tags" -> handleDictionaryTags(context, query, parts.getOrNull(1)?.toIntOrNull())
            "dict_tag" -> handleDictionaryTagToggle(context, query, parts)
            "dict_add_custom" -> handleDictionaryAddCustom(context, query)
            "review_start" -> handleReviewStart(context, query)
            "review_answer" -> handleReviewAnswer(context, query, parts)
            "hw_next" -> handleHomeworkNext(context, query, parts)
            "hw_add_dict" -> handleHomeworkAddDictionary(context, query, parts)
            "reminders" -> handleReminders(context, query)
            "reminder_enable_weekdays" -> handleReminderEnableWeekdays(context, query)
            "reminder_disable" -> handleReminderDisable(context, query)
            "help" -> handleHelp(context, query)
            "next_homework" -> handleNextHomework(context, query, parts.getOrNull(1)?.toIntOrNull())
            else -> logger.warn("Unknown callback action: '$action', data: '$data'")
        }
    }

    private suspend fun handleLessonCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            logger.warn("handleLessonCallback: lessonId is null")
            return
        }

        val lesson = lessonService.getLessonById(lessonId) ?: run {
            logger.warn("handleLessonCallback: lesson not found for id=$lessonId")
            context.sendHtml(query.from, S.lessonNotFound)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(lesson.orderIndex, lesson.title))
            appendLine()
            appendLine(lesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(lesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}")),
                    listOf(dataInlineButton(S.btnSetReminder, "set_reminder"))
                )
            )
        )
    }

    private suspend fun handleLessonsList(context: BehaviourContext, query: DataCallbackQuery, page: Int) {
        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
        val user = userService.findByTelegramId(query.from.id.chatId.long)
        val completed = if (user != null) progressService.getCompletedLessonIds(user.id) else emptySet()
        val perPage = 5
        val totalPages = (lessons.size + perPage - 1) / perPage
        val safePage = page.coerceIn(0, maxOf(totalPages - 1, 0))
        val pageLessons = lessons.drop(safePage * perPage).take(perPage)

        val buttons = pageLessons.map { lesson ->
            val topic = extractLessonTopic(lesson.title)
            val label = if (completed.contains(lesson.id)) {
                "${S.btnLesson} ${lesson.orderIndex} · $topic ✅"
            } else {
                "${S.btnLesson} ${lesson.orderIndex} · $topic"
            }
            listOf(dataInlineButton(label, "lesson_start:${lesson.id}"))
        }.toMutableList()

        if (totalPages > 1) {
            val nav = buildList {
                if (safePage > 0) add(dataInlineButton("◀️", "lessons_list:${safePage - 1}"))
                if (safePage < totalPages - 1) add(dataInlineButton("▶️", "lessons_list:${safePage + 1}"))
            }
            if (nav.isNotEmpty()) {
                buttons.add(nav)
            }
        }

        // Edit in place for smooth pagination
        context.editOrSendHtml(
            query,
            if (totalPages > 1) "${S.lessonsTitle}\n\nСтраница ${safePage + 1}/$totalPages" else S.lessonsTitle,
            replyMarkup = InlineKeyboardMarkup(
                buttons + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        if (user != null) {
            analyticsService.log("lesson_list_opened", user.id)
        }
    }

    @Suppress("MagicNumber")
    private fun extractLessonTopic(title: String): String {
        // Take text before colon or first 25 chars, whichever is shorter
        val beforeColon = title.substringBefore(":").trim()
        return if (beforeColon.length <= 25) {
            beforeColon
        } else {
            beforeColon.take(22) + "..."
        }
    }

    private suspend fun handleLessonStart(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        val telegramId = query.from.id.chatId.long
        if (lessonId == null) {
            logger.warn("handleLessonStart: lessonId is null")
            return
        }
        val lesson = lessonService.getLessonById(lessonId) ?: run {
            logger.warn("handleLessonStart: lesson not found for id=$lessonId")
            context.sendHtml(query.from, S.lessonNotFound)
            return
        }

        val introText = buildString {
            appendLine(S.lessonIntroTitle(lesson.orderIndex, lesson.title))
            appendLine()
            appendLine(lesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(lesson.content.markdownToHtml())
        }

        // Replace menu message with lesson content
        context.replaceWithHtml(
            query,
            introText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}"))
                )
            )
        )
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleLessonStart: user not found for telegramId=$telegramId")
            return
        }
        progressService.markLessonStarted(user.id, lesson.id)
        analyticsService.log("lesson_opened", user.id, props = mapOf("lessonId" to lesson.id.toString()))
    }

    private suspend fun handleVocabularyCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        handleVocabularyList(context, query, lessonId)
    }

    private suspend fun handleVocabularyList(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
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

        // Edit in place for smoother navigation
        context.editOrSendHtml(
            query,
            S.vocabularyForLesson(lessonId),
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private suspend fun handleVocabularyWord(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?,
        vocabId: Int?
    ) {
        if (lessonId == null || vocabId == null) {
            return
        }
        val vocab = lessonService.findVocabularyById(vocabId) ?: return
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val entry = dictionaryService.getEntry(user.id, vocabId)
        val isFavorite = entry?.isFavorite == true

        val text = buildString {
            appendLine(S.vocabularyWordTitle(vocab.word, vocab.translation))
            vocab.pronunciation?.let { appendLine(S.vocabularyPronunciation(it)) }
            vocab.example?.let { appendLine(S.vocabularyExample(it)) }
        }

        val buttons = buildList {
            if (isFavorite) {
                add(listOf(dataInlineButton(S.btnRemoveFromDictionary, "vocab_remove:$lessonId:$vocabId")))
            } else {
                add(listOf(dataInlineButton(S.btnAddToDictionary, "vocab_add:$lessonId:$vocabId")))
            }
            add(listOf(dataInlineButton(S.btnBack, "vocab_list:$lessonId")))
        }

        // Edit in place for smoother navigation
        context.editOrSendHtml(
            query,
            text,
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private suspend fun handleVocabularyAddAll(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val vocabIds = lessonService.getVocabulary(lessonId).map { it.id }
        dictionaryService.addAllFavorites(user.id, vocabIds)
        // Go directly back to vocabulary list with stars updated (no intermediate message)
        handleVocabularyList(context, query, lessonId)
    }

    private suspend fun handleVocabularyAdd(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?,
        vocabId: Int?
    ) {
        if (lessonId == null || vocabId == null) {
            return
        }
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        dictionaryService.addFavorite(user.id, vocabId)
        handleVocabularyWord(context, query, lessonId, vocabId)
    }

    private suspend fun handleVocabularyRemove(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?,
        vocabId: Int?
    ) {
        if (lessonId == null || vocabId == null) {
            return
        }
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        dictionaryService.removeFavorite(user.id, vocabId)
        handleVocabularyWord(context, query, lessonId, vocabId)
    }

    private suspend fun handlePracticeStart(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        handleLessonPractice(context, query, user.currentLessonId)
    }

    private suspend fun handleLessonPractice(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }
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
        userStateService.set(user.id, UserFlowState.EXERCISE.name, json.encodeToString(payload))
        sendExercise(context, query, payload)
    }

    private suspend fun handleExerciseAnswer(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 4) {
            logger.warn("handleExerciseAnswer: invalid parts count ${parts.size}")
            return
        }
        val vocabId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleExerciseAnswer: invalid vocabId '${parts[2]}'")
            return
        }
        val selectedIndex = parts[3].toIntOrNull() ?: run {
            logger.warn("handleExerciseAnswer: invalid selectedIndex '${parts[3]}'")
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleExerciseAnswer: user not found for telegramId=$telegramId")
            return
        }
        val state = userStateService.get(user.id) ?: run {
            logger.warn("handleExerciseAnswer: no state for user=${user.id}")
            return
        }
        if (state.state != UserFlowState.EXERCISE.name) {
            logger.warn("handleExerciseAnswer: wrong state '${state.state}', expected EXERCISE")
            return
        }

        val payload = json.decodeFromString<ExerciseFlowPayload>(state.payload)
        val options = payload.optionsByVocabId[vocabId] ?: run {
            logger.warn("handleExerciseAnswer: no options for vocabId=$vocabId")
            return
        }
        val selected = options.getOrNull(selectedIndex) ?: run {
            logger.warn("handleExerciseAnswer: invalid selectedIndex=$selectedIndex, options count=${options.size}")
            return
        }
        val correct = payload.correctByVocabId[vocabId] ?: run {
            logger.warn("handleExerciseAnswer: no correct answer for vocabId=$vocabId")
            return
        }
        val isCorrect = selected == correct

        val verdict = if (isCorrect) S.exerciseCorrect else S.exerciseIncorrect

        analyticsService.log(
            "exercise_answered",
            user.id,
            props = mapOf("isCorrect" to isCorrect.toString())
        )

        val explanation = payload.explanationsByVocabId[vocabId] ?: ""
        val buttons = if (isCorrect) {
            listOf(listOf(dataInlineButton(S.btnNext, "exercise_next")))
        } else {
            listOf(
                listOf(dataInlineButton(S.btnAddToDictionary, "exercise_add_dict:$vocabId")),
                listOf(dataInlineButton(S.btnNext, "exercise_next"))
            )
        }

        // Edit in place for smooth flow
        context.editOrSendHtml(
            query,
            buildString {
                appendLine(verdict)
                if (explanation.isNotBlank()) {
                    appendLine(explanation)
                }
            },
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private suspend fun handleExerciseAddDictionary(
        context: BehaviourContext,
        query: DataCallbackQuery,
        vocabId: Int?
    ) {
        if (vocabId == null) {
            return
        }
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        dictionaryService.addFavorite(user.id, vocabId)
        handleExerciseNext(context, query)
    }

    private suspend fun handleExerciseNext(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val state = userStateService.get(user.id) ?: return
        if (state.state != UserFlowState.EXERCISE.name) {
            return
        }
        val payload = json.decodeFromString<ExerciseFlowPayload>(state.payload)
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
        userStateService.set(user.id, UserFlowState.EXERCISE.name, json.encodeToString(nextPayload))
        sendExercise(context, query, nextPayload)
    }

    private suspend fun sendExercise(
        context: BehaviourContext,
        query: DataCallbackQuery,
        payload: ExerciseFlowPayload
    ) {
        val vocabId = payload.vocabularyIds.getOrNull(payload.exerciseIndex) ?: return
        val vocab = lessonService.findVocabularyById(vocabId) ?: return
        val options = payload.optionsByVocabId[vocabId] ?: return

        val buttons = options.mapIndexed { index, option ->
            listOf(dataInlineButton(option, "exercise_answer:${payload.lessonId}:$vocabId:$index"))
        }

        analyticsService.log(
            "exercise_started",
            userService.findByTelegramId(query.from.id.chatId.long)?.id ?: return,
            props = mapOf("type" to "vocab_mcq")
        )

        // Edit in place for smooth exercise flow
        context.editOrSendHtml(
            query,
            S.exercisePrompt(vocab.word),
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private suspend fun handleDictionaryPrompt(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        handleDictionaryList(context, query, 0)
    }

    private suspend fun handleDictionaryList(
        context: BehaviourContext,
        query: DataCallbackQuery,
        page: Int
    ) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val entries = dictionaryService.listUserDictionary(user.id)
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
                entry.pronunciation?.let { appendLine(S.vocabularyPronunciation(it)) }
                entry.example?.let { appendLine(S.vocabularyExample(it)) }
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

        // Edit in place for smooth pagination
        context.editOrSendHtml(
            query,
            text,
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private suspend fun handleDictionaryAddCustom(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        userStateService.set(user.id, UserFlowState.DICT_ADD_CUSTOM.name, "{}")
        context.editOrSendHtml(
            query,
            S.dictionaryAddPrompt,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnCancel, "dictionary_prompt")))
            )
        )
    }

    private suspend fun handleDictionaryFavorite(
        context: BehaviourContext,
        query: DataCallbackQuery,
        vocabId: Int?
    ) {
        if (vocabId == null) {
            return
        }
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        dictionaryService.toggleFavorite(user.id, vocabId)
        analyticsService.log("word_favorited", user.id, props = mapOf("itemId" to vocabId.toString()))
        // Return to dictionary list with updated state
        handleDictionaryList(context, query, 0)
    }

    private suspend fun handleDictionaryTags(
        context: BehaviourContext,
        query: DataCallbackQuery,
        vocabId: Int?
    ) {
        if (vocabId == null) {
            return
        }
        val tags = listOf("фразы", "глаголы", "существительные", "часто")
        val buttons = tags.map { tag ->
            listOf(dataInlineButton(tag, "dict_tag:$vocabId:$tag"))
        }
        context.sendHtml(
            query.from,
            S.dictionaryTagPrompt,
            replyMarkup = InlineKeyboardMarkup(buttons + listOf(listOf(dataInlineButton(S.btnBack, "dictionary_prompt"))))
        )
    }

    private suspend fun handleDictionaryTagToggle(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        if (parts.size < 3) {
            return
        }
        val vocabId = parts[1].toIntOrNull() ?: return
        val tag = parts[2]
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val entry = dictionaryService.getEntry(user.id, vocabId)
        val tags = entry?.tags?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
        val nextTags = if (tags.contains(tag)) tags - tag else tags + tag
        dictionaryService.setTags(user.id, vocabId, nextTags)
        // Return to dictionary list
        handleDictionaryList(context, query, 0)
    }

    private suspend fun handleReviewStart(context: BehaviourContext, query: DataCallbackQuery) {
        val telegramId = query.from.id.chatId.long
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewStart: user not found for telegramId=$telegramId")
            return
        }
        val queue = reviewService.buildQueue(user.id, limit = 12, currentLessonId = user.currentLessonId)
        if (queue.isEmpty()) {
            context.editOrSendHtml(
                query,
                S.reviewEmpty,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        val payload = ReviewFlowPayload(
            vocabularyIds = queue.map { it.id },
            index = 0
        )
        userStateService.set(user.id, UserFlowState.REVIEW.name, json.encodeToString(payload))
        analyticsService.log("review_started", user.id)
        sendReviewCard(context, query, payload)
    }

    private suspend fun handleReviewAnswer(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 3) {
            logger.warn("handleReviewAnswer: invalid parts count ${parts.size}")
            return
        }
        val vocabId = parts[1].toIntOrNull() ?: run {
            logger.warn("handleReviewAnswer: invalid vocabId '${parts[1]}'")
            return
        }
        val isCorrect = parts[2] == "1"

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleReviewAnswer: user not found for telegramId=$telegramId")
            return
        }
        reviewService.updateCard(user.id, vocabId, isCorrect)
        progressService.recordReview(user.id)
        analyticsService.log("review_completed", user.id, props = mapOf("isCorrect" to isCorrect.toString()))

        val state = userStateService.get(user.id) ?: run {
            logger.warn("handleReviewAnswer: no state for user=${user.id}")
            return
        }
        if (state.state != UserFlowState.REVIEW.name) {
            logger.warn("handleReviewAnswer: wrong state '${state.state}', expected REVIEW")
            return
        }
        val payload = json.decodeFromString<ReviewFlowPayload>(state.payload)
        val nextIndex = payload.index + 1
        if (nextIndex >= payload.vocabularyIds.size) {
            userStateService.clear(user.id)
            context.editOrSendHtml(
                query,
                S.reviewDone,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }
        val nextPayload = payload.copy(index = nextIndex)
        userStateService.set(user.id, UserFlowState.REVIEW.name, json.encodeToString(nextPayload))
        sendReviewCard(context, query, nextPayload)
    }

    private suspend fun sendReviewCard(
        context: BehaviourContext,
        query: DataCallbackQuery,
        payload: ReviewFlowPayload
    ) {
        val vocabId = payload.vocabularyIds.getOrNull(payload.index) ?: return
        val item = lessonService.findVocabularyById(vocabId) ?: return
        val card = buildString {
            appendLine(S.reviewCardTitle(item.word))
            appendLine(S.reviewCardTranslation(item.translation))
            item.example?.let { appendLine(S.dictionaryExample(it)) }
        }

        // Edit in place for smooth review flow
        context.editOrSendHtml(
            query,
            card,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        dataInlineButton(S.btnRemember, "review_answer:$vocabId:1"),
                        dataInlineButton(S.btnAgain, "review_answer:$vocabId:0")
                    )
                )
            )
        )
    }

    private suspend fun handleReminders(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val pref = reminderPreferenceService.getOrDefault(user.id)
        val status = if (pref.isEnabled) S.reminderStatusOn(pref.daysOfWeek, pref.timeLocal)
        else S.reminderStatusOff

        context.editOrSendHtml(
            query,
            status,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnEnableWeekdays, "reminder_enable_weekdays")),
                    listOf(dataInlineButton(S.btnDisableReminders, "reminder_disable")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private suspend fun handleReminderEnableWeekdays(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val pref = reminderPreferenceService.setSchedule(user.id, "MON,TUE,WED,THU,FRI", "19:00")
        analyticsService.log("reminder_set", user.id, props = mapOf("schedule" to "${pref.daysOfWeek} ${pref.timeLocal}"))
        context.editOrSendHtml(
            query,
            S.reminderEnabled(pref.daysOfWeek, pref.timeLocal),
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }

    private suspend fun handleReminderDisable(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderPreferenceService.setEnabled(user.id, false)
        context.editOrSendHtml(
            query,
            S.reminderDisabled,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }

    private suspend fun handleHelp(context: BehaviourContext, query: DataCallbackQuery) {
        context.editOrSendHtml(
            query,
            S.help,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }

    private suspend fun handleContinue(context: BehaviourContext, query: DataCallbackQuery) {
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
                val payload = json.decodeFromString<ExerciseFlowPayload>(state.payload)
                sendExercise(context, query, payload)
            }
            UserFlowState.REVIEW.name -> {
                val payload = json.decodeFromString<ReviewFlowPayload>(state.payload)
                sendReviewCard(context, query, payload)
            }
            UserFlowState.DICT_SEARCH.name -> {
                context.sendHtml(query.from, S.dictionaryPrompt)
            }
            UserFlowState.HOMEWORK_TEXT.name -> {
                context.sendHtml(query.from, S.homeworkContinue)
            }
        }
    }

    private suspend fun handleHomeworkCallback(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        if (lessonId == null) {
            return
        }

        context.editOrSendHtml(
            query,
            S.homeworkStart,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnStartHomework, "start_homework:$lessonId")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
        val user = userService.findByTelegramId(query.from.id.chatId.long)
        if (user != null) {
            analyticsService.log("hw_opened", user.id, props = mapOf("lessonId" to lessonId.toString()))
        }
    }

    private suspend fun handleStartHomework(
        context: BehaviourContext,
        query: DataCallbackQuery,
        lessonId: Int?
    ) {
        val telegramId = query.from.id.chatId.long
        if (lessonId == null) {
            logger.warn("handleStartHomework: lessonId is null")
            return
        }

        val homework = homeworkService.getHomeworkForLesson(lessonId) ?: run {
            logger.info("handleStartHomework: no homework for lessonId=$lessonId")
            context.editOrSendHtml(
                query,
                S.homeworkNotReady,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
                )
            )
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleStartHomework: user not found for telegramId=$telegramId")
            return
        }

        if (homeworkService.hasCompletedHomework(user.id, homework.id)) {
            context.editOrSendHtml(
                query,
                S.homeworkAlreadyCompleted,
                replyMarkup = InlineKeyboardMarkup(
                    listOf(
                        listOf(dataInlineButton(S.btnContinue, "lesson_start:${user.currentLessonId}")),
                        listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                    )
                )
            )
            return
        }

        sendQuestion(context, query, homework.questions.first(), 0, homework.id)
        analyticsService.log("hw_opened", user.id, props = mapOf("lessonId" to lessonId.toString()))
    }

    private suspend fun sendQuestion(
        context: BehaviourContext,
        query: DataCallbackQuery,
        question: com.turki.core.domain.HomeworkQuestion,
        index: Int,
        homeworkId: Int
    ) {
        val questionText = "${S.questionTitle(index + 1)}\n\n${question.questionText}"

        when (question.questionType) {
            QuestionType.MULTIPLE_CHOICE -> {
                // Only send index in callback to avoid 64-byte limit and colon parsing issues
                val buttons = question.options.mapIndexed { optIndex, option ->
                    listOf(dataInlineButton(option, "answer:$homeworkId:${question.id}:$optIndex"))
                }
                // Edit in place for smooth homework flow
                context.editOrSendHtml(
                    query,
                    questionText,
                    replyMarkup = InlineKeyboardMarkup(buttons)
                )
            }
            QuestionType.TEXT_INPUT, QuestionType.TRANSLATION -> {
                // For text input, we need a new message as user will type answer
                context.replaceWithHtml(query, "$questionText\n\n${S.writeYourAnswer}")
                HomeworkStateManager.setCurrentQuestion(query.from.id.chatId.long, homeworkId, question.id)
                val user = userService.findByTelegramId(query.from.id.chatId.long)
                if (user != null) {
                    userStateService.set(user.id, UserFlowState.HOMEWORK_TEXT.name, "{}")
                }
            }
        }
    }

    private suspend fun handleAnswer(
        context: BehaviourContext,
        query: DataCallbackQuery,
        parts: List<String>
    ) {
        // Format: answer:homeworkId:questionId:optionIndex
        val telegramId = query.from.id.chatId.long
        if (parts.size < 4) {
            logger.warn("handleAnswer: invalid parts count ${parts.size}, expected 4. Data: $parts")
            return
        }

        val homeworkId = parts[1].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid homeworkId '${parts[1]}'")
            return
        }
        val questionId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid questionId '${parts[2]}'")
            return
        }
        val optionIndex = parts[3].toIntOrNull() ?: run {
            logger.warn("handleAnswer: invalid optionIndex '${parts[3]}'")
            return
        }

        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleAnswer: user not found for telegramId=$telegramId")
            return
        }
        val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
            logger.warn("handleAnswer: homework not found for homeworkId=$homeworkId, user=${user.id}")
            return
        }
        val question = homework.questions.firstOrNull { it.id == questionId } ?: run {
            logger.warn("handleAnswer: question not found for questionId=$questionId in homework=$homeworkId")
            return
        }

        // Get the answer text from the question options by index
        val answer = question.options.getOrNull(optionIndex) ?: run {
            logger.warn("handleAnswer: invalid optionIndex=$optionIndex, options count=${question.options.size}")
            return
        }

        val currentAnswers = HomeworkStateManager.getAnswers(user.telegramId)
        currentAnswers[questionId] = answer
        HomeworkStateManager.setAnswers(user.telegramId, currentAnswers)

        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }

        val isCorrect = homeworkService.isAnswerCorrect(question, answer)

        if (!isCorrect) {
            val buttons = buildHomeworkWrongButtons(homeworkId, questionId, homework.lessonId, question)
            val text = buildString {
                appendLine(S.exerciseIncorrect)
                appendLine(S.homeworkCorrectAnswer(question.correctAnswer))
            }
            // Edit in place for smooth homework flow
            context.editOrSendHtml(
                query,
                text,
                replyMarkup = InlineKeyboardMarkup(buttons)
            )
            return
        }

        if (currentIndex < homework.questions.size - 1) {
            sendQuestion(context, query, homework.questions[currentIndex + 1], currentIndex + 1, homeworkId)
        } else {
            submitHomeworkResult(context, query, user, homework, currentAnswers)
        }
    }

    private suspend fun handleHomeworkNext(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 3) {
            logger.warn("handleHomeworkNext: invalid parts count ${parts.size}")
            return
        }
        val homeworkId = parts[1].toIntOrNull() ?: run {
            logger.warn("handleHomeworkNext: invalid homeworkId '${parts[1]}'")
            return
        }
        val questionId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleHomeworkNext: invalid questionId '${parts[2]}'")
            return
        }
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleHomeworkNext: user not found for telegramId=$telegramId")
            return
        }
        val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
            logger.warn("handleHomeworkNext: homework not found for homeworkId=$homeworkId")
            return
        }
        val currentIndex = homework.questions.indexOfFirst { it.id == questionId }
        if (currentIndex < homework.questions.size - 1) {
            sendQuestion(context, query, homework.questions[currentIndex + 1], currentIndex + 1, homeworkId)
        } else {
            val answers = HomeworkStateManager.getAnswers(user.telegramId)
            submitHomeworkResult(context, query, user, homework, answers)
        }
    }

    private suspend fun handleHomeworkAddDictionary(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
        val telegramId = query.from.id.chatId.long
        if (parts.size < 3) {
            logger.warn("handleHomeworkAddDictionary: invalid parts count ${parts.size}")
            return
        }
        val homeworkId = parts[1].toIntOrNull() ?: run {
            logger.warn("handleHomeworkAddDictionary: invalid homeworkId '${parts[1]}'")
            return
        }
        val questionId = parts[2].toIntOrNull() ?: run {
            logger.warn("handleHomeworkAddDictionary: invalid questionId '${parts[2]}'")
            return
        }
        val user = userService.findByTelegramId(telegramId) ?: run {
            logger.warn("handleHomeworkAddDictionary: user not found for telegramId=$telegramId")
            return
        }
        val homework = homeworkService.getHomeworkById(homeworkId) ?: run {
            logger.warn("handleHomeworkAddDictionary: homework not found for homeworkId=$homeworkId")
            return
        }
        val question = homework.questions.firstOrNull { it.id == questionId } ?: run {
            logger.warn("handleHomeworkAddDictionary: question not found for questionId=$questionId")
            return
        }
        val vocabId = resolveHomeworkVocabularyId(homework.lessonId, question)
        if (vocabId != null) {
            dictionaryService.addFavorite(user.id, vocabId)
        }
        handleHomeworkNext(context, query, listOf("hw_next", homeworkId.toString(), questionId.toString()))
    }

    private suspend fun submitHomeworkResult(
        context: BehaviourContext,
        query: DataCallbackQuery,
        user: com.turki.core.domain.User,
        homework: com.turki.core.domain.Homework,
        answers: Map<Int, String>
    ) {
        val submission = homeworkService.submitHomework(user.id, homework.id, answers)
        HomeworkStateManager.clearState(user.telegramId)
        userStateService.clear(user.id)
        progressService.recordHomework(user.id)
        if (submission.score == submission.maxScore) {
            progressService.markLessonCompleted(user.id, homework.lessonId)
        }
        analyticsService.log("hw_submitted", user.id, props = mapOf("score" to submission.score.toString()))

        val resultText = if (submission.score == submission.maxScore) {
            S.homeworkComplete(submission.score, submission.maxScore)
        } else {
            S.homeworkResult(submission.score, submission.maxScore)
        }

        val feedback = buildHomeworkFeedback(homework, answers, submission.score, submission.maxScore)

        // Get next lesson to offer navigation
        val nextLesson = lessonService.getNextLesson(homework.lessonId, Language.TURKISH)
        val buttons = buildList {
            add(listOf(dataInlineButton(S.btnRepeatTopic, "lesson_start:${homework.lessonId}")))
            if (nextLesson != null) {
                add(listOf(dataInlineButton(S.btnNextLesson, "lesson_start:${nextLesson.id}")))
            }
            add(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
        }
        val keyboard = InlineKeyboardMarkup(buttons)

        // Edit in place for smooth homework flow
        context.editOrSendHtml(query, "$resultText\n\n$feedback", replyMarkup = keyboard)
    }

    private suspend fun buildHomeworkWrongButtons(
        homeworkId: Int,
        questionId: Int,
        lessonId: Int,
        question: com.turki.core.domain.HomeworkQuestion
    ) = buildList {
        val vocabId = resolveHomeworkVocabularyId(lessonId, question)
        if (vocabId != null) {
            add(listOf(dataInlineButton(S.btnAddToDictionary, "hw_add_dict:$homeworkId:$questionId")))
        } else {
            add(listOf(dataInlineButton(S.btnAddCustomWord, "dict_add_custom")))
        }
        add(listOf(dataInlineButton(S.btnNext, "hw_next:$homeworkId:$questionId")))
    }

    private suspend fun resolveHomeworkVocabularyId(
        lessonId: Int,
        question: com.turki.core.domain.HomeworkQuestion
    ): Int? {
        val vocabulary = lessonService.getVocabulary(lessonId)
        val answer = normalizeText(question.correctAnswer)
        val questionText = normalizeText(question.questionText)
        return vocabulary.firstOrNull { item ->
            val word = normalizeText(item.word)
            val translation = normalizeText(item.translation)
            word == answer || translation == answer || questionText.contains(word) || questionText.contains(translation)
        }?.id
    }

    private fun normalizeText(text: String): String =
        text.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

    private suspend fun handleProgressCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendHtml(query.from, S.notRegistered)
            return
        }

        val summary = progressService.getProgressSummary(user.id)

        val progressText = S.progress(
            firstName = user.firstName,
            completedLessons = summary.completedLessons,
            totalLessons = summary.totalLessons,
            subscriptionActive = user.subscriptionActive,
            currentLevel = summary.currentLevel,
            streakDays = summary.currentStreak
        )

        context.editOrSendHtml(
            query,
            progressText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
        analyticsService.log("progress_opened", user.id)
    }

    private suspend fun handleNextLessonCallback(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return

        val nextLesson = lessonService.getLessonById(user.currentLessonId)

        if (nextLesson == null) {
            context.sendHtml(query.from, S.allLessonsCompleted)
            return
        }

        val lessonText = buildString {
            appendLine(S.lessonTitle(nextLesson.orderIndex, nextLesson.title))
            appendLine()
            appendLine(nextLesson.description.markdownToHtml())
            appendLine()
            appendLine("─────────────────────")
            appendLine()
            appendLine(nextLesson.content.markdownToHtml())
        }

        context.sendHtml(
            query.from,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${nextLesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${nextLesson.id}"))
                )
            )
        )
    }

    private suspend fun handleSetReminder(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        reminderService.createHomeworkReminder(user.id)
        context.sendHtml(query.from, S.reminderSet)
    }

    private suspend fun handleSettings(context: BehaviourContext, query: DataCallbackQuery) {
        context.editOrSendHtml(
            query,
            S.settingsTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnResetProgress, "reset_progress")),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private suspend fun handleResetProgress(context: BehaviourContext, query: DataCallbackQuery) {
        context.editOrSendHtml(
            query,
            S.resetProgressConfirm,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnConfirmReset, "confirm_reset")),
                    listOf(dataInlineButton(S.btnCancel, "settings"))
                )
            )
        )
    }

    private suspend fun handleConfirmReset(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        userService.resetProgress(user.id)
        userStateService.clear(user.id)
        progressService.resetProgress(user.id)
        dictionaryService.clearUser(user.id)
        reviewService.clearUser(user.id)

        context.sendHtml(
            query.from,
            S.progressResetSuccess,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton("${S.btnStartLesson} 1", "lesson_start:1")))
            )
        )
    }

    private suspend fun handleConfirmDelete(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: run {
            context.sendHtml(query.from, S.notRegistered)
            return
        }
        userDataService.deleteUserData(user.id)
        context.sendHtml(query.from, S.deleteDataSuccess)
    }

    private suspend fun handleSelectLevel(context: BehaviourContext, query: DataCallbackQuery) {
        context.editOrSendHtml(
            query,
            S.selectLevelTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("A1", true), "set_level:A1"),
                        dataInlineButton(S.btnLevelWithStatus("A2", false), "set_level:A2")
                    ),
                    listOf(
                        dataInlineButton(S.btnLevelWithStatus("B1", false), "set_level:B1"),
                        dataInlineButton(S.btnLevelWithStatus("B2", false), "set_level:B2")
                    ),
                    listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))
                )
            )
        )
    }

    private suspend fun handleSetLevel(context: BehaviourContext, query: DataCallbackQuery, level: String?) {
        if (level == null) {
            return
        }

        val message = when (level) {
            "A1" -> S.levelA1Active
            else -> S.levelLocked(level)
        }

        context.editOrSendHtml(
            query,
            message,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBack, "select_level")))
            )
        )
    }

    private suspend fun handleKnowledgeTest(context: BehaviourContext, query: DataCallbackQuery) {
        context.editOrSendHtml(
            query,
            S.knowledgeTestTitle,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu")))
            )
        )
    }

    private suspend fun handleNextHomework(context: BehaviourContext, query: DataCallbackQuery, lessonId: Int?) {
        if (lessonId == null) {
            return
        }
        val nextLesson = lessonService.getNextLesson(lessonId, Language.TURKISH)
        if (nextLesson == null) {
            context.sendHtml(query.from, S.homeworkNoNext)
            return
        }
        handleHomeworkCallback(context, query, nextLesson.id)
    }

    private suspend fun handleBackToMenu(context: BehaviourContext, query: DataCallbackQuery) {
        val user = userService.findByTelegramId(query.from.id.chatId.long) ?: return
        val hasState = userStateService.get(user.id) != null
        val buttons = buildList {
            if (hasState) {
                add(listOf(dataInlineButton(S.btnContinue, "continue")))
            }
            addAll(
                listOf(
                    listOf(dataInlineButton(S.btnLessons, "lessons_list")),
                    listOf(dataInlineButton(S.btnPractice, "practice_start")),
                    listOf(dataInlineButton(S.btnDictionary, "dictionary_prompt")),
                    listOf(dataInlineButton(S.btnReview, "review_start")),
                    listOf(dataInlineButton(S.btnHomework, "homework:${user.currentLessonId}")),
                    listOf(dataInlineButton(S.btnProgress, "progress")),
                    listOf(dataInlineButton(S.btnReminders, "reminders")),
                    listOf(dataInlineButton(S.btnResetProgress, "reset_progress")),
                    listOf(dataInlineButton(S.btnHelp, "help"))
                )
            )
        }

        // Edit in place to replace the current menu
        context.editOrSendHtml(
            query,
            S.menuTitle,
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private fun buildHomeworkFeedback(
        homework: com.turki.core.domain.Homework,
        answers: Map<Int, String>,
        score: Int,
        maxScore: Int
    ): String {
        val wrong = homework.questions.filter { question ->
            !homeworkService.isAnswerCorrect(question, answers[question.id])
        }

        if (wrong.isEmpty()) {
            if (score < maxScore) {
                return "Есть ошибки — попробуйте ещё раз."
            }
            return S.homeworkFeedbackPerfect
        }

        val lines = wrong.take(3).joinToString("\n") { question ->
            "• ${question.questionText}\n  ${S.homeworkCorrectAnswer(question.correctAnswer)}"
        }

        return S.homeworkFeedbackSummary(lines, wrong.size)
    }
}

object HomeworkStateManager {
    private val currentQuestions = mutableMapOf<Long, Pair<Int, Int>>()
    private val userAnswers = mutableMapOf<Long, MutableMap<Int, String>>()

    fun setCurrentQuestion(telegramId: Long, homeworkId: Int, questionId: Int) {
        currentQuestions[telegramId] = homeworkId to questionId
    }

    fun getCurrentQuestion(telegramId: Long): Pair<Int, Int>? = currentQuestions[telegramId]

    fun getAnswers(telegramId: Long): MutableMap<Int, String> =
        userAnswers.getOrPut(telegramId) { mutableMapOf() }

    fun setAnswers(telegramId: Long, answers: MutableMap<Int, String>) {
        userAnswers[telegramId] = answers
    }

    fun clearState(telegramId: Long) {
        currentQuestions.remove(telegramId)
        userAnswers.remove(telegramId)
    }

    fun clearCurrentQuestion(telegramId: Long) {
        currentQuestions.remove(telegramId)
    }
}
