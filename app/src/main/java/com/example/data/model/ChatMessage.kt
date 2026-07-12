package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String = "", // Messages belong to a student's thread
    val studentName: String = "",
    val sender: String = "", // "Parent" or "Teacher" or "Admin"
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) : Serializable
