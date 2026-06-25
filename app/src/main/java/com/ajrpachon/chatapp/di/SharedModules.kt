package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.data.remote.source.GroupRemoteSource
import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.data.repository.CallRepositoryImpl
import com.ajrpachon.chatapp.data.repository.ConversationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.GroupRepositoryImpl
import com.ajrpachon.chatapp.data.repository.InvitationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.MessageRepositoryImpl
import com.ajrpachon.chatapp.data.repository.UserRepositoryImpl
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.AddGroupMemberUseCase
import com.ajrpachon.chatapp.domain.usecase.BlockUserUseCase
import com.ajrpachon.chatapp.domain.usecase.CreateGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveMessagesUseCase
import com.ajrpachon.chatapp.domain.usecase.PromoteGroupMemberUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.RemoveGroupMemberUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.domain.usecase.SetUsernameUseCase
import com.ajrpachon.chatapp.domain.usecase.UpdateGroupUseCase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val remoteModule = module {
    singleOf(::UserRemoteSource)
    singleOf(::MessageRemoteSource)
    singleOf(::InvitationRemoteSource)
    singleOf(::GroupRemoteSource)
    singleOf(::FcmTokenRemoteSource)
    single { FcmTokenManager(get(), get()) }
}

val repositoryModule = module {
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::ConversationRepositoryImpl) { bind<ConversationRepository>() }
    singleOf(::MessageRepositoryImpl) { bind<MessageRepository>() }
    singleOf(::InvitationRepositoryImpl) { bind<InvitationRepository>() }
    singleOf(::GroupRepositoryImpl) { bind<GroupRepository>() }
    singleOf(::CallRepositoryImpl) { bind<CallRepository>() }
}

val useCaseModule = module {
    factoryOf(::GetCurrentUserUseCase)
    factoryOf(::SetUsernameUseCase)
    factoryOf(::SearchUsersUseCase)
    factoryOf(::GetOrCreateConversationUseCase)
    factoryOf(::ObserveConversationsUseCase)
    factoryOf(::ObserveMessagesUseCase)
    factoryOf(::SendMessageUseCase)
    factoryOf(::ObserveInvitationsUseCase)
    factoryOf(::RespondInvitationUseCase)
    factoryOf(::CreateGroupUseCase)
    factoryOf(::GetGroupMembersUseCase)
    factoryOf(::UpdateGroupUseCase)
    factoryOf(::AddGroupMemberUseCase)
    factoryOf(::RemoveGroupMemberUseCase)
    factoryOf(::LeaveGroupUseCase)
    factoryOf(::PromoteGroupMemberUseCase)
    factoryOf(::SendInvitationUseCase)
    factoryOf(::BlockUserUseCase)
}

val sharedModules = listOf(remoteModule, repositoryModule, useCaseModule)
