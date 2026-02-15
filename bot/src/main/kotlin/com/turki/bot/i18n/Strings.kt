package com.turki.bot.i18n

/**
 * Interface for bot message localization.
 *
 * This interface defines all text strings used by the bot, organized into categories:
 * - Welcome messages and greetings
 * - Lesson-related messages
 * - Vocabulary display
 * - Homework instructions and results
 * - Progress tracking
 * - Settings and configuration
 * - Button labels
 * - Reminder messages
 *
 * Implementations should provide localized strings for each supported language.
 * All strings support HTML formatting for Telegram's HTML parse mode.
 */
interface Strings {
    fun welcome(firstName: String): String
    val notRegistered: String
    val lessonNotFound: String
    val allLessonsCompleted: String
    val homeworkStart: String
    val help: String
    val reminderSet: String
    fun lessonTitle(orderIndex: Int, title: String): String
    val vocabularyTitle: String
    fun vocabularyForLesson(lessonId: Int): String
    val vocabularyEmpty: String
    fun vocabularyItem(word: String, translation: String): String
    val homeworkNotReady: String
    val homeworkAlreadyCompleted: String
    fun questionTitle(index: Int): String
    val writeYourAnswer: String
    fun homeworkComplete(score: Int, maxScore: Int): String
    fun homeworkResult(score: Int, maxScore: Int): String
    fun progress(
        firstName: String,
        completedLessons: Int,
        totalLessons: Int,
        subscriptionActive: Boolean,
        currentLevel: String,
        streakDays: Int
    ): String
    val settingsTitle: String
    val resetProgressConfirm: String
    val progressResetSuccess: String
    val homeworkFeedbackPerfect: String
    fun homeworkFeedbackSummary(details: String, wrongCount: Int): String
    fun homeworkCorrectAnswer(answer: String): String
    val homeworkNoNext: String
    val homeworkContinue: String
    val lessonIntro: String
    fun lessonIntroTitle(orderIndex: Int, title: String): String
    val lessonsTitle: String
    val practiceIntro: String
    val practicePrompt: String
    val exerciseNotReady: String
    fun exercisePrompt(word: String): String
    val exerciseCorrect: String
    val exerciseIncorrect: String
    val exerciseComplete: String
    val reviewIntro: String
    val reviewEmpty: String
    val reviewDone: String
    val reviewSelectDifficulty: String
    val reviewDifficultyWarmup: String
    val reviewDifficultyTraining: String
    val reviewDifficultyMarathon: String
    fun reviewProgress(current: Int, total: Int): String
    fun reviewCardTitle(word: String): String
    fun reviewCardTranslation(translation: String): String
    val reviewTranslateToTurkish: String
    val reviewTranslateToRussian: String
    val dictionaryPrompt: String
    val dictionaryEmpty: String
    val dictionaryAddPrompt: String
    val dictionaryAddFormatError: String
    val dictionaryNoResults: String
    fun dictionaryCardTitle(word: String, translation: String): String
    fun dictionaryPronunciation(pronunciation: String): String
    fun dictionaryExample(example: String): String
    fun dictionaryTags(tags: String): String
    val dictionaryTagsEmpty: String
    val dictionaryTagPrompt: String
    fun dictionaryTagsUpdated(tags: String): String
    val dictionaryFavorited: String
    val dictionaryUnfavorited: String
    fun dictionaryAddedAll(count: Int): String
    val reminderStatusOff: String
    fun reminderStatusOn(days: String, time: String): String
    fun reminderEnabled(days: String, time: String, nextReminder: String): String
    val reminderDisabled: String
    val reminderSelectFrequency: String
    val reminderFrequencyDaily: String
    val reminderFrequency1x: String
    val reminderFrequency2x: String
    val reminderFrequency3x: String
    val reminderFrequency4x: String
    val reminderSelectDays: String
    val reminderSelectTime: String
    val reminderTimeMorning: String
    val reminderTimeDay: String
    val reminderTimeEvening: String
    val reminderTimeNight: String
    fun reminderDaysSelected(count: Int, needed: Int): String
    fun weeklyReport(lessons: Int, practice: Int, review: Int, homework: Int): String
    val menuTitle: String
    val continueNothing: String
    val deleteDataConfirm: String
    val deleteDataSuccess: String
    val exportDataPreparing: String
    val exportDataReady: String
    val exportDataEmpty: String
    val selectLevelTitle: String
    val levelA1Active: String
    fun levelLocked(level: String): String
    val knowledgeTestTitle: String
    val mainMenuTitle: String
    val btnStartLesson: String
    val btnLesson: String
    val btnContinue: String
    val btnHomework: String
    val btnProgress: String
    val btnLessons: String
    val btnPractice: String
    val btnDictionary: String
    val btnReview: String
    val btnReminders: String
    val btnHelp: String
    val btnSelectLevel: String
    val btnKnowledgeTest: String
    val btnSettings: String
    val btnVocabulary: String
    val btnGoToHomework: String
    val btnStartPractice: String
    val btnStartReview: String
    val btnSetReminder: String
    val btnStartHomework: String
    val btnNextLesson: String
    val btnNext: String
    val btnRemember: String
    val btnAgain: String
    val btnRepeatTopic: String
    val btnNextHomework: String
    val btnEditTags: String
    val btnAddToDictionary: String
    val btnAddCustomWord: String
    val btnAddAllToDictionary: String
    val btnRemoveFromDictionary: String
    val btnEnableWeekdays: String
    val btnDisableReminders: String
    val btnConfigureReminders: String
    val btnMon: String
    val btnTue: String
    val btnWed: String
    val btnThu: String
    val btnFri: String
    val btnSat: String
    val btnSun: String
    val btnConfirmDays: String
    val btnTryAgain: String
    val btnResetProgress: String
    val btnBackToMenu: String
    val btnConfirmReset: String
    val btnCancel: String
    val btnConfirmDelete: String
    val btnBack: String
    val btnContinueLesson: String
    fun btnLevelWithStatus(level: String, isActive: Boolean): String
    val reminderLesson: String
    val reminderHomework: String
    val reminderSubscription: String

    // Learn words
    val btnLearnWords: String
    val learnWordsIntro: String
    val learnWordsEmpty: String
    val learnWordsDone: String
    val learnDifficultyEasy: String
    val learnDifficultyMedium: String
    val learnDifficultyHard: String
    fun learnWordsProgress(current: Int, total: Int): String
    val learnTranslateToTurkish: String
    val learnTranslateToRussian: String
    val learnChooseTurkish: String
    val learnChooseRussian: String

    // Unsubscribe / subscribe notifications
    val btnUnsubscribeWeekly: String
    val btnUnsubscribeReminders: String
    val btnResubscribeWeekly: String
    val unsubscribedWeekly: String
    val unsubscribedReminders: String
    val resubscribedWeekly: String
    val resubscribedReminders: String
    val weeklyReportsStatusOn: String
    val weeklyReportsStatusOff: String

    // Support
    val supportPrompt: String
    val supportSent: String
    val supportReply: String
    fun supportMessageToAdmin(userId: Long, username: String?, firstName: String, message: String): String
}
