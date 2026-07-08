package com.example.data.local

import androidx.room.*
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord): Long

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteAttendanceForDate(date: String)

    @Query("SELECT * FROM attendance_records WHERE attendanceId = :attendanceId")
    suspend fun getAttendanceByAttendanceId(attendanceId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceList(): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records WHERE date = :date AND studentId = :studentId")
    suspend fun deleteAttendanceForStudentOnDate(studentId: String, date: String)

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0")
    fun getUnsyncedRecords(): Flow<List<AttendanceRecord>>

    @Query("UPDATE attendance_records SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)
}
