package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String = "",
    val studentName: String = "",
    val parentMobile: String = "",
    val messageText: String = "",
    val status: String = "Delivered", // "Sent", "Delivered"
    val channel: String = "SMS", // "SMS" or "WhatsApp"
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
