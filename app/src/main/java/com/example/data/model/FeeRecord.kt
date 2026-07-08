package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "fee_records")
data class FeeRecord(
    @PrimaryKey val feeId: String = "", // UUID or unique Firestore ID
    val studentId: String = "", // Links to Student.studentId
    val studentName: String = "",
    val title: String = "", // e.g., "July 2026 Monthly Fee", "Exam Fee"
    val amount: Double = 0.0,
    val dueDate: String = "", // "YYYY-MM-DD"
    val status: String = "Unpaid", // "Unpaid", "Pending", "Paid", "Rejected"
    val paymentDate: String = "", // date when student submitted payment
    val transactionId: String = "", // UPI reference ID
    val upiUsed: String = "", // UPI address paid to
    val screenshotUrl: String = "", // URL to proof screenshot
    val rejectionReason: String = "", // if rejected by admin
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable
