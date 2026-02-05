package com.turki.bot.handler

import com.turki.bot.i18n.S
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.core.domain.EventNames
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import com.turki.bot.service.UserFlowState
import com.turki.bot.util.markdownToHtml
import com.turki.bot.util.sendHtml
import com.turki.core.domain.Language
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.serialization.decodeFromString

/**
 * Handler for Telegram bot text commands.
 *
 * This class processes all text commands sent by users to the bot, including:
 * - /start - User registration and welcome message
 * - /lesson - Display current lesson
 * - /homework - Show homework options
 * - /progress - Display user progress
 * - /help - Show help information
 * - /vocabulary - Display lesson vocabulary
 *
 * All commands are processed asynchronously and send HTML-formatted responses.
 */
class CommandHandler(
    private val userService: UserService,
    private val lessonService: LessonService,
    private val progressService: ProgressService,
    private val dictionaryService: DictionaryService,
    private val userStateService: UserStateService,
    private val analyticsService: AnalyticsService,
    private val reminderPreferenceService: ReminderPreferenceService,
    private val userDataService: UserDataService
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    suspend fun handleStart(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val isNewUser = userService.findByTelegramId(from.id.chatId.long) == null
        val user = userService.findOrCreateUser(
            telegramId = from.id.chatId.long,
            username = from.username?.username,
            firstName = from.firstName,
            lastName = from.lastName
        )

        if (isNewUser) {
            analyticsService.log(EventNames.USER_REGISTERED, user.id)
        } else {
            analyticsService.log(EventNames.USER_RETURNED, user.id)
        }
        analyticsService.log(EventNames.SESSION_START, user.id)
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "start"))
        context.sendHtml(message.chat, S.welcome(user.firstName))
        sendMainMenu(context, message, user, S.mainMenuTitle)
    }

    suspend fun handleLesson(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "lesson"))

        val lesson = lessonService.getLessonById(user.currentLessonId)

        if (lesson == null) {
            context.sendHtml(message.chat, S.allLessonsCompleted)
            return
        }

        analyticsService.log(EventNames.LESSON_STARTED, user.id, props = mapOf("lessonId" to lesson.id.toString()))

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
            message.chat,
            lessonText,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnVocabulary, "vocabulary:${lesson.id}")),
                    listOf(dataInlineButton(S.btnGoToHomework, "homework:${lesson.id}")),
                    listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${lesson.id}"))
                )
            )
        )
    }

    suspend fun handleHomework(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "homework"))

        context.sendHtml(
            message.chat,
            S.homeworkStart,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnStartHomework, "start_homework:${user.currentLessonId}"))
                )
            )
        )
    }

    suspend fun handleProgress(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "progress"))

        val summary = progressService.getProgressSummary(user.id)

        val progressText = S.progress(
            firstName = user.firstName,
            completedLessons = summary.completedLessons,
            totalLessons = summary.totalLessons,
            subscriptionActive = user.subscriptionActive,
            currentLevel = summary.currentLevel,
            streakDays = summary.currentStreak
        )

        context.sendHtml(message.chat, progressText)
    }

    suspend fun handleHelp(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        val userId = from?.let { userService.findByTelegramId(it.id.chatId.long)?.id }
        if (userId != null) {
            analyticsService.log(EventNames.COMMAND_USED, userId, props = mapOf("command" to "help"))
        }
        context.sendHtml(message.chat, S.help)
    }

    suspend fun handleVocabulary(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from
        if (from == null) {
            return
        }
        val user = userService.findByTelegramId(from.id.chatId.long)
        if (user == null) {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "vocabulary"))

        val vocabulary = lessonService.getVocabulary(user.currentLessonId)

        if (vocabulary.isEmpty()) {
            context.sendHtml(message.chat, S.vocabularyEmpty)
            return
        }

        val vocabText = buildString {
            appendLine(S.vocabularyForLesson(user.currentLessonId))
            appendLine()
            vocabulary.forEach { item ->
                appendLine(S.vocabularyItem(item.word, item.translation))
                item.pronunciation?.let { appendLine(S.dictionaryPronunciation(it)) }
                item.example?.let { appendLine(S.dictionaryExample(it)) }
                appendLine()
            }
        }

        context.sendHtml(message.chat, vocabText)
    }

    suspend fun handleMenu(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "menu"))
        sendMainMenu(context, message, user, S.menuTitle)
    }

    suspend fun handleLessons(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "lessons"))

        val lessons = lessonService.getLessonsByLanguage(Language.TURKISH)
        val completed = progressService.getCompletedLessonIds(user.id)
        val perPage = 5
        val totalPages = (lessons.size + perPage - 1) / perPage
        val pageLessons = lessons.take(perPage)
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
            buttons.add(listOf(dataInlineButton("▶️", "lessons_list:1")))
        }

        context.sendHtml(
            message.chat,
            if (totalPages > 1) "${S.lessonsTitle}\n\nСтраница 1/$totalPages" else S.lessonsTitle,
            replyMarkup = InlineKeyboardMarkup(buttons + listOf(listOf(dataInlineButton(S.btnBackToMenu, "back_to_menu"))))
        )
    }

    suspend fun handlePractice(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "practice"))
        context.sendHtml(message.chat, S.practiceIntro)
        context.sendHtml(
            message.chat,
            S.practicePrompt,
            replyMarkup = InlineKeyboardMarkup(
                listOf(listOf(dataInlineButton(S.btnStartPractice, "lesson_practice:${user.currentLessonId}")))
            )
        )
    }

    suspend fun handleDictionary(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "dictionary"))

        val text = message.content.text
        val query = text.removePrefix("/dictionary").trim()
        if (query.isEmpty()) {
            sendDictionaryList(context, message, user.id, 0)
            return
        }

        handleDictionaryQuery(context, message, user.id, query)
    }

    suspend fun handleDictionaryQueryText(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        val query = message.content.text.trim()
        handleDictionaryQuery(context, message, user.id, query)
    }

    suspend fun handleDictionaryCustomText(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        val input = message.content.text.trim()
        val parsed = parseCustomDictionaryInput(input)
        if (parsed == null) {
            userStateService.set(user.id, UserFlowState.DICT_ADD_CUSTOM.name, "{}")
            context.sendHtml(message.chat, S.dictionaryAddFormatError)
            return
        }
        dictionaryService.addCustomWord(user.id, parsed.first, parsed.second)
        sendDictionaryList(context, message, user.id, 0)
    }

    suspend fun handleReview(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "review"))
        context.sendHtml(
            message.chat,
            S.reviewIntro,
            replyMarkup = InlineKeyboardMarkup(listOf(listOf(dataInlineButton(S.btnStartReview, "review_start"))))
        )
    }

    suspend fun handleReminders(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "reminders"))
        val pref = reminderPreferenceService.getOrDefault(user.id)
        val status = if (pref.isEnabled) S.reminderStatusOn(pref.daysOfWeek, pref.timeLocal)
        else S.reminderStatusOff

        context.sendHtml(
            message.chat,
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

    suspend fun handleReset(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "reset"))
        context.sendHtml(
            message.chat,
            S.resetProgressConfirm,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnConfirmReset, "confirm_reset")),
                    listOf(dataInlineButton(S.btnCancel, "back_to_menu"))
                )
            )
        )
    }

    suspend fun handleDelete(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }
        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "delete"))
        context.sendHtml(
            message.chat,
            S.deleteDataConfirm,
            replyMarkup = InlineKeyboardMarkup(
                listOf(
                    listOf(dataInlineButton(S.btnConfirmDelete, "confirm_delete")),
                    listOf(dataInlineButton(S.btnCancel, "back_to_menu"))
                )
            )
        )
    }

    suspend fun handleExport(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "export"))
        context.sendHtml(message.chat, S.exportDataPreparing)

        val exportJson = userDataService.exportUserData(user)
        val fileName = "turki_data_${user.telegramId}.json"
        val fileBytes = exportJson.toByteArray(Charsets.UTF_8)

        context.sendDocument(
            message.chat,
            fileBytes.asMultipartFile(fileName),
            text = S.exportDataReady
        )

        analyticsService.log(EventNames.DATA_EXPORTED, user.id)
    }

    suspend fun handleSupport(context: BehaviourContext, message: CommonMessage<TextContent>) {
        val from = message.from ?: return
        val user = userService.findByTelegramId(from.id.chatId.long) ?: run {
            context.sendHtml(message.chat, S.notRegistered)
            return
        }

        analyticsService.log(EventNames.COMMAND_USED, user.id, props = mapOf("command" to "support"))
        userStateService.set(user.id, UserFlowState.SUPPORT_MESSAGE.name, "{}")
        context.sendHtml(message.chat, S.supportPrompt)
    }

    private suspend fun handleDictionaryQuery(
        context: BehaviourContext,
        message: CommonMessage<TextContent>,
        userId: Long,
        query: String
    ) {
        val results = dictionaryService.search(query, limit = 1)
        if (results.isEmpty()) {
            context.sendHtml(message.chat, S.dictionaryNoResults)
            return
        }

        val item = results.first()
        val entry = dictionaryService.getEntry(userId, item.id)
        val favLabel = if (entry?.isFavorite == true) "⭐️" else "☆"
        val tags = entry?.tags?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
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

    private suspend fun sendDictionaryList(
        context: BehaviourContext,
        message: CommonMessage<TextContent>,
        userId: Long,
        page: Int
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

    private fun parseCustomDictionaryInput(input: String): Pair<String, String>? {
        val separators = listOf(" - ", " — ", " : ", " , ")
        val sep = separators.firstOrNull { input.contains(it) } ?: return null
        val parts = input.split(sep, limit = 2)
        if (parts.size < 2) return null
        val word = parts[0].trim()
        val translation = parts[1].trim()
        if (word.isBlank() || translation.isBlank()) return null
        return word to translation
    }

    private suspend fun sendMainMenu(
        context: BehaviourContext,
        message: CommonMessage<TextContent>,
        user: com.turki.core.domain.User,
        header: String
    ) {
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

        context.sendHtml(
            message.chat,
            header,
            replyMarkup = InlineKeyboardMarkup(buttons)
        )
    }

    private fun extractLessonTopic(title: String): String {
        val first = title.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        val cleaned = first.trim { ch -> !ch.isLetterOrDigit() }
        return if (cleaned.isNotBlank()) cleaned else title.take(20)
    }
}
