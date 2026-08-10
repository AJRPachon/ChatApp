package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observe(): Flow<ThemePreference>
    suspend fun set(theme: ThemePreference)
}
