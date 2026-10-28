package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val encryptedContent: String,
    val iv: String,
    val plainSnippet: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
