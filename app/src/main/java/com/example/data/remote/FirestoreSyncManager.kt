package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {
    private var firestore: FirebaseFirestore? = null
    
    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.LocalOnly("Firebase not configured"))
    val syncStatus: StateFlow<SyncState> = _syncStatus

    private fun configureOfflinePersistence(fs: FirebaseFirestore) {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB of local offline storage
                    .build())
                .build()
            fs.firestoreSettings = settings
            Log.d("FirestoreSyncManager", "Firestore offline persistence (100MB persistent cache) configured successfully.")
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Failed to configure modern local cache settings: ${e.message}. Trying legacy persistence.")
            try {
                @Suppress("DEPRECATION")
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                fs.firestoreSettings = settings
                Log.d("FirestoreSyncManager", "Firestore legacy offline persistence enabled successfully.")
            } catch (ex: Exception) {
                Log.e("FirestoreSyncManager", "Failed to configure fallback legacy persistence: ${ex.message}", ex)
            }
        }
    }

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val fs = FirebaseFirestore.getInstance()
                configureOfflinePersistence(fs)
                firestore = fs
                _syncStatus.value = SyncState.Connected
                Log.d("FirestoreSyncManager", "Firebase Firestore initialized successfully.")
            } else {
                FirebaseApp.initializeApp(context)
                val fs = FirebaseFirestore.getInstance()
                configureOfflinePersistence(fs)
                firestore = fs
                _syncStatus.value = SyncState.Connected
                Log.d("FirestoreSyncManager", "Firebase Firestore initialized with dynamic default.")
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncState.LocalOnly(e.message ?: "Firebase initialization failed")
            Log.w("FirestoreSyncManager", "Running in offline/local-only mode: ${e.message}")
        }
    }

    fun isCloudAvailable(): Boolean = firestore != null

    suspend fun syncStudent(student: Student): Boolean {
        val fs = firestore
        if (fs == null) {
            Log.d("FirestoreSyncManager", "Simulated cloud student sync for: ${student.name}")
            return true
        }

        return try {
            val docId = student.studentId.ifEmpty { "student_${student.id}" }
            val data = hashMapOf(
                "studentId" to docId,
                "name" to student.name,
                "fatherName" to student.fatherName,
                "motherName" to student.motherName,
                "class" to student.studentClass,
                "section" to student.section,
                "rollNumber" to student.rollNumber,
                "mobile" to student.mobile,
                "email" to student.email,
                "gender" to student.gender,
                "dob" to student.dob,
                "address" to student.address,
                "aadhaar" to student.aadhaar,
                "admissionDate" to student.admissionDate,
                "photo" to student.photo,
                "status" to student.status,
                "password" to student.password,
                "mainSubject" to student.mainSubject
            )
            fs.collection("students")
                .document(docId)
                .set(data)
                .await()
            Log.d("FirestoreSyncManager", "Successfully synced student $docId to Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing student to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteStudentRemote(studentId: String): Boolean {
        val fs = firestore ?: return true
        return try {
            fs.collection("students").document(studentId).delete().await()
            Log.d("FirestoreSyncManager", "Successfully deleted student $studentId from Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error deleting student from Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun syncAttendance(record: AttendanceRecord): Boolean {
        val fs = firestore
        if (fs == null) {
            Log.d("FirestoreSyncManager", "Simulated cloud sync for ${record.studentName} - ${record.date}: ${record.status}")
            return true
        }

        return try {
            val docId = record.attendanceId.ifEmpty { "${record.studentId}_${record.date}" }
            val data = hashMapOf(
                "attendanceId" to docId,
                "studentId" to record.studentId,
                "studentName" to record.studentName,
                "class" to record.studentClass,
                "section" to record.section,
                "rollNumber" to record.rollNumber,
                "date" to record.date,
                "status" to record.status,
                "createdAt" to record.createdAt
            )
            fs.collection("attendance")
                .document(docId)
                .set(data)
                .await()
            Log.d("FirestoreSyncManager", "Successfully synced attendance record $docId to Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing attendance to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAttendanceRemote(attendanceId: String): Boolean {
        val fs = firestore ?: return true
        return try {
            fs.collection("attendance").document(attendanceId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun listenToAppLogo(onLogoChanged: (String) -> Unit) {
        val fs = firestore ?: return
        try {
            fs.collection("settings").document("app_config")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val logoUrl = snapshot.getString("app_logo_uri") ?: ""
                        onLogoChanged(logoUrl)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register app logo listener: ${ex.message}")
        }
    }

    fun listenToStudentAttendance(studentId: String, onNewRecord: (AttendanceRecord) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to student attendance failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (docChange in snapshot.documentChanges) {
                            if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                docChange.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                val doc = docChange.document
                                try {
                                    val record = AttendanceRecord(
                                        attendanceId = doc.getString("attendanceId") ?: doc.id,
                                        studentId = doc.getString("studentId") ?: "",
                                        studentName = doc.getString("studentName") ?: "",
                                        studentClass = doc.getString("class") ?: "",
                                        section = doc.getString("section") ?: "",
                                        rollNumber = doc.getString("rollNumber") ?: "",
                                        date = doc.getString("date") ?: "",
                                        status = doc.getString("status") ?: "Present",
                                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                        isSynced = true
                                    )
                                    onNewRecord(record)
                                } catch (ex: Exception) {
                                    Log.e("FirestoreSyncManager", "Error parsing attendance record: ${ex.message}")
                                }
                            }
                        }
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register student attendance listener: ${ex.message}")
            null
        }
    }

    suspend fun updateAppLogoRemote(logoUrl: String): Boolean {
        val fs = firestore ?: return false
        return try {
            val data = hashMapOf("app_logo_uri" to logoUrl)
            fs.collection("settings").document("app_config").set(data).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error updating app logo remote: ${e.message}", e)
            false
        }
    }

    suspend fun syncFeeRecord(fee: com.example.data.model.FeeRecord): Boolean {
        val fs = firestore ?: return true
        return try {
            val data = hashMapOf(
                "feeId" to fee.feeId,
                "studentId" to fee.studentId,
                "studentName" to fee.studentName,
                "title" to fee.title,
                "amount" to fee.amount,
                "dueDate" to fee.dueDate,
                "status" to fee.status,
                "paymentDate" to fee.paymentDate,
                "transactionId" to fee.transactionId,
                "upiUsed" to fee.upiUsed,
                "screenshotUrl" to fee.screenshotUrl,
                "rejectionReason" to fee.rejectionReason,
                "lastUpdated" to fee.lastUpdated
            )
            fs.collection("fee_records")
                .document(fee.feeId)
                .set(data)
                .await()
            Log.d("FirestoreSyncManager", "Successfully synced fee record ${fee.feeId} to Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing fee record to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteFeeRemote(feeId: String): Boolean {
        val fs = firestore ?: return true
        return try {
            fs.collection("fee_records").document(feeId).delete().await()
            Log.d("FirestoreSyncManager", "Successfully deleted fee record $feeId from Firestore.")
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error deleting fee record from Firestore: ${e.message}", e)
            false
        }
    }

    fun listenToAllStudents(onStudentListChanged: (List<Student>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("students")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to all students failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<Student>()
                        for (doc in snapshot.documents) {
                            try {
                                val student = Student(
                                    id = 0,
                                    studentId = doc.getString("studentId") ?: doc.id,
                                    name = doc.getString("name") ?: "",
                                    fatherName = doc.getString("fatherName") ?: "",
                                    motherName = doc.getString("motherName") ?: "",
                                    mobile = doc.getString("mobile") ?: "",
                                    email = doc.getString("email") ?: "",
                                    studentClass = doc.getString("class") ?: "",
                                    section = doc.getString("section") ?: "",
                                    rollNumber = doc.getString("rollNumber") ?: "",
                                    gender = doc.getString("gender") ?: "",
                                    dob = doc.getString("dob") ?: "",
                                    address = doc.getString("address") ?: "",
                                    aadhaar = doc.getString("aadhaar") ?: "",
                                    admissionDate = doc.getString("admissionDate") ?: "",
                                    photo = doc.getString("photo") ?: "",
                                    status = doc.getString("status") ?: "Active",
                                    password = doc.getString("password") ?: "",
                                    mainSubject = doc.getString("mainSubject") ?: ""
                                )
                                list.add(student)
                            } catch (ex: Exception) {
                                Log.e("FirestoreSyncManager", "Error parsing student: ${ex.message}")
                            }
                        }
                        onStudentListChanged(list)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register students listener: ${ex.message}")
            null
        }
    }

    fun listenToAllAttendance(onAttendanceListChanged: (List<AttendanceRecord>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("attendance")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to all attendance failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<AttendanceRecord>()
                        for (doc in snapshot.documents) {
                            try {
                                val record = AttendanceRecord(
                                    id = 0,
                                    attendanceId = doc.getString("attendanceId") ?: doc.id,
                                    studentId = doc.getString("studentId") ?: "",
                                    studentName = doc.getString("studentName") ?: "",
                                    studentClass = doc.getString("class") ?: "",
                                    section = doc.getString("section") ?: "",
                                    rollNumber = doc.getString("rollNumber") ?: "",
                                    date = doc.getString("date") ?: "",
                                    status = doc.getString("status") ?: "Present",
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    isSynced = true
                                )
                                list.add(record)
                            } catch (ex: Exception) {
                                Log.e("FirestoreSyncManager", "Error parsing attendance: ${ex.message}")
                            }
                        }
                        onAttendanceListChanged(list)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register attendance listener: ${ex.message}")
            null
        }
    }

    fun listenToAllFeeRecords(onFeeListChanged: (List<com.example.data.model.FeeRecord>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("fee_records")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to all fee records failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<com.example.data.model.FeeRecord>()
                        for (doc in snapshot.documents) {
                            try {
                                val fee = com.example.data.model.FeeRecord(
                                    feeId = doc.getString("feeId") ?: doc.id,
                                    studentId = doc.getString("studentId") ?: "",
                                    studentName = doc.getString("studentName") ?: "",
                                    title = doc.getString("title") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    dueDate = doc.getString("dueDate") ?: "",
                                    status = doc.getString("status") ?: "Unpaid",
                                    paymentDate = doc.getString("paymentDate") ?: "",
                                    transactionId = doc.getString("transactionId") ?: "",
                                    upiUsed = doc.getString("upiUsed") ?: "",
                                    screenshotUrl = doc.getString("screenshotUrl") ?: "",
                                    rejectionReason = doc.getString("rejectionReason") ?: "",
                                    lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                                )
                                list.add(fee)
                            } catch (ex: Exception) {
                                Log.e("FirestoreSyncManager", "Error parsing fee record: ${ex.message}")
                            }
                        }
                        onFeeListChanged(list)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register all fee records listener: ${ex.message}")
            null
        }
    }

    fun listenToStudentFeeRecords(studentId: String, onFeeListChanged: (List<com.example.data.model.FeeRecord>) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("fee_records")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to student fee records failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = mutableListOf<com.example.data.model.FeeRecord>()
                        for (doc in snapshot.documents) {
                            try {
                                val fee = com.example.data.model.FeeRecord(
                                    feeId = doc.getString("feeId") ?: doc.id,
                                    studentId = doc.getString("studentId") ?: "",
                                    studentName = doc.getString("studentName") ?: "",
                                    title = doc.getString("title") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    dueDate = doc.getString("dueDate") ?: "",
                                    status = doc.getString("status") ?: "Unpaid",
                                    paymentDate = doc.getString("paymentDate") ?: "",
                                    transactionId = doc.getString("transactionId") ?: "",
                                    upiUsed = doc.getString("upiUsed") ?: "",
                                    screenshotUrl = doc.getString("screenshotUrl") ?: "",
                                    rejectionReason = doc.getString("rejectionReason") ?: "",
                                    lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()
                                )
                                list.add(fee)
                            } catch (ex: Exception) {
                                Log.e("FirestoreSyncManager", "Error parsing fee record: ${ex.message}")
                            }
                        }
                        onFeeListChanged(list)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register student fee records listener: ${ex.message}")
            null
        }
    }

    fun listenToPaymentConfig(onConfigChanged: (String, String, String) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val fs = firestore ?: return null
        return try {
            fs.collection("settings").document("payment_config")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("FirestoreSyncManager", "Listen to payment config failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val upiId = snapshot.getString("upi_id") ?: ""
                        val upiName = snapshot.getString("upi_name") ?: ""
                        val upiQrUrl = snapshot.getString("upi_qr_url") ?: ""
                        onConfigChanged(upiId, upiName, upiQrUrl)
                    }
                }
        } catch (ex: Exception) {
            Log.e("FirestoreSyncManager", "Failed to register payment config listener: ${ex.message}")
            null
        }
    }

    suspend fun updatePaymentConfigRemote(upiId: String, upiName: String, upiQrUrl: String): Boolean {
        val fs = firestore ?: return false
        return try {
            val data = hashMapOf(
                "upi_id" to upiId,
                "upi_name" to upiName,
                "upi_qr_url" to upiQrUrl
            )
            fs.collection("settings").document("payment_config").set(data).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error updating payment config remote: ${e.message}", e)
            false
        }
    }
}

sealed interface SyncState {
    object Connected : SyncState
    data class LocalOnly(val reason: String) : SyncState
    object Syncing : SyncState
}
