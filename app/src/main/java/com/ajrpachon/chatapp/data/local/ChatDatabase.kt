package com.ajrpachon.chatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.dao.ChatEventDao
import com.ajrpachon.chatapp.data.local.dao.ScheduledMessageDao
import com.ajrpachon.chatapp.data.local.dao.SessionDao
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.FolderDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.MessageReadReceiptDao
import com.ajrpachon.chatapp.data.local.dao.PollDao
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.data.local.entity.ChatEventDBO
import com.ajrpachon.chatapp.data.local.entity.EventRsvpDBO
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.local.entity.FolderConversationDBO
import com.ajrpachon.chatapp.data.local.entity.FolderDBO
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.data.local.entity.PollVoteDBO
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.local.entity.MessageReadReceiptDBO
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO

@Database(
    entities = [
        UserDBO::class,
        ConversationDBO::class,
        MessageDBO::class,
        InvitationDBO::class,
        GroupMemberDBO::class,
        ReactionDBO::class,
        StatusDBO::class,
        PollDBO::class,
        PollOptionDBO::class,
        PollVoteDBO::class,
        StickerPackDBO::class,
        StickerDBO::class,
        MessageReadReceiptDBO::class,
        FolderDBO::class,
        FolderConversationDBO::class,
        BroadcastListDBO::class,
        BroadcastListMemberDBO::class,
        ChatEventDBO::class,
        EventRsvpDBO::class,
        SessionDBO::class,
        ScheduledMessageDBO::class,
    ],
    version = 32,
    exportSchema = true,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun invitationDao(): InvitationDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun reactionDao(): ReactionDao
    abstract fun statusDao(): StatusDao
    abstract fun pollDao(): PollDao
    abstract fun stickerPackDao(): StickerPackDao
    abstract fun messageReadReceiptDao(): MessageReadReceiptDao
    abstract fun folderDao(): FolderDao
    abstract fun broadcastListDao(): BroadcastListDao
    abstract fun chatEventDao(): ChatEventDao
    abstract fun sessionDao(): SessionDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
}
