package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val chatId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val tokens: Int? = null,
    val durationMs: Long? = null,
    val attachmentName: String? = null,
    val attachmentSizeBytes: Long? = null,
    val attachmentContent: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "local_models")
data class LocalModelEntity(
    @PrimaryKey val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val addedAt: Long = System.currentTimeMillis()
)
