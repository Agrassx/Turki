package com.turki.bot.handler.callback.lesson

private const val LESSON_TOPIC_MAX_LENGTH = 25
private const val LESSON_TOPIC_TRUNCATE_LENGTH = 22

internal fun extractLessonTopic(title: String): String {
    val beforeColon = title.substringBefore(":").trim()
    return if (beforeColon.length <= LESSON_TOPIC_MAX_LENGTH) {
        beforeColon
    } else {
        beforeColon.take(LESSON_TOPIC_TRUNCATE_LENGTH) + "..."
    }
}
