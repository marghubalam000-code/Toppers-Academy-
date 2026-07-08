package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val attendanceId: String = "", // Unique ID
    val studentId: String, // Matches Student.studentId
    val studentName: String,
    val studentClass: String, // Maps to Firestore field "class"
    val section: String,
    val rollNumber: String,
    val date: String, // "yyyy-MM-dd"
    val status: String, // "Present", "Absent", "Leave", "Holiday"
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) : Serializable
