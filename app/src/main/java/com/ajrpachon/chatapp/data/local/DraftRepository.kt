package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// This class shares its simple name with the domain interface it implements — aliased to avoid
// a same-name clash (there is no "...Impl" suffix on this one, unlike its sibling repositories).
import com.ajrpachon.chatapp.domain.repository.DraftRepository as DraftRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.draftDataStore by preferencesDataStore(name = "message_drafts")

class DraftRepository(private val context: Context) : DraftRepositoryContract {

    override suspend fun saveDraft(conversationId: String, text: String) {
        val key = stringPreferencesKey("draft_$conversationId")
        context.draftDataStore.edit { prefs ->
            if (text.isBlank()) prefs.remove(key) else prefs[key] = text
        }
    }

    override fun getDraft(conversationId: String): Flow<String> {
        val key = stringPreferencesKey("draft_$conversationId")
        return context.draftDataStore.data.map { prefs -> prefs[key] ?: "" }
    }

    override fun getAllDrafts(): Flow<Map<String, String>> =
        context.draftDataStore.data.map { prefs ->
            prefs.asMap()
                .entries
                .filter { it.key.name.startsWith("draft_") }
                .associate { entry ->
                    val convId = entry.key.name.removePrefix("draft_")
                    convId to (entry.value as? String ?: "")
                }
                .filter { it.value.isNotBlank() }
        }
}
