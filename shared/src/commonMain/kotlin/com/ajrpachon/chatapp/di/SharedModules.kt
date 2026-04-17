package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.data.repository.ConversationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.InvitationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.MessageRepositoryImpl
import com.ajrpachon.chatapp.data.repository.UserRepositoryImpl
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveMessagesUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import org.koin.dsl.module

val remoteModule = module {
    single { UserRemoteSource(get()) }
    single { MessageRemoteSource(get()) }
    single { InvitationRemoteSource(get()) }
}

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
    single<ConversationRepository> { ConversationRepositoryImpl(get(), get(), get(), get()) }
    // currentUserId resolved at runtime — provided by AppModule
    factory<MessageRepository> { (currentUserId: String) ->
        MessageRepositoryImpl(get(), get(), get(), currentUserId)
    }
    single<InvitationRepository> { InvitationRepositoryImpl(get(), get(), get()) }
}

val useCaseModule = module {
    factory { GetCurrentUserUseCase(get()) }
    factory { SetUsernameUseCase(get()) }
    factory { ObserveConversationsUseCase(get()) }
    factory { ObserveMessagesUseCase(get()) }
    factory { SendMessageUseCase(get()) }
    factory { ObserveInvitationsUseCase(get()) }
    factory { RespondInvitationUseCase(get()) }
}

val sharedModules = listOf(remoteModule, repositoryModule, useCaseModule)
