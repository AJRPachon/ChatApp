package com.ajrpachon.chatapp.ui.userinfo
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserInfoViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UserInfoState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = catchResult { userRepository.getUserById(userId) }.getOrNull()
            _state.update {
                it.copy(
                    displayName = user?.displayName ?: "",
                    username = user?.username ?: "",
                    avatarUrl = user?.avatarUrl,
                    isLoading = false,
                )
            }
        }
    }
}
