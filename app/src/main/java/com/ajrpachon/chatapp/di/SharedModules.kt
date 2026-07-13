package com.ajrpachon.chatapp.di

import com.ajrpachon.chatapp.data.repository.AuthRepositoryImpl
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.data.remote.source.CallRemoteSource
import com.ajrpachon.chatapp.data.remote.source.ConversationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.data.remote.source.GroupRemoteSource
import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.data.remote.source.ReactionRemoteSource
import com.ajrpachon.chatapp.data.remote.source.StatusRemoteSource
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.service.FcmTokenManager
import com.ajrpachon.chatapp.data.repository.CallRepositoryImpl
import com.ajrpachon.chatapp.data.repository.ContactRepositoryImpl
import com.ajrpachon.chatapp.data.repository.ConversationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.GroupRepositoryImpl
import com.ajrpachon.chatapp.data.repository.InvitationRepositoryImpl
import com.ajrpachon.chatapp.data.repository.MessageRepositoryImpl
import com.ajrpachon.chatapp.data.repository.PollRepositoryImpl
import com.ajrpachon.chatapp.data.repository.ReactionRepositoryImpl
import com.ajrpachon.chatapp.data.repository.ScheduledMessageRepositoryImpl
import com.ajrpachon.chatapp.data.repository.TypingRepositoryImpl
import com.ajrpachon.chatapp.data.repository.BroadcastListRepositoryImpl
import com.ajrpachon.chatapp.data.repository.SessionRepositoryImpl
import com.ajrpachon.chatapp.data.repository.StatusRepositoryImpl
import com.ajrpachon.chatapp.data.repository.StickerPackRepositoryImpl
import com.ajrpachon.chatapp.data.repository.UserRepositoryImpl
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.ContactRepository
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import com.ajrpachon.chatapp.domain.repository.TypingRepository
import com.ajrpachon.chatapp.domain.repository.BroadcastListRepository
import com.ajrpachon.chatapp.domain.repository.SessionRepository
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.domain.repository.StickerPackRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import org.koin.android.ext.koin.androidContext
import com.ajrpachon.chatapp.domain.usecase.AddGroupMemberUseCase
import com.ajrpachon.chatapp.domain.usecase.BlockUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCacheFileUseCase
import com.ajrpachon.chatapp.domain.usecase.GetDeviceContactsUseCase
import com.ajrpachon.chatapp.domain.usecase.CreateGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val remoteModule = module {
    singleOf(::CallRemoteSource)
    singleOf(::ConversationRemoteSource)
    singleOf(::UserRemoteSource)
    singleOf(::MessageRemoteSource)
    singleOf(::InvitationRemoteSource)
    singleOf(::GroupRemoteSource)
    singleOf(::FcmTokenRemoteSource)
    singleOf(::ReactionRemoteSource)
    singleOf(::StatusRemoteSource)
    single { FcmTokenManager(get(), get()) }
}

val repositoryModule = module {
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    singleOf(::UserRepositoryImpl) { bind<UserRepository>() }
    singleOf(::ConversationRepositoryImpl) { bind<ConversationRepository>() }
    singleOf(::MessageRepositoryImpl) { bind<MessageRepository>() }
    singleOf(::InvitationRepositoryImpl) { bind<InvitationRepository>() }
    singleOf(::GroupRepositoryImpl) { bind<GroupRepository>() }
    singleOf(::CallRepositoryImpl) { bind<CallRepository>() }
    singleOf(::ReactionRepositoryImpl) { bind<ReactionRepository>() }
    singleOf(::ScheduledMessageRepositoryImpl) { bind<ScheduledMessageRepository>() }
    singleOf(::TypingRepositoryImpl) { bind<TypingRepository>() }
    single<ContactRepository> { ContactRepositoryImpl(androidContext().contentResolver) }
    singleOf(::StatusRepositoryImpl) { bind<StatusRepository>() }
    singleOf(::BroadcastListRepositoryImpl) { bind<BroadcastListRepository>() }
    singleOf(::SessionRepositoryImpl) { bind<SessionRepository>() }
    singleOf(::StickerPackRepositoryImpl) { bind<StickerPackRepository>() }
    singleOf(::PollRepositoryImpl) { bind<PollRepository>() }
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
    factoryOf(::GetDeviceContactsUseCase)
    single { GetCacheFileUseCase(androidContext().cacheDir) }
    single { ReadUriAsBytesUseCase(androidContext().contentResolver) }
}

val sharedModules = listOf(remoteModule, repositoryModule, useCaseModule)
