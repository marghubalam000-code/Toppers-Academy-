package com.example.data.repository

import com.example.data.local.StudentDao
import com.example.data.local.AttendanceDao
import com.example.data.local.FeeDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.data.model.FeeRecord
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.SupabaseSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import android.util.Log

class AttendanceRepository(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val feeDao: FeeDao,
    private val syncManager: FirestoreSyncManager,
    private val supabaseSyncManager: SupabaseSyncManager
) {
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val studentCount: Flow<Int> = studentDao.getStudentCountFlow()
    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()
    val allFees: Flow<List<FeeRecord>> = feeDao.getAllFeesFlow()
    val unsyncedRecords: Flow<List<AttendanceRecord>> = attendanceDao.getUnsyncedRecords()
    val syncStatus = syncManager.syncStatus
    val supabaseSyncStatus = supabaseSyncManager.syncStatus

    fun getFeesForStudent(studentId: String): Flow<List<FeeRecord>> {
        return feeDao.getFeesForStudentFlow(studentId)
    }

    suspend fun getFeeById(feeId: String): FeeRecord? {
        return feeDao.getFeeById(feeId)
    }

    suspend fun insertFee(fee: FeeRecord): Long {
        val result = feeDao.insertFee(fee)
        syncManager.syncFeeRecord(fee)
        return result
    }

    suspend fun updateFee(fee: FeeRecord) {
        feeDao.updateFee(fee)
        syncManager.syncFeeRecord(fee)
    }

    suspend fun deleteFee(fee: FeeRecord) {
        feeDao.deleteFee(fee)
        syncManager.deleteFeeRemote(fee.feeId)
    }

    suspend fun insertFeesLocal(fees: List<FeeRecord>) {
        feeDao.insertFees(fees)
    }

    suspend fun insertStudentsLocal(students: List<Student>) {
        val existingStudents = studentDao.getAllStudentsList().associateBy { it.studentId }
        val studentsToInsert = students.map { remoteStudent ->
            val existing = existingStudents[remoteStudent.studentId]
            if (existing != null) {
                remoteStudent.copy(id = existing.id)
            } else {
                remoteStudent.copy(id = 0)
            }
        }
        studentsToInsert.forEach { studentDao.insertStudent(it) }
    }

    suspend fun insertAttendanceRecordsLocal(records: List<AttendanceRecord>) {
        val existingRecords = attendanceDao.getAllAttendanceList().associateBy { it.attendanceId }
        val recordsToInsert = records.map { remoteRecord ->
            val existing = existingRecords[remoteRecord.attendanceId]
            if (existing != null) {
                remoteRecord.copy(id = existing.id)
            } else {
                remoteRecord.copy(id = 0)
            }
        }
        recordsToInsert.forEach { attendanceDao.insertAttendanceRecord(it) }
    }

    fun getAttendanceForDate(dateString: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceForDate(dateString)
    }

    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceForStudent(studentId)
    }

    suspend fun getStudentById(id: Int): Student? {
        return studentDao.getStudentById(id)
    }

    suspend fun getStudentByStudentId(studentId: String): Student? {
        return studentDao.getStudentByStudentId(studentId)
    }

    suspend fun insertStudent(student: Student): Long {
        val localId = studentDao.insertStudent(student)
        val generatedPassword = if (student.password.isEmpty()) {
            val rand = java.util.Random()
            val code = 100000 + rand.nextInt(900000)
            code.toString()
        } else {
            student.password
        }
        
        val studentWithId = if (student.studentId.isEmpty()) {
            // Generate elegant ID: TA- followed by five digits based on local ID (e.g. TA-10025)
            val generatedId = "TA-${10000 + localId}"
            val updated = student.copy(id = localId.toInt(), studentId = generatedId, password = generatedPassword)
            studentDao.updateStudent(updated)
            updated
        } else {
            val updated = student.copy(id = localId.toInt(), password = generatedPassword)
            studentDao.updateStudent(updated)
            updated
        }
        
        // Sync student with Firestore and Supabase
        syncManager.syncStudent(studentWithId)
        supabaseSyncManager.syncStudent(studentWithId)
        return localId
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
        syncManager.syncStudent(student)
        supabaseSyncManager.syncStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
        syncManager.deleteStudentRemote(student.studentId)
        supabaseSyncManager.deleteStudentRemote(student.studentId)
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord): Boolean {
        // Prevent duplicates for the same student on the same date: delete existing local record
        attendanceDao.deleteAttendanceForStudentOnDate(record.studentId, record.date)
        
        // Insert locally
        val localId = attendanceDao.insertAttendanceRecord(record)
        val updatedRecord = if (record.id == 0) record.copy(id = localId.toInt()) else record
        
        // Sync with Firestore
        val isFirebaseSynced = syncManager.syncAttendance(updatedRecord)
        // Sync with Supabase
        val isSupabaseSynced = supabaseSyncManager.syncAttendance(updatedRecord)
        
        if (isFirebaseSynced && isSupabaseSynced) {
            attendanceDao.markAsSynced(listOf(updatedRecord.id))
            return true
        }
        return false
    }

    suspend fun saveMultipleAttendanceRecords(records: List<AttendanceRecord>): Int {
        var syncedCount = 0
        for (record in records) {
            val savedAndSynced = saveAttendanceRecord(record)
            if (savedAndSynced) {
                syncedCount++
            }
        }
        return syncedCount
    }

    suspend fun syncUnsyncedRecords() {
        val records = unsyncedRecords.first()
        for (record in records) {
            val isFirebaseSynced = syncManager.syncAttendance(record)
            val isSupabaseSynced = supabaseSyncManager.syncAttendance(record)
            if (isFirebaseSynced && isSupabaseSynced) {
                attendanceDao.markAsSynced(listOf(record.id))
            }
        }
    }

    suspend fun fetchAndSyncFromSupabase(): Boolean {
        val remoteStudents = supabaseSyncManager.fetchStudents()
        val remoteAttendance = supabaseSyncManager.fetchAttendance()

        if (remoteStudents != null) {
            for (student in remoteStudents) {
                val existing = studentDao.getStudentByStudentId(student.studentId)
                if (existing != null) {
                    studentDao.updateStudent(student.copy(id = existing.id))
                } else {
                    studentDao.insertStudent(student)
                }
            }
        }

        if (remoteAttendance != null) {
            for (record in remoteAttendance) {
                attendanceDao.deleteAttendanceForStudentOnDate(record.studentId, record.date)
                attendanceDao.insertAttendanceRecord(record)
            }
        }
        
        return remoteStudents != null || remoteAttendance != null
    }

    fun isCloudAvailable(): Boolean = syncManager.isCloudAvailable()
    fun isSupabaseAvailable(): Boolean = supabaseSyncManager.isSupabaseConfigured()
    suspend fun fetchAllowedEmailsFromSupabase(): List<String>? = supabaseSyncManager.fetchAllowedEmails()
    suspend fun addAllowedEmailToSupabase(email: String): Boolean = supabaseSyncManager.addAllowedEmailRemote(email)
    suspend fun removeAllowedEmailFromSupabase(email: String): Boolean = supabaseSyncManager.removeAllowedEmailRemote(email)
    fun getGoogleWebClientId(): String = supabaseSyncManager.getGoogleWebClientId()
    fun saveGoogleWebClientId(clientId: String) = supabaseSyncManager.saveGoogleWebClientId(clientId)
    suspend fun signInWithSupabaseGoogle(idToken: String): Result<String> = supabaseSyncManager.signInWithSupabaseGoogle(idToken)

    suspend fun saveAttendanceRecordFromCloud(record: AttendanceRecord) {
        attendanceDao.deleteAttendanceForStudentOnDate(record.studentId, record.date)
        attendanceDao.insertAttendanceRecord(record.copy(isSynced = true))
    }
}
