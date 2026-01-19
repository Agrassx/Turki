package com.turki.admin.viewmodel

import com.turki.core.domain.User
import com.turki.core.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

data class UsersState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class UsersViewModel(
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(UsersState())
    val state: StateFlow<UsersState> = _state.asStateFlow()

    fun loadUsers() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val users = userRepository.findAll()
                _state.update { it.copy(users = users, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun toggleSubscription(user: User) {
        scope.launch {
            val newActive = !user.subscriptionActive
            val expiresAt = if (newActive) Clock.System.now() + 30.days else null

            userRepository.updateSubscription(user.id, newActive, expiresAt)
            loadUsers()
        }
    }
}
