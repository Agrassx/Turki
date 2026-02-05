package com.turki.bot.di

import com.turki.bot.handler.CallbackHandler
import com.turki.bot.handler.CommandHandler
import com.turki.bot.handler.HomeworkHandler
import com.turki.bot.handler.callback.CallbackAction
import com.turki.bot.handler.callback.dictionary.DictAddCustomAction
import com.turki.bot.handler.callback.dictionary.DictFavAction
import com.turki.bot.handler.callback.dictionary.DictListAction
import com.turki.bot.handler.callback.dictionary.DictTagAction
import com.turki.bot.handler.callback.dictionary.DictTagsAction
import com.turki.bot.handler.callback.dictionary.DictionaryPromptAction
import com.turki.bot.handler.callback.exercise.ExerciseAddDictAction
import com.turki.bot.handler.callback.exercise.ExerciseAnswerAction
import com.turki.bot.handler.callback.exercise.ExerciseNextAction
import com.turki.bot.handler.callback.exercise.LessonPracticeAction
import com.turki.bot.handler.callback.exercise.PracticeStartAction
import com.turki.bot.handler.callback.homework.AnswerAction
import com.turki.bot.handler.callback.homework.HomeworkAction
import com.turki.bot.handler.callback.homework.HomeworkAddDictAction
import com.turki.bot.handler.callback.homework.HomeworkNextAction
import com.turki.bot.handler.callback.homework.NextHomeworkAction
import com.turki.bot.handler.callback.homework.StartHomeworkAction
import com.turki.bot.handler.callback.lesson.LessonAction
import com.turki.bot.handler.callback.lesson.LessonStartAction
import com.turki.bot.handler.callback.lesson.LessonsListAction
import com.turki.bot.handler.callback.lesson.NextLessonAction
import com.turki.bot.handler.callback.menu.BackToMenuAction
import com.turki.bot.handler.callback.menu.ConfirmDeleteAction
import com.turki.bot.handler.callback.menu.ConfirmResetAction
import com.turki.bot.handler.callback.menu.ContinueAction
import com.turki.bot.handler.callback.menu.HelpAction
import com.turki.bot.handler.callback.menu.KnowledgeTestAction
import com.turki.bot.handler.callback.menu.ProgressAction
import com.turki.bot.handler.callback.menu.ResetProgressAction
import com.turki.bot.handler.callback.menu.SelectLevelAction
import com.turki.bot.handler.callback.menu.SetLevelAction
import com.turki.bot.handler.callback.menu.SettingsAction
import com.turki.bot.handler.callback.reminder.ReminderDayToggleAction
import com.turki.bot.handler.callback.reminder.ReminderDaysConfirmAction
import com.turki.bot.handler.callback.reminder.ReminderDisableAction
import com.turki.bot.handler.callback.reminder.ReminderEnableWeekdaysAction
import com.turki.bot.handler.callback.reminder.ReminderFrequencyAction
import com.turki.bot.handler.callback.reminder.ReminderTimeAction
import com.turki.bot.handler.callback.reminder.RemindersAction
import com.turki.bot.handler.callback.reminder.SetReminderAction
import com.turki.bot.handler.callback.review.ReviewAnswerAction
import com.turki.bot.handler.callback.review.ReviewDifficultyAction
import com.turki.bot.handler.callback.review.ReviewSessionAnswerAction
import com.turki.bot.handler.callback.review.ReviewSessionNextAction
import com.turki.bot.handler.callback.review.ReviewStartAction
import com.turki.bot.handler.callback.vocabulary.VocabAddAction
import com.turki.bot.handler.callback.vocabulary.VocabAddAllAction
import com.turki.bot.handler.callback.vocabulary.VocabListAction
import com.turki.bot.handler.callback.vocabulary.VocabRemoveAction
import com.turki.bot.handler.callback.vocabulary.VocabWordAction
import com.turki.bot.handler.callback.vocabulary.VocabularyAction
import com.turki.bot.handler.command.CommandAction
import com.turki.bot.handler.command.CommandTextAction
import com.turki.bot.handler.command.dictionary.DictionaryCommand
import com.turki.bot.handler.command.dictionary.DictionaryCustomTextAction
import com.turki.bot.handler.command.dictionary.DictionaryQueryTextAction
import com.turki.bot.handler.command.lesson.HomeworkCommand
import com.turki.bot.handler.command.lesson.LessonCommand
import com.turki.bot.handler.command.lesson.LessonsCommand
import com.turki.bot.handler.command.lesson.PracticeCommand
import com.turki.bot.handler.command.lesson.VocabularyCommand
import com.turki.bot.handler.command.menu.HelpCommand
import com.turki.bot.handler.command.menu.MenuCommand
import com.turki.bot.handler.command.menu.ProgressCommand
import com.turki.bot.handler.command.reminder.RemindersCommand
import com.turki.bot.handler.command.review.ReviewCommand
import com.turki.bot.handler.command.system.DeleteCommand
import com.turki.bot.handler.command.system.ExportCommand
import com.turki.bot.handler.command.system.ResetCommand
import com.turki.bot.handler.command.system.StartCommand
import com.turki.bot.handler.command.system.SupportCommand
import com.turki.bot.handler.homework.HomeworkTextAction
import com.turki.bot.handler.homework.HomeworkTextAnswerAction
import com.turki.bot.service.HomeworkService
import com.turki.bot.service.LessonService
import com.turki.bot.service.AnalyticsService
import com.turki.bot.service.DictionaryService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.MetricsService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.SupportService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import org.koin.dsl.module

