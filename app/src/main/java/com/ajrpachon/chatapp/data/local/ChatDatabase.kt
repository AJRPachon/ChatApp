package com.ajrpachon.chatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.PollDao
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.data.local.entity.PollVoteDBO
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
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
    ],
    version = 26,
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
}
