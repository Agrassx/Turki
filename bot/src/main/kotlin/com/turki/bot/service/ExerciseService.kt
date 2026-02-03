package com.turki.bot.service

import com.turki.core.domain.VocabularyItem
import kotlin.random.Random

data class ExerciseItem(
    val vocabularyId: Int,
    val prompt: String,
    val options: List<String>,
    val correctOption: String,
    val explanation: String
)

class ExerciseService(
    private val lessonService: LessonService
) {
    suspend fun buildLessonExercises(lessonId: Int, limit: Int = 3): List<ExerciseItem> {
        val vocabulary = lessonService.getVocabulary(lessonId)
        if (vocabulary.isEmpty()) {
            return emptyList()
        }

        val target = vocabulary.shuffled().take(limit)
        return target.map { item ->
            val options = buildOptions(item, vocabulary)
            ExerciseItem(
                vocabularyId = item.id,
                prompt = "Переведи слово: ${item.word}",
                options = options,
                correctOption = item.translation,
                explanation = "«${item.word}» = «${item.translation}»"
            )
        }
    }

    private fun buildOptions(item: VocabularyItem, vocabulary: List<VocabularyItem>): List<String> {
        val distractors = vocabulary
            .filter { it.id != item.id }
            .map { it.translation }
            .distinct()
            .shuffled()
            .take(3)
        return (distractors + item.translation).shuffled(Random(item.id))
    }
}