val botModule = module {
    single { TimeZone.currentSystemDefault() }
    single<Random> { Random.Default }
    single { UserService(get(), get()) }
    single { LessonService(get()) }
    single { HomeworkService(get(), get(), get()) }
    single { ReminderService(get(), get()) }
    single { UserStateService(get()) }
    single { ExerciseService(get(), get()) }
    single { ProgressService(get(), get(), get(), get(), get()) }
    single { DictionaryService(get(), get(), get(), get()) }
    single { ReviewService(get(), get(), get(), get(), get(), get(), get()) }
    single { ReminderPreferenceService(get(), get()) }
    single { AnalyticsService(get(), get()) }
    single { UserDataService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SupportService() }
    single { MetricsService(get(), get(), get(), get()) }

    single<CommandAction> { StartCommand(get(), get(), get()) }
    single<CommandAction> { LessonCommand(get(), get(), get()) }
    single<CommandAction> { HomeworkCommand(get(), get()) }
    single<CommandAction> { ProgressCommand(get(), get(), get()) }
    single<CommandAction> { HelpCommand(get(), get()) }
    single<CommandAction> { VocabularyCommand(get(), get(), get()) }
    single<CommandAction> { MenuCommand(get(), get(), get()) }
    single<CommandAction> { LessonsCommand(get(), get(), get(), get()) }
    single<CommandAction> { PracticeCommand(get(), get()) }
    single<CommandAction> { DictionaryCommand(get(), get(), get()) }
    single<CommandAction> { ReviewCommand(get(), get()) }
    single<CommandAction> { RemindersCommand(get(), get(), get()) }
    single<CommandAction> { ResetCommand(get(), get()) }
    single<CommandAction> { DeleteCommand(get(), get()) }
    single<CommandAction> { ExportCommand(get(), get(), get()) }
    single<CommandAction> { SupportCommand(get(), get(), get()) }

    single<CommandTextAction> { DictionaryQueryTextAction(get(), get(), get()) }
    single<CommandTextAction> { DictionaryCustomTextAction(get(), get(), get()) }

    single { CommandHandler(getAll<CommandAction>(), getAll<CommandTextAction>()) }

    single<HomeworkTextAction> { HomeworkTextAnswerAction(get(), get(), get(), get(), get(), get()) }
    single { HomeworkHandler(getAll<HomeworkTextAction>()) }

    single<CallbackAction> { LessonAction(get()) }
    single<CallbackAction> { LessonsListAction(get(), get(), get(), get()) }
    single<CallbackAction> { LessonStartAction(get(), get(), get(), get()) }
    single<CallbackAction> { LessonPracticeAction(get(), get(), get(), get(), get()) }
    single<CallbackAction> { PracticeStartAction(get(), get(), get(), get(), get()) }
    single<CallbackAction> { NextLessonAction(get(), get()) }
    single<CallbackAction> { VocabularyAction(get(), get(), get()) }
    single<CallbackAction> { VocabListAction(get(), get(), get()) }
    single<CallbackAction> { VocabWordAction(get(), get(), get()) }
    single<CallbackAction> { VocabAddAllAction(get(), get(), get()) }
    single<CallbackAction> { VocabAddAction(get(), get(), get()) }
    single<CallbackAction> { VocabRemoveAction(get(), get(), get()) }
    single<CallbackAction> { HomeworkAction(get(), get()) }
    single<CallbackAction> { StartHomeworkAction(get(), get(), get(), get()) }
    single<CallbackAction> { AnswerAction(get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { HomeworkNextAction(get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { HomeworkAddDictAction(get(), get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { NextHomeworkAction(get(), get(), get()) }
    single<CallbackAction> { ProgressAction(get(), get(), get()) }
    single<CallbackAction> { SettingsAction() }
    single<CallbackAction> { ResetProgressAction() }
    single<CallbackAction> { ConfirmResetAction(get(), get(), get(), get(), get()) }
    single<CallbackAction> { ConfirmDeleteAction(get(), get()) }
    single<CallbackAction> { SelectLevelAction() }
    single<CallbackAction> { SetLevelAction() }
    single<CallbackAction> { KnowledgeTestAction() }
    single<CallbackAction> { HelpAction() }
    single<CallbackAction> { BackToMenuAction(get(), get()) }
    single<CallbackAction> { ContinueAction(get(), get(), get(), get()) }
    single<CallbackAction> { DictionaryPromptAction(get(), get()) }
    single<CallbackAction> { DictListAction(get(), get()) }
    single<CallbackAction> { DictAddCustomAction(get(), get()) }
    single<CallbackAction> { DictFavAction(get(), get(), get()) }
    single<CallbackAction> { DictTagsAction(get()) }
    single<CallbackAction> { DictTagAction(get(), get()) }
    single<CallbackAction> { ReviewStartAction(get()) }
    single<CallbackAction> { ReviewDifficultyAction(get(), get(), get(), get()) }
    single<CallbackAction> { ReviewSessionAnswerAction(get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { ReviewSessionNextAction(get(), get()) }
    single<CallbackAction> { ReviewAnswerAction(get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { SetReminderAction(get(), get()) }
    single<CallbackAction> { RemindersAction(get(), get()) }
    single<CallbackAction> { ReminderFrequencyAction(get(), get()) }
    single<CallbackAction> { ReminderDayToggleAction() }
    single<CallbackAction> { ReminderDaysConfirmAction() }
    single<CallbackAction> { ReminderTimeAction(get(), get(), get()) }
    single<CallbackAction> { ReminderEnableWeekdaysAction(get(), get(), get()) }
    single<CallbackAction> { ReminderDisableAction(get(), get()) }
    single<CallbackAction> { ExerciseAnswerAction(get(), get(), get()) }
    single<CallbackAction> { ExerciseAddDictAction(get(), get(), get(), get(), get(), get()) }
    single<CallbackAction> { ExerciseNextAction(get(), get(), get(), get(), get()) }
    single { CallbackHandler(getAll()) }
}
