package com.turki.bot.handler.command.lesson

internal fun extractLessonTopic(title: String): String {
    val first = title.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    val cleaned = first.trim { ch -> !ch.isLetterOrDigit() }
    return if (cleaned.isNotBlank()) cleaned else title.take(20)
}
