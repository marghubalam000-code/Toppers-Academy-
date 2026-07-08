package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String = "", // "TA-" followed by unique sequence
    val name: String,
    val fatherName: String = "",
    val motherName: String = "",
    val mobile: String = "",
    val email: String = "",
    val studentClass: String = "", // Maps to Firestore field "class"
    val section: String = "",
    val rollNumber: String = "",
    val gender: String = "",
    val dob: String = "",
    val address: String = "",
    val aadhaar: String = "",
    val admissionDate: String = "",
    val photo: String = "", // Student Photo (Base64 representation or path)
    val status: String = "Active", // "Active" or "Inactive"
    val password: String = "", // Generated password for student login
    val mainSubject: String = "", // Main subject of the student
    val fatherMobile: String = "" // Father's mobile number
) : Serializable
