package com.turki.bot.handler.callback.homework

import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import org.slf4j.LoggerFactory

class HomeworkAddDictAction(
    private val userService: UserService,
    private val homeworkService: HomeworkService,
    private val dictionaryService: DictionaryService,
    private val lessonService: LessonService,
    private val userStateService: UserStateService,
    private val progressService: ProgressService,
    private val analyticsService: AnalyticsService
) : CallbackAction {
    override val action: String = "hw_add_dict"

    override suspend fun invoke(context: BehaviourContext, query: DataCallbackQuery, parts: List<String>) {
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
        val vocabId = resolveHomeworkVocabularyId(lessonService, homework.lessonId, question)
        if (vocabId != null) {
            dictionaryService.addFavorite(user.id, vocabId)
        }
        advanceHomework(
            context,
            query,
            listOf("hw_next", homeworkId.toString(), questionId.toString()),
            userService,
            homeworkService,
            userStateService,
            progressService,
            analyticsService,
            lessonService
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger("HomeworkAddDictAction")
    }
}
