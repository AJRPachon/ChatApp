package com.ajrpachon.chatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO

@Database(
    entities = [UserDBO::class, ConversationDBO::class, MessageDBO::class, InvitationDBO::class, GroupMemberDBO::class, ReactionDBO::class],
    version = 17,
    exportSchema = true,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun invitationDao(): InvitationDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun reactionDao(): ReactionDao
}
