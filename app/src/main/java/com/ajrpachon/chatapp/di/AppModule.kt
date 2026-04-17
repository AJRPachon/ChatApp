package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.BuildConfig
import com.ajrpachon.chatapp.data.local.buildChatDatabase
import com.ajrpachon.chatapp.ui.auth.AuthViewModel
import com.ajrpachon.chatapp.ui.chat.ChatViewModel
import com.ajrpachon.chatapp.ui.conversations.ConversationListViewModel
import com.ajrpachon.chatapp.ui.invitations.InvitationsViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single { buildChatDatabase(androidContext()) }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().userDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().conversationDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().messageDao() }
    single { get<com.ajrpachon.chatapp.data.local.ChatDatabase>().invitationDao() }
}

val networkModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get(), BuildConfig.GOOGLE_WEB_CLIENT_ID) }
    viewModel { ConversationListViewModel(get(), get()) }
    viewModel { (conversationId: String) -> ChatViewModel(conversationId, get(), get()) }
    viewModel { InvitationsViewModel(get(), get()) }
}

val appModules = sharedModules + listOf(databaseModule, networkModule, viewModelModule)
