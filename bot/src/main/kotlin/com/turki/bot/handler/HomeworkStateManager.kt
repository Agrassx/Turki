package com.turki.bot.handler

object HomeworkStateManager {
    private val currentQuestions = mutableMapOf<Long, Pair<Int, Int>>()
    private val userAnswers = mutableMapOf<Long, MutableMap<Int, String>>()
    private val questionMessageIds = mutableMapOf<Long, Long>()

    fun setCurrentQuestion(telegramId: Long, homeworkId: Int, questionId: Int, messageId: Long? = null) {
        currentQuestions[telegramId] = homeworkId to questionId
        if (messageId != null) {
            questionMessageIds[telegramId] = messageId
        }
    }

    fun getCurrentQuestion(telegramId: Long): Pair<Int, Int>? = currentQuestions[telegramId]

    fun clearCurrentQuestion(telegramId: Long) {
        currentQuestions.remove(telegramId)
        questionMessageIds.remove(telegramId)
    }

    fun getQuestionMessageId(telegramId: Long): Long? = questionMessageIds[telegramId]

    fun setAnswers(telegramId: Long, answers: MutableMap<Int, String>) {
        userAnswers[telegramId] = answers
    }

    fun getAnswers(telegramId: Long): MutableMap<Int, String> {
        return userAnswers.getOrPut(telegramId) { mutableMapOf() }
    }

    fun clearState(telegramId: Long) {
        clearCurrentQuestion(telegramId)
        userAnswers.remove(telegramId)
    }
}
