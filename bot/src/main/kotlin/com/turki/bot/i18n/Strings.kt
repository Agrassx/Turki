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
    fun vocabularyWordTitle(word: String, translation: String): String
    fun vocabularyItem(word: String, translation: String): String
    fun vocabularyPronunciation(pronunciation: String): String
    fun vocabularyExample(example: String): String
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
    fun reviewCardTitle(word: String): String
    fun reviewCardTranslation(translation: String): String
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
    fun reminderEnabled(days: String, time: String): String
    val reminderDisabled: String
    fun weeklyReport(lessons: Int, practice: Int, review: Int, homework: Int): String
    val menuTitle: String
    val continueNothing: String
    val deleteDataConfirm: String
    val deleteDataSuccess: String
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
}
