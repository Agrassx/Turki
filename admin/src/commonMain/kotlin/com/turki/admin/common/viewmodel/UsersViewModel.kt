package com.turki.admin.common.viewmodel

import com.turki.admin.common.api.AdminApi
import com.turki.admin.common.domain.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsersState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class UsersViewModel(
    private val adminApi: AdminApi
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(UsersState())
    val state: StateFlow<UsersState> = _state.asStateFlow()

    fun loadUsers() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val users = adminApi.getUsers()
                _state.update { it.copy(users = users, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleSubscription(user: User) {
        scope.launch {
            try {
                adminApi.toggleSubscription(user.id, !user.subscriptionActive)
                loadUsers()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun resetUserProgress(user: User) {
        scope.launch {
            try {
                adminApi.resetUserProgress(user.id)
                loadUsers()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun resetAllProgress() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                adminApi.resetAllProgress()
                loadUsers()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
