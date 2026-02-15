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
import com.turki.bot.handler.callback.reminder.ResubscribeWeeklyAction
import com.turki.bot.handler.callback.reminder.UnsubscribeRemindersAction
import com.turki.bot.handler.callback.reminder.UnsubscribeWeeklyAction
import com.turki.bot.handler.callback.review.ReviewAnswerAction
import com.turki.bot.handler.callback.review.ReviewDifficultyAction
import com.turki.bot.handler.callback.review.ReviewSessionAnswerAction
import com.turki.bot.handler.callback.review.ReviewSessionNextAction
import com.turki.bot.handler.callback.learn.LearnAnswerAction
import com.turki.bot.handler.callback.learn.LearnDifficultyAction
import com.turki.bot.handler.callback.learn.LearnNextAction
import com.turki.bot.handler.callback.learn.LearnWordsAction
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
import com.turki.bot.service.ErrorNotifierService
import com.turki.bot.service.ExerciseService
import com.turki.bot.service.ProgressService
import com.turki.bot.service.ReminderPreferenceService
import com.turki.bot.service.ReminderService
import com.turki.bot.service.MetricsService
import com.turki.bot.service.LearnWordsService
import com.turki.bot.service.ReviewService
import com.turki.bot.service.SupportService
import com.turki.bot.service.UserDataService
import com.turki.bot.service.UserService
import com.turki.bot.service.UserStateService
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import org.koin.dsl.bind
import org.koin.dsl.module

val botModule = module {
    single { TimeZone.of("Europe/Moscow") }
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
    single { LearnWordsService(get(), get()) }
    single { ReminderPreferenceService(get(), get()) }
    single { AnalyticsService(get(), get()) }
    single { UserDataService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { SupportService() }
    single { MetricsService(get(), get(), get(), get()) }
    single { ErrorNotifierService() }

    single { StartCommand(get(), get(), get()) } bind CommandAction::class
    single { LessonCommand(get(), get(), get()) } bind CommandAction::class
    single { HomeworkCommand(get(), get()) } bind CommandAction::class
    single { ProgressCommand(get(), get(), get()) } bind CommandAction::class
    single { HelpCommand(get(), get()) } bind CommandAction::class
    single { VocabularyCommand(get(), get(), get()) } bind CommandAction::class
    single { MenuCommand(get(), get(), get()) } bind CommandAction::class
    single { LessonsCommand(get(), get(), get(), get()) } bind CommandAction::class
    single { PracticeCommand(get(), get()) } bind CommandAction::class
    single { DictionaryCommand(get(), get(), get()) } bind CommandAction::class
    single { ReviewCommand(get(), get()) } bind CommandAction::class
    single { RemindersCommand(get(), get(), get()) } bind CommandAction::class
    single { ResetCommand(get(), get()) } bind CommandAction::class
    single { DeleteCommand(get(), get()) } bind CommandAction::class
    single { ExportCommand(get(), get(), get()) } bind CommandAction::class
    single { SupportCommand(get(), get(), get()) } bind CommandAction::class

    single { DictionaryQueryTextAction(get(), get(), get()) } bind CommandTextAction::class
    single { DictionaryCustomTextAction(get(), get(), get()) } bind CommandTextAction::class

    single { CommandHandler(getAll<CommandAction>(), getAll<CommandTextAction>()) }

    single { HomeworkTextAnswerAction(get(), get(), get(), get(), get(), get()) } bind HomeworkTextAction::class
    single { HomeworkHandler(getAll<HomeworkTextAction>()) }

    single { LessonAction(get()) } bind CallbackAction::class
    single { LessonsListAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { LessonStartAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { LessonPracticeAction(get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { PracticeStartAction(get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { NextLessonAction(get(), get()) } bind CallbackAction::class
    single { VocabularyAction(get(), get(), get()) } bind CallbackAction::class
    single { VocabListAction(get(), get(), get()) } bind CallbackAction::class
    single { VocabWordAction(get(), get(), get()) } bind CallbackAction::class
    single { VocabAddAllAction(get(), get(), get()) } bind CallbackAction::class
    single { VocabAddAction(get(), get(), get()) } bind CallbackAction::class
    single { VocabRemoveAction(get(), get(), get()) } bind CallbackAction::class
    single { HomeworkAction(get(), get()) } bind CallbackAction::class
    single { StartHomeworkAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { AnswerAction(get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { HomeworkNextAction(get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { HomeworkAddDictAction(get(), get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { NextHomeworkAction(get(), get(), get()) } bind CallbackAction::class
    single { ProgressAction(get(), get(), get()) } bind CallbackAction::class
    single { SettingsAction() } bind CallbackAction::class
    single { ResetProgressAction() } bind CallbackAction::class
    single { ConfirmResetAction(get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { ConfirmDeleteAction(get(), get()) } bind CallbackAction::class
    single { SelectLevelAction() } bind CallbackAction::class
    single { SetLevelAction() } bind CallbackAction::class
    single { KnowledgeTestAction() } bind CallbackAction::class
    single { HelpAction() } bind CallbackAction::class
    single { BackToMenuAction(get(), get()) } bind CallbackAction::class
    single { ContinueAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { DictionaryPromptAction(get(), get()) } bind CallbackAction::class
    single { DictListAction(get(), get()) } bind CallbackAction::class
    single { DictAddCustomAction(get(), get()) } bind CallbackAction::class
    single { DictFavAction(get(), get(), get()) } bind CallbackAction::class
    single { DictTagsAction(get()) } bind CallbackAction::class
    single { DictTagAction(get(), get()) } bind CallbackAction::class
    single { ReviewStartAction(get()) } bind CallbackAction::class
    single { ReviewDifficultyAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { ReviewSessionAnswerAction(get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { ReviewSessionNextAction(get(), get()) } bind CallbackAction::class
    single { ReviewAnswerAction(get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { SetReminderAction(get(), get()) } bind CallbackAction::class
    single { RemindersAction(get(), get(), get()) } bind CallbackAction::class
    single { ReminderFrequencyAction(get(), get()) } bind CallbackAction::class
    single { ReminderDayToggleAction() } bind CallbackAction::class
    single { ReminderDaysConfirmAction() } bind CallbackAction::class
    single { ReminderTimeAction(get(), get(), get()) } bind CallbackAction::class
    single { ReminderEnableWeekdaysAction(get(), get(), get()) } bind CallbackAction::class
    single { ReminderDisableAction(get(), get()) } bind CallbackAction::class
    single { UnsubscribeWeeklyAction(get(), get()) } bind CallbackAction::class
    single { UnsubscribeRemindersAction(get(), get()) } bind CallbackAction::class
    single { ResubscribeWeeklyAction(get(), get()) } bind CallbackAction::class
    single { ExerciseAnswerAction(get(), get(), get()) } bind CallbackAction::class
    single { ExerciseAddDictAction(get(), get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { ExerciseNextAction(get(), get(), get(), get(), get()) } bind CallbackAction::class
    single { LearnWordsAction(get()) } bind CallbackAction::class
    single { LearnDifficultyAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { LearnAnswerAction(get(), get(), get(), get()) } bind CallbackAction::class
    single { LearnNextAction(get(), get()) } bind CallbackAction::class
    single { CallbackHandler(getAll()) }
}
