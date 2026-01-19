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
    fun vocabularyPronunciation(pronunciation: String): String
    fun vocabularyExample(example: String): String
    val homeworkNotReady: String
    val homeworkAlreadyCompleted: String
    fun questionTitle(index: Int): String
    val writeYourAnswer: String
    fun homeworkComplete(score: Int, maxScore: Int): String
    fun homeworkResult(score: Int, maxScore: Int): String
    fun progress(firstName: String, completedLessons: Int, totalLessons: Int, subscriptionActive: Boolean): String
    val settingsTitle: String
    val resetProgressConfirm: String
    val progressResetSuccess: String
    val selectLevelTitle: String
    val levelA1Active: String
    fun levelLocked(level: String): String
    val knowledgeTestTitle: String
    val mainMenuTitle: String
    val btnStartLesson: String
    val btnHomework: String
    val btnProgress: String
    val btnSelectLevel: String
    val btnKnowledgeTest: String
    val btnSettings: String
    val btnVocabulary: String
    val btnGoToHomework: String
    val btnSetReminder: String
    val btnStartHomework: String
    val btnNextLesson: String
    val btnTryAgain: String
    val btnResetProgress: String
    val btnBackToMenu: String
    val btnConfirmReset: String
    val btnCancel: String
    val btnBack: String
    val btnContinueLesson: String
    fun btnLevelWithStatus(level: String, isActive: Boolean): String
    val reminderLesson: String
    val reminderHomework: String
    val reminderSubscription: String
}
