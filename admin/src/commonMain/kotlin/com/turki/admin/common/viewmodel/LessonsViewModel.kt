package com.turki.admin.common.viewmodel

import com.turki.admin.common.api.AdminApi
import com.turki.admin.common.domain.Lesson
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
    private val adminApi: AdminApi
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(LessonsState())
    val state: StateFlow<LessonsState> = _state.asStateFlow()

    fun loadLessons() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val lessons = adminApi.getLessons()
                _state.update { it.copy(lessons = lessons, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
