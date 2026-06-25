package com.ajrpachon.chatapp.ui.profile
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.utils.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val AVATARS_BUCKET = "avatars"

class ProfileViewModel(
    private val supabase: SupabaseClient,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userDao: UserDao,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                _state.update {
                    it.copy(
                        displayName = user.displayName,
                        username = user.username,
                        email = user.email,
                        avatarUrl = user.avatarUrl,
                    )
                }
            }.onFailure { e -> AppLogger.e(TAG, "Load profile failed", e) }
        }
    }

    fun onAvatarSelected(bytes: ByteArray, mimeType: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, error = null) }
            catchResult {
                val ext = if (mimeType.contains("png")) "png" else "jpg"
                val path = "$userId/avatar.$ext"

                supabase.storage[AVATARS_BUCKET].upload(path, bytes) { upsert = true }
                val url = supabase.storage[AVATARS_BUCKET].publicUrl(path)

                supabase.postgrest["profiles"].update(
                    buildJsonObject { put("avatar_url", url) }
                ) { filter { eq("id", userId) } }

                userDao.getById(userId)?.let { userDao.upsert(it.copy(avatarUrl = url)) }
                _state.update { it.copy(avatarUrl = url) }
            }.onFailure { e ->
                AppLogger.e(TAG, "Avatar upload failed", e)
                _state.update { it.copy(error = e.message ?: "Error al subir la foto") }
            }
            _state.update { it.copy(isUploadingAvatar = false) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            catchResult {
                fcmTokenManager.deleteToken()
                supabase.auth.signOut()
                userDao.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out failed", e) }
            _effect.send(ProfileEffect.NavigateToAuth)
        }
    }

    fun requestSignOutAll() {
        viewModelScope.launch { _effect.send(ProfileEffect.ShowSignOutAllConfirm) }
    }

    fun signOutAll() {
        viewModelScope.launch {
            catchResult {
                fcmTokenManager.deleteToken()
                supabase.auth.signOut(SignOutScope.GLOBAL)
                userDao.clearCurrentUser()
            }.onFailure { e -> AppLogger.e(TAG, "Sign out all failed", e) }
            _effect.send(ProfileEffect.NavigateToAuth)
        }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
