package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "exam_marks")
data class ExamMark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String = "",
    val studentName: String = "",
    val examType: String = "", // "Unit Test", "Half-Yearly", "Final Exam"
    val subject: String = "", // e.g. "Mathematics", "Science", etc.
    val marksObtained: Double = 0.0,
    val maxMarks: Double = 100.0,
    val remarks: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable
