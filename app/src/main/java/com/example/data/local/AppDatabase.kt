package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.data.model.FeeRecord
import com.example.data.model.Teacher
import com.example.data.model.ExamMark
import com.example.data.model.ChatMessage
import com.example.data.model.SmsLog

@Database(
    entities = [
        Student::class,
        AttendanceRecord::class,
        FeeRecord::class,
        Teacher::class,
        ExamMark::class,
        ChatMessage::class,
        SmsLog::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun feeDao(): FeeDao
    abstract fun teacherDao(): TeacherDao
    abstract fun examMarkDao(): ExamMarkDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun smsLogDao(): SmsLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "toppers_attendance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
