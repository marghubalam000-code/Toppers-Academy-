package com.example.data.local

import androidx.room.*
import com.example.data.model.Teacher
import com.example.data.model.ExamMark
import com.example.data.model.ChatMessage
import com.example.data.model.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachersFlow(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE teacherId = :teacherId")
    suspend fun getTeacherByTeacherId(teacherId: String): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)
}

@Dao
interface ExamMarkDao {
    @Query("SELECT * FROM exam_marks WHERE studentId = :studentId ORDER BY examType ASC, subject ASC")
    fun getMarksForStudentFlow(studentId: String): Flow<List<ExamMark>>

    @Query("SELECT * FROM exam_marks ORDER BY studentName ASC, examType ASC, subject ASC")
    fun getAllMarksFlow(): Flow<List<ExamMark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamMark(mark: ExamMark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamMarks(marks: List<ExamMark>)

    @Update
    suspend fun updateExamMark(mark: ExamMark)

    @Delete
    suspend fun deleteExamMark(mark: ExamMark)

    @Query("DELETE FROM exam_marks WHERE studentId = :studentId AND examType = :examType AND subject = :subject")
    suspend fun deleteSpecificMark(studentId: String, examType: String, subject: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE studentId = :studentId ORDER BY timestamp ASC")
    fun getMessagesForStudentFlow(studentId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET isRead = 1 WHERE studentId = :studentId AND sender = :sender")
    suspend fun markMessagesAsRead(studentId: String, sender: String)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE studentId = :studentId AND sender != 'Parent'")
    suspend fun markMessagesForParentAsRead(studentId: String)

    @Delete
    suspend fun deleteMessage(message: ChatMessage)
}

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogsFlow(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLog): Long
}
