package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teacherId: String = "", // e.g. "T-101"
    val name: String = "",
    val assignedClass: String = "", // e.g. "Class 10"
    val assignedSection: String = "", // e.g. "A"
    val mobile: String = "",
    val email: String = "",
    val password: String = "123456", // Default password
    val status: String = "Active",
    val subject: String = "",
    val photoPath: String = ""
) : Serializable
