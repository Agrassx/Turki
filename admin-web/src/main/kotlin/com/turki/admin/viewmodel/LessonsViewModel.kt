package com.turki.admin.viewmodel

import com.turki.core.domain.Lesson
import com.turki.core.repository.LessonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LessonsState(
    val lessons: List<Lesson> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LessonsViewModel(
    private val lessonRepository: LessonRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(LessonsState())
    val state: StateFlow<LessonsState> = _state.asStateFlow()

    fun loadLessons() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lessons = lessonRepository.findAll().map { lesson ->
                    val vocabulary = lessonRepository.getVocabularyItems(lesson.id)
                    lesson.copy(vocabularyItems = vocabulary)
                }
                _state.update { it.copy(lessons = lessons, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
