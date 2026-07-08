package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.SupabaseSyncManager
import com.example.data.repository.AttendanceRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.util.FirebaseAuthHelper
import com.example.util.FirebaseStorageHelper
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val syncManager = FirestoreSyncManager(application)
    private val supabaseSyncManager = SupabaseSyncManager(application)
    private val repository = AttendanceRepository(db.studentDao(), db.attendanceDao(), db.feeDao(), syncManager, supabaseSyncManager)
    private val notificationHelper = NotificationHelper(application)
    private val sharedPrefs = application.getSharedPreferences("toppers_admin_prefs", Context.MODE_PRIVATE)

    // Formats
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    // Admin Credentials & Authentication State
    private val authHelper = FirebaseAuthHelper.getInstance(application)
    private val storageHelper = FirebaseStorageHelper.getInstance(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isAdminLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_logged_in", false))
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn

    private val _adminUsername = MutableStateFlow(sharedPrefs.getString("admin_username", "marghubalam000@gmail.com") ?: "marghubalam000@gmail.com")
    val adminUsername: StateFlow<String> = _adminUsername

    // Supabase Helpers
    val supabaseSyncState = repository.supabaseSyncStatus
    val supabaseLastError = supabaseSyncManager.lastError

    fun getSupabaseUrl(): String = supabaseSyncManager.getSupabaseUrl()
    fun getSupabaseKey(): String = supabaseSyncManager.getSupabaseKey()
    fun saveSupabaseCredentials(url: String, key: String) = supabaseSyncManager.saveCredentials(url, key)
    fun clearSupabaseCredentials() = supabaseSyncManager.clearCredentials()
    fun isSupabaseAvailable(): Boolean = supabaseSyncManager.isSupabaseConfigured()

    // Default password for simulation: "password123"
    private var adminPassword = sharedPrefs.getString("admin_password", "password123") ?: "password123"

    // Theme Preference State: "light", "dark", or "system"
    private val _themePreference = MutableStateFlow(sharedPrefs.getString("theme_pref", "system") ?: "system")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setThemePreference(pref: String) {
        if (pref in listOf("light", "dark", "system")) {
            _themePreference.value = pref
            sharedPrefs.edit().putString("theme_pref", pref).apply()
        }
    }

    // App Logo State (stores dynamic custom logo URL or local Uri string)
    private val _appLogoUri = MutableStateFlow(sharedPrefs.getString("app_logo_uri", "") ?: "")
    val appLogoUri: StateFlow<String> = _appLogoUri.asStateFlow()

    fun setAppLogoUri(uri: String) {
        val finalUriStr = if (uri.startsWith("content://")) {
            try {
                val context = getApplication<Application>()
                val sourceUri = Uri.parse(uri)
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    val file = java.io.File(context.filesDir, "custom_app_logo.png")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Uri.fromFile(file).toString()
                } ?: uri
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Failed to copy logo to internal storage", e)
                uri
            }
        } else {
            uri
        }
        _appLogoUri.value = finalUriStr
        sharedPrefs.edit().putString("app_logo_uri", finalUriStr).apply()

        // Sync uploaded school logo URL to Firestore settings collection so everyone automatically receives it
        viewModelScope.launch {
            try {
                syncManager.updateAppLogoRemote(finalUriStr)
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error syncing logo to Firestore settings: ${e.message}")
            }
        }
    }

    fun resetAppLogoUri() {
        _appLogoUri.value = ""
        sharedPrefs.edit().putString("app_logo_uri", "").apply()

        // Sync deletion to Firestore
        viewModelScope.launch {
            try {
                syncManager.updateAppLogoRemote("")
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error resetting logo in Firestore settings: ${e.message}")
            }
        }
    }

    // Marghub Signature Logo State (stores dynamic custom logo URL or local Uri string)
    private val _marghubLogoUri = MutableStateFlow(sharedPrefs.getString("marghub_logo_uri", "") ?: "")
    val marghubLogoUri: StateFlow<String> = _marghubLogoUri.asStateFlow()

    fun setMarghubLogoUri(uri: String) {
        val finalUriStr = if (uri.startsWith("content://")) {
            try {
                val context = getApplication<Application>()
                val sourceUri = Uri.parse(uri)
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    val file = java.io.File(context.filesDir, "custom_marghub_logo.png")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Uri.fromFile(file).toString()
                } ?: uri
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Failed to copy marghub logo to internal storage", e)
                uri
            }
        } else {
            uri
        }
        _marghubLogoUri.value = finalUriStr
        sharedPrefs.edit().putString("marghub_logo_uri", finalUriStr).apply()
    }

    fun resetMarghubLogoUri() {
        _marghubLogoUri.value = ""
        sharedPrefs.edit().putString("marghub_logo_uri", "").apply()
        try {
            val file = java.io.File(getApplication<Application>().filesDir, "custom_marghub_logo.png")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("AttendanceViewModel", "Failed to delete custom marghub logo file", e)
        }
    }

    private fun listenToAppLogoFromFirestore() {
        syncManager.listenToAppLogo { remoteUrl ->
            if (remoteUrl.isNotEmpty() && remoteUrl != _appLogoUri.value) {
                if (remoteUrl.startsWith("http")) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val context = getApplication<Application>()
                            val url = java.net.URL(remoteUrl)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            connection.inputStream.use { inputStream ->
                                val file = java.io.File(context.filesDir, "custom_app_logo.png")
                                file.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                                val localUriStr = Uri.fromFile(file).toString()
                                withContext(Dispatchers.Main) {
                                    _appLogoUri.value = localUriStr
                                    sharedPrefs.edit().putString("app_logo_uri", localUriStr).apply()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AttendanceViewModel", "Failed to download remote logo: ${e.message}")
                            withContext(Dispatchers.Main) {
                                _appLogoUri.value = remoteUrl
                                sharedPrefs.edit().putString("app_logo_uri", remoteUrl).apply()
                            }
                        }
                    }
                } else {
                    _appLogoUri.value = remoteUrl
                    sharedPrefs.edit().putString("app_logo_uri", remoteUrl).apply()
                }
            } else if (remoteUrl.isEmpty() && _appLogoUri.value.isNotEmpty()) {
                _appLogoUri.value = ""
                sharedPrefs.edit().putString("app_logo_uri", "").apply()
                try {
                    val file = java.io.File(getApplication<Application>().filesDir, "custom_app_logo.png")
                    if (file.exists()) file.delete()
                } catch (e: Exception) {}
            }
        }
    }

    // Payment config state
    private val _upiId = MutableStateFlow(sharedPrefs.getString("payment_upi_id", "") ?: "")
    val upiId: StateFlow<String> = _upiId.asStateFlow()

    private val _upiName = MutableStateFlow(sharedPrefs.getString("payment_upi_name", "") ?: "")
    val upiName: StateFlow<String> = _upiName.asStateFlow()

    private val _upiQrUrl = MutableStateFlow(sharedPrefs.getString("payment_upi_qr_url", "") ?: "")
    val upiQrUrl: StateFlow<String> = _upiQrUrl.asStateFlow()

    // Real-time Fee List state backed by Room database Flow
    val allFeesList: StateFlow<List<com.example.data.model.FeeRecord>> = repository.allFees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Listener registrations
    private var paymentConfigListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var allFeesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var studentFeesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var allStudentsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var allAttendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun listenToPaymentConfigFromFirestore() {
        paymentConfigListener?.remove()
        paymentConfigListener = syncManager.listenToPaymentConfig { upiIdVal, upiNameVal, upiQrUrlVal ->
            _upiId.value = upiIdVal
            _upiName.value = upiNameVal
            _upiQrUrl.value = upiQrUrlVal
            sharedPrefs.edit()
                .putString("payment_upi_id", upiIdVal)
                .putString("payment_upi_name", upiNameVal)
                .putString("payment_upi_qr_url", upiQrUrlVal)
                .apply()
        }
    }

    fun updatePaymentConfig(upiIdVal: String, upiNameVal: String, upiQrUrlVal: String) {
        _upiId.value = upiIdVal
        _upiName.value = upiNameVal
        _upiQrUrl.value = upiQrUrlVal
        sharedPrefs.edit()
            .putString("payment_upi_id", upiIdVal)
            .putString("payment_upi_name", upiNameVal)
            .putString("payment_upi_qr_url", upiQrUrlVal)
            .apply()
        viewModelScope.launch {
            syncManager.updatePaymentConfigRemote(upiIdVal, upiNameVal, upiQrUrlVal)
        }
    }

    private fun listenToAllFeesFromFirestore() {
        allFeesListener?.remove()
        allFeesListener = syncManager.listenToAllFeeRecords { list ->
            viewModelScope.launch {
                repository.insertFeesLocal(list)
            }
        }
    }

    private fun listenToStudentFeesFromFirestore(studentId: String) {
        studentFeesListener?.remove()
        studentFeesListener = syncManager.listenToStudentFeeRecords(studentId) { list ->
            viewModelScope.launch {
                repository.insertFeesLocal(list)
            }
        }
    }

    private fun listenToAllStudentsFromFirestore() {
        allStudentsListener?.remove()
        allStudentsListener = syncManager.listenToAllStudents { list ->
            viewModelScope.launch {
                repository.insertStudentsLocal(list)
            }
        }
    }

    private fun listenToAllAttendanceFromFirestore() {
        allAttendanceListener?.remove()
        allAttendanceListener = syncManager.listenToAllAttendance { list ->
            viewModelScope.launch {
                repository.insertAttendanceRecordsLocal(list)
            }
        }
    }

    fun addFeeForStudent(studentId: String, studentName: String, title: String, amount: Double, dueDate: String) {
        val fee = com.example.data.model.FeeRecord(
            feeId = java.util.UUID.randomUUID().toString(),
            studentId = studentId,
            studentName = studentName,
            title = title,
            amount = amount,
            dueDate = dueDate,
            status = "Unpaid",
            lastUpdated = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertFee(fee)
        }
    }

    fun submitFeePayment(feeId: String, transactionId: String, screenshotUri: Uri?, upiUsed: String) {
        viewModelScope.launch {
            var url = ""
            if (screenshotUri != null) {
                _uiState.value = UiState.Saving
                val uploaded = storageHelper.uploadStudentPhoto(screenshotUri, "fee_payment_${feeId}")
                if (uploaded != null) {
                    url = uploaded
                }
            }
            val existing = repository.getFeeById(feeId)
            if (existing != null) {
                val updated = existing.copy(
                    status = "Pending",
                    transactionId = transactionId,
                    screenshotUrl = url.ifEmpty { existing.screenshotUrl },
                    upiUsed = upiUsed,
                    paymentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateFee(updated)
                _uiState.value = UiState.Success
            }
        }
    }

    fun verifyFeePayment(feeId: String, isApproved: Boolean, rejectionReason: String = "") {
        viewModelScope.launch {
            val existing = repository.getFeeById(feeId)
            if (existing != null) {
                val updated = existing.copy(
                    status = if (isApproved) "Paid" else "Rejected",
                    rejectionReason = rejectionReason,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateFee(updated)
            }
        }
    }

    fun deleteFeeRecord(fee: com.example.data.model.FeeRecord) {
        viewModelScope.launch {
            repository.deleteFee(fee)
        }
    }

    private var studentAttendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun listenToStudentAttendanceRemote(studentId: String) {
        studentAttendanceListener?.remove()
        studentAttendanceListener = syncManager.listenToStudentAttendance(studentId) { record ->
            notifyAttendanceIfNew(record)
        }
    }

    private fun notifyAttendanceIfNew(record: AttendanceRecord) {
        // Include status in key so that updates (e.g. Present -> Absent or vice-versa) trigger real-time notification
        val key = "notified_attendance_${record.attendanceId.ifEmpty { "${record.studentId}_${record.date}" }}_${record.status.lowercase()}"
        if (!sharedPrefs.getBoolean(key, false)) {
            viewModelScope.launch {
                try {
                    repository.saveAttendanceRecordFromCloud(record)
                    
                    // Only trigger push notification if this is the phone where that specific student is logged in
                    if (record.studentId == _currentStudent.value?.studentId) {
                        // Trigger push notification with the dynamic logo (loaded from custom_app_logo.png)
                        notificationHelper.sendStudentAttendanceNotification(
                            studentName = record.studentName,
                            status = record.status,
                            date = record.date,
                            subject = "Daily Attendance"
                        )
                    }
                    // Mark as notified
                    sharedPrefs.edit().putBoolean(key, true).apply()
                } catch (e: Exception) {
                    Log.e("AttendanceViewModel", "Failed to process remote student attendance record: ${e.message}")
                }
            }
        }
    }

    // Dynamic list of authorized email IDs
    private val _allowedEmails = MutableStateFlow<Set<String>>(getAllowedEmailsFromPrefs())
    val allowedEmails: StateFlow<Set<String>> = _allowedEmails.asStateFlow()

    private fun getAllowedEmailsFromPrefs(): Set<String> {
        val saved = sharedPrefs.getStringSet("allowed_emails_list", null)
        if (saved == null) {
            val defaultSet = setOf("marghubalam000@gmail.com", "admin@toppers.com", "principal@toppers.com")
            sharedPrefs.edit().putStringSet("allowed_emails_list", defaultSet).apply()
            return defaultSet
        }
        return saved
    }

    fun addAllowedEmail(email: String) {
        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty()) return
        val current = _allowedEmails.value.toMutableSet()
        current.add(trimmed)
        _allowedEmails.value = current
        sharedPrefs.edit().putStringSet("allowed_emails_list", current).apply()
        
        // Sync to Supabase
        viewModelScope.launch {
            try {
                repository.addAllowedEmailToSupabase(trimmed)
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error adding allowed email to Supabase: ${e.message}")
            }
        }
    }

    fun removeAllowedEmail(email: String) {
        val trimmed = email.trim().lowercase()
        if (trimmed == "marghubalam000@gmail.com") return // Always protect primary admin account
        val current = _allowedEmails.value.toMutableSet()
        current.remove(trimmed)
        _allowedEmails.value = current
        sharedPrefs.edit().putStringSet("allowed_emails_list", current).apply()
        
        // Sync to Supabase
        viewModelScope.launch {
            try {
                repository.removeAllowedEmailFromSupabase(trimmed)
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error removing allowed email from Supabase: ${e.message}")
            }
        }
    }

    fun getGoogleWebClientId(): String = repository.getGoogleWebClientId()
    fun saveGoogleWebClientId(clientId: String) = repository.saveGoogleWebClientId(clientId)

    fun isAuthorizedAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val emailNormalized = email.trim().lowercase()
        return emailNormalized == "marghubalam000@gmail.com"
    }

    fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return email.matches(emailPattern.toRegex())
    }

    // SMTP Configuration States
    private val _smtpHost = MutableStateFlow(sharedPrefs.getString("smtp_host", com.example.BuildConfig.SMTP_HOST) ?: com.example.BuildConfig.SMTP_HOST)
    val smtpHost: StateFlow<String> = _smtpHost.asStateFlow()

    private val _smtpPort = MutableStateFlow(sharedPrefs.getInt("smtp_port", com.example.BuildConfig.SMTP_PORT.toIntOrNull() ?: 587))
    val smtpPort: StateFlow<Int> = _smtpPort.asStateFlow()

    private val _smtpUsername = MutableStateFlow(sharedPrefs.getString("smtp_username", com.example.BuildConfig.SMTP_USER) ?: com.example.BuildConfig.SMTP_USER)
    val smtpUsername: StateFlow<String> = _smtpUsername.asStateFlow()

    private val _smtpPassword = MutableStateFlow(sharedPrefs.getString("smtp_password", com.example.BuildConfig.SMTP_PASS) ?: com.example.BuildConfig.SMTP_PASS)
    val smtpPassword: StateFlow<String> = _smtpPassword.asStateFlow()

    private val _smtpSenderName = MutableStateFlow(sharedPrefs.getString("smtp_sender_name", "Toppers Academy Portal") ?: "Toppers Academy Portal")
    val smtpSenderName: StateFlow<String> = _smtpSenderName.asStateFlow()

    private val _smtpEnabled = MutableStateFlow(sharedPrefs.getBoolean("smtp_enabled", true))
    val smtpEnabled: StateFlow<Boolean> = _smtpEnabled.asStateFlow()

    private fun initSmtpDefaults() {
        if (!sharedPrefs.contains("smtp_host") || sharedPrefs.getString("smtp_host", "").isNullOrBlank()) {
            sharedPrefs.edit().apply {
                putString("smtp_host", com.example.BuildConfig.SMTP_HOST)
                putInt("smtp_port", com.example.BuildConfig.SMTP_PORT.toIntOrNull() ?: 587)
                putString("smtp_username", com.example.BuildConfig.SMTP_USER)
                putString("smtp_password", com.example.BuildConfig.SMTP_PASS)
                putString("smtp_sender_name", "Toppers Academy Portal")
                putBoolean("smtp_enabled", true)
                apply()
            }
        }
    }

    fun saveSmtpSettings(host: String, port: Int, user: String, pass: String, sender: String, enabled: Boolean) {
        val cleanedHost = host.trim().lowercase()
            .replace("smpt", "smtp")
            .replace("stmp", "smtp")
        _smtpHost.value = cleanedHost
        _smtpPort.value = port
        _smtpUsername.value = user
        _smtpPassword.value = pass
        _smtpSenderName.value = sender
        _smtpEnabled.value = enabled

        sharedPrefs.edit().apply {
            putString("smtp_host", cleanedHost)
            putInt("smtp_port", port)
            putString("smtp_username", user)
            putString("smtp_password", pass)
            putString("smtp_sender_name", sender)
            putBoolean("smtp_enabled", enabled)
            apply()
        }
    }

    fun sendSmtpOtp(email: String, otp: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val rawHost = _smtpHost.value.trim()
            val host = rawHost.lowercase()
                .replace("smpt", "smtp")
                .replace("stmp", "smtp")
            val port = _smtpPort.value
            val user = _smtpUsername.value.trim()
            val pass = _smtpPassword.value.trim()
            val sender = _smtpSenderName.value.trim()

            if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                onComplete(Result.failure(Exception("SMTP Settings are not completely configured.")))
                return@launch
            }

            val subject = "🔑 Toppers Academy Portal - Verification Code"
            val htmlBody = """
                <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f5; max-width: 600px; margin: 0 auto; border-radius: 12px; border: 1px solid #e4e4e7;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #6366f1; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">Toppers Academy</h2>
                        <p style="color: #71717a; margin: 4px 0 0 0; font-size: 14px; font-weight: 600;">Secure Portal Sign-In</p>
                    </div>
                    
                    <div style="background-color: #ffffff; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); text-align: center;">
                        <p style="color: #3f3f46; font-size: 16px; margin: 0 0 16px 0;">Use the following verification code to complete your administrator sign-in:</p>
                        
                        <div style="display: inline-block; background-color: #f5f3ff; color: #6d28d9; font-size: 36px; font-weight: 900; padding: 12px 36px; border-radius: 8px; letter-spacing: 6px; margin: 10px 0 20px 0; border: 1px solid #ddd6fe;">
                            $otp
                        </div>
                        
                        <p style="color: #71717a; font-size: 13px; margin: 0;">This code is valid for 10 minutes. Do not share this OTP with anyone.</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 24px; color: #a1a1aa; font-size: 11px;">
                        <p style="margin: 0;">Toppers Academy Administration Portal Security Alert</p>
                        <p style="margin: 4px 0 0 0;">This is an automated security message. Please do not reply directly.</p>
                    </div>
                </div>
            """.trimIndent()

            val result = com.example.util.SmtpSender.sendEmail(
                host = host,
                port = port,
                username = user,
                password = pass,
                senderName = sender,
                recipient = email,
                subject = subject,
                bodyText = htmlBody
            )
            onComplete(result)
        }
    }

    // Filter states
    private val _selectedClass = MutableStateFlow("Class 10")
    val selectedClass: StateFlow<String> = _selectedClass

    private val _selectedSection = MutableStateFlow("A")
    val selectedSection: StateFlow<String> = _selectedSection

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate

    val selectedDateDisplay: StateFlow<String> = _selectedDate.map { dateStr ->
        try {
            val date = dbDateFormat.parse(dateStr)
            if (date != null) displayDateFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Raw list of students
    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentCount: StateFlow<Int> = repository.studentCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Full historic attendance list
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current unsynced record list count
    val unsyncedCount: StateFlow<Int> = repository.unsyncedRecords
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Connection Sync State
    val syncState = repository.syncStatus

    // Active attendance sheet being edited: studentId (String) -> AttendanceStatus (Present, Absent, Leave, Holiday)
    private val _attendanceSheet = MutableStateFlow<Map<String, String>>(emptyMap())
    val attendanceSheet: StateFlow<Map<String, String>> = _attendanceSheet

    // UI state for feedback dialogs/toasts
    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState

    // Student Authentication and Portal State
    private fun loadSavedStudent(): Student? {
        if (!sharedPrefs.getBoolean("is_student_logged_in", false)) return null
        val studentId = sharedPrefs.getString("student_id", null) ?: return null
        return Student(
            id = sharedPrefs.getInt("student_id_int", 0),
            studentId = studentId,
            name = sharedPrefs.getString("student_name", "") ?: "",
            fatherName = sharedPrefs.getString("student_father", "") ?: "",
            motherName = sharedPrefs.getString("student_mother", "") ?: "",
            mobile = sharedPrefs.getString("student_mobile", "") ?: "",
            email = sharedPrefs.getString("student_email", "") ?: "",
            studentClass = sharedPrefs.getString("student_class", "") ?: "",
            section = sharedPrefs.getString("student_section", "") ?: "",
            rollNumber = sharedPrefs.getString("student_roll", "") ?: "",
            gender = sharedPrefs.getString("student_gender", "") ?: "",
            dob = sharedPrefs.getString("student_dob", "") ?: "",
            address = sharedPrefs.getString("student_address", "") ?: "",
            aadhaar = sharedPrefs.getString("student_aadhaar", "") ?: "",
            photo = sharedPrefs.getString("student_photo", "") ?: "",
            status = sharedPrefs.getString("student_status", "Active") ?: "Active",
            password = sharedPrefs.getString("student_pass", "") ?: "",
            mainSubject = sharedPrefs.getString("student_main_subject", "") ?: ""
        )
    }

    private fun saveLoggedInStudent(student: Student) {
        sharedPrefs.edit().apply {
            putBoolean("is_student_logged_in", true)
            putString("student_id", student.studentId)
            putString("student_name", student.name)
            putString("student_father", student.fatherName)
            putString("student_mother", student.motherName)
            putString("student_mobile", student.mobile)
            putString("student_email", student.email)
            putString("student_class", student.studentClass)
            putString("student_section", student.section)
            putString("student_roll", student.rollNumber)
            putString("student_gender", student.gender)
            putString("student_dob", student.dob)
            putString("student_address", student.address)
            putString("student_aadhaar", student.aadhaar)
            putString("student_photo", student.photo)
            putString("student_status", student.status)
            putString("student_pass", student.password)
            putString("student_main_subject", student.mainSubject)
            putInt("student_id_int", student.id)
            apply()
        }
    }

    private fun clearLoggedInStudent() {
        sharedPrefs.edit().apply {
            putBoolean("is_student_logged_in", false)
            remove("student_id")
            remove("student_name")
            remove("student_father")
            remove("student_mother")
            remove("student_mobile")
            remove("student_email")
            remove("student_class")
            remove("student_section")
            remove("student_roll")
            remove("student_gender")
            remove("student_dob")
            remove("student_address")
            remove("student_aadhaar")
            remove("student_photo")
            remove("student_status")
            remove("student_pass")
            remove("student_main_subject")
            remove("student_id_int")
            apply()
        }
    }

    private val _currentStudent = MutableStateFlow<Student?>(loadSavedStudent())
    val currentStudent: StateFlow<Student?> = _currentStudent.asStateFlow()

    private val _isStudentLoggedIn = MutableStateFlow(sharedPrefs.getBoolean("is_student_logged_in", false))
    val isStudentLoggedIn: StateFlow<Boolean> = _isStudentLoggedIn.asStateFlow()

    private val _welcomeUser = MutableStateFlow<String?>(null)
    val welcomeUser: StateFlow<String?> = _welcomeUser.asStateFlow()

    fun showWelcome(name: String) {
        _welcomeUser.value = name
    }

    fun dismissWelcome() {
        _welcomeUser.value = null
    }

    init {
        // Initialize working SMTP settings from BuildConfig defaults
        initSmtpDefaults()

        // Start listening to school logo URL updates in real-time from Firestore settings collection
        listenToAppLogoFromFirestore()

        // Start listening to payment config in real-time
        listenToPaymentConfigFromFirestore()

        // Real-time listener for current student attendance push-notifications and fees
        viewModelScope.launch {
            currentStudent.collect { student ->
                if (student != null) {
                    listenToStudentAttendanceRemote(student.studentId)
                    listenToStudentFeesFromFirestore(student.studentId)
                } else {
                    studentAttendanceListener?.remove()
                    studentAttendanceListener = null
                    studentFeesListener?.remove()
                    studentFeesListener = null
                }
            }
        }

        // Real-time listener for all fees, students, and attendance when logged in as Admin
        viewModelScope.launch {
            isAdminLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    listenToAllFeesFromFirestore()
                    listenToAllStudentsFromFirestore()
                    listenToAllAttendanceFromFirestore()
                } else {
                    allFeesListener?.remove()
                    allFeesListener = null
                    allStudentsListener?.remove()
                    allStudentsListener = null
                    allAttendanceListener?.remove()
                    allAttendanceListener = null
                }
            }
        }

        try {
            if (authHelper.isUserLoggedIn) {
                val email = authHelper.currentEmail ?: "admin@toppers.com"
                if (authHelper.isAuthorizedAdmin(email)) {
                    _isAdminLoggedIn.value = true
                    _adminUsername.value = email
                    sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                } else {
                    // Log out if authenticated but unauthorized (protected route policy)
                    authHelper.signOut()
                    _isAdminLoggedIn.value = false
                    sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
                }
            }
        } catch (e: Exception) {
            Log.w("AttendanceViewModel", "Firebase Auth not available: ${e.message}")
        }

        // Observe students, selected date, selected class & section to reload/create attendance state
        combine(students, _selectedDate, _selectedClass, _selectedSection) { studentList, dateStr, cls, sec ->
            loadAttendanceSheetForFilters(studentList, dateStr, cls, sec)
        }.launchIn(viewModelScope)
    }

    private fun getTodayDateString(): String {
        return dbDateFormat.format(Calendar.getInstance().time)
    }

    private suspend fun loadAttendanceSheetForFilters(
        studentList: List<Student>,
        dateStr: String,
        cls: String,
        sec: String
    ) {
        val filteredList = studentList.filter {
            it.studentClass.equals(cls, ignoreCase = true) && it.section.equals(sec, ignoreCase = true) && it.status == "Active"
        }

        if (filteredList.isEmpty()) {
            _attendanceSheet.value = emptyMap()
            return
        }

        // Fetch saved records for this date from local DB
        val savedRecords = repository.getAttendanceForDate(dateStr).first()
        val sheet = filteredList.associate { student ->
            val savedRecord = savedRecords.find { it.studentId == student.studentId }
            // Default to Present if no record exists
            student.studentId to (savedRecord?.status ?: "Present")
        }
        _attendanceSheet.value = sheet
    }

    // AUTH ACTIONS
    fun loginWithOtpSuccess(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val emailNormalized = email.trim().lowercase()
            // Check if email is in allowed emails list
            val isAllowed = _allowedEmails.value.any { it.trim().lowercase() == emailNormalized }
            
            delay(1000) // Aesthetic delay for progress indicator
            
            if (isAllowed) {
                _isAdminLoggedIn.value = true
                _adminUsername.value = emailNormalized
                showWelcome("Administrator")
                sharedPrefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("admin_username", emailNormalized)
                    .apply()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error("Unauthorized: Email $emailNormalized is not registered as an administrator.")
            }
        }
    }

    fun loginWithGoogle(email: String, idToken: String? = null) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val emailNormalized = email.trim().lowercase()
            
            if (emailNormalized != "marghubalam000@gmail.com") {
                delay(1000)
                _authState.value = AuthState.Error("Unauthorized: Only marghubalam000@gmail.com is authorized to log in.")
                return@launch
            }
            
            var isAllowed = false
            
            // If we have an idToken and Supabase is configured, verify with Supabase Auth!
            if (!idToken.isNullOrEmpty() && isSupabaseAvailable()) {
                val authResult = repository.signInWithSupabaseGoogle(idToken)
                if (authResult.isSuccess) {
                    val supabaseEmail = authResult.getOrNull()?.trim()?.lowercase() ?: ""
                    Log.d("AttendanceViewModel", "Supabase Auth Google sign-in successful for: $supabaseEmail")
                    isAllowed = (supabaseEmail == "marghubalam000@gmail.com")
                } else {
                    val errorMsg = authResult.exceptionOrNull()?.message ?: "Supabase Google OAuth failed."
                    _authState.value = AuthState.Error("Supabase Auth error: $errorMsg")
                    return@launch
                }
            } else {
                // Client-side / Offline email verification fallback
                isAllowed = (emailNormalized == "marghubalam000@gmail.com")
            }
            
            delay(1200) // Beautiful authentic animation pause
            
            if (isAllowed) {
                _isAdminLoggedIn.value = true
                _adminUsername.value = emailNormalized
                showWelcome("Marghubur Rahman")
                sharedPrefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("admin_username", emailNormalized)
                    .apply()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error("Unauthorized: Only marghubalam000@gmail.com is allowed.")
            }
        }
    }

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        val emailNormalized = email.trim().lowercase()
        
        if (emailNormalized != "marghubalam000@gmail.com") {
            _authState.value = AuthState.Error("Unauthorized: Only marghubalam000@gmail.com is authorized to log in.")
            return
        }
        
        if (!authHelper.isFirebaseAvailable) {
            // Local fallback simulation mode
            if (pass == adminPassword) {
                _isAdminLoggedIn.value = true
                _adminUsername.value = emailNormalized
                showWelcome("Marghubur Rahman")
                sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error("Incorrect Credentials or Unauthorized Email")
            }
            return
        }

        authHelper.signIn(emailNormalized, pass) { result ->
            result.fold(
                onSuccess = { user ->
                    val userEmail = user.email?.trim()?.lowercase() ?: ""
                    if (userEmail == "marghubalam000@gmail.com") {
                        _isAdminLoggedIn.value = true
                        _adminUsername.value = userEmail
                        showWelcome("Marghubur Rahman")
                        sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                        _authState.value = AuthState.Success
                    } else {
                        authHelper.signOut()
                        _authState.value = AuthState.Error("Unauthorized: Only marghubalam000@gmail.com is authorized to log in.")
                    }
                },
                onFailure = { exception ->
                    val errMsg = exception.localizedMessage ?: "Authentication failed"
                    // If Firebase fails (e.g., no internet or user not found), fall back to local default credentials for development
                    if (pass == adminPassword) {
                        _isAdminLoggedIn.value = true
                        _adminUsername.value = emailNormalized
                        showWelcome("Marghubur Rahman")
                        sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                        _authState.value = AuthState.Success
                    } else {
                        _authState.value = AuthState.Error(errMsg)
                    }
                }
            )
        }
    }

    fun logout() {
        authHelper.signOut()
        _isAdminLoggedIn.value = false
        sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String): Boolean {
        if (authHelper.isFirebaseAvailable) {
            authHelper.sendPasswordReset(email) { success ->
                if (success) {
                    Log.d("AttendanceViewModel", "Password reset instructions dispatched to $email")
                } else {
                    Log.e("AttendanceViewModel", "Password reset dispatch error")
                }
            }
            return true
        }
        return email == "admin@toppers.com"
    }

    fun changePassword(currentPass: String, newPass: String): Boolean {
        if (authHelper.isFirebaseAvailable) {
            authHelper.updatePassword(newPass) { success ->
                if (success) {
                    Log.d("AttendanceViewModel", "Security credentials updated in Firebase Auth")
                } else {
                    Log.e("AttendanceViewModel", "Failed to update Firebase Auth password")
                }
            }
        }
        return if (currentPass == adminPassword) {
            adminPassword = newPass
            sharedPrefs.edit().putString("admin_password", newPass).apply()
            true
        } else {
            false
        }
    }

    fun clearAuthState() {
        _authState.value = AuthState.Idle
    }

    // FILTERS SETTING
    fun setClassAndSection(cls: String, sec: String) {
        _selectedClass.value = cls
        _selectedSection.value = sec
    }

    fun setDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun changeDateByDays(days: Int) {
        val cal = Calendar.getInstance()
        try {
            val currentDate = dbDateFormat.parse(_selectedDate.value)
            if (currentDate != null) {
                cal.time = currentDate
            }
        } catch (e: Exception) {
            // Use today
        }
        cal.add(Calendar.DAY_OF_YEAR, days)
        _selectedDate.value = dbDateFormat.format(cal.time)
    }

    // ONE-CLICK ATTENDANCE ACTIONS
    fun setStudentStatus(studentId: String, status: String) {
        val currentSheet = _attendanceSheet.value.toMutableMap()
        currentSheet[studentId] = status
        _attendanceSheet.value = currentSheet
    }

    fun markAllAs(status: String) {
        val currentSheet = _attendanceSheet.value.toMutableMap()
        for (studentId in currentSheet.keys) {
            currentSheet[studentId] = status
        }
        _attendanceSheet.value = currentSheet
    }

    // SAVE CORE ATTENDANCE
    fun saveAttendance(sendNotifications: Boolean) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                val studentList = students.value
                val currentSheet = _attendanceSheet.value
                val dateStr = _selectedDate.value
                val cls = _selectedClass.value
                val sec = _selectedSection.value

                // Filter students who actually belong to the filtered class & section
                val filteredStudents = studentList.filter {
                    it.studentClass.equals(cls, ignoreCase = true) && it.section.equals(sec, ignoreCase = true) && it.status == "Active"
                }

                if (filteredStudents.isEmpty()) {
                    _uiState.value = UiState.Error("No students in this class/section to save.")
                    return@launch
                }

                // Prepare records
                val recordsToSave = filteredStudents.map { student ->
                    val status = currentSheet[student.studentId] ?: "Present"
                    AttendanceRecord(
                        studentId = student.studentId,
                        studentName = student.name,
                        studentClass = student.studentClass,
                        section = student.section,
                        rollNumber = student.rollNumber,
                        date = dateStr,
                        status = status,
                        createdAt = System.currentTimeMillis()
                    )
                }

                // Delete old records for this class and section on this date to ensure no duplicates
                val existingRecords = repository.getAttendanceForDate(dateStr).first()
                for (record in existingRecords) {
                    if (record.studentClass.equals(cls, ignoreCase = true) && record.section.equals(sec, ignoreCase = true)) {
                        db.attendanceDao().deleteAttendanceForStudentOnDate(record.studentId, dateStr)
                    }
                }

                val synced = repository.saveMultipleAttendanceRecords(recordsToSave)
                _uiState.value = UiState.SuccessWithSync(synced, recordsToSave.size)

                // Send Student Attendance Notifications & Parent Alerts
                for (student in filteredStudents) {
                    val status = currentSheet[student.studentId] ?: "Present"
                    
                    // Trigger push/local notification to Student phone only if logged in
                    if (student.studentId == currentStudent.value?.studentId) {
                        notificationHelper.sendStudentAttendanceNotification(
                            studentName = student.name,
                            status = status,
                            date = dateStr,
                            subject = student.mainSubject.ifEmpty { "General Class" }
                        )
                    }

                    if (sendNotifications && (status == "Absent" || status == "Leave")) {
                        notificationHelper.sendParentAlertNotification(
                            studentName = student.name,
                            rollNumber = student.rollNumber,
                            isPresent = status == "Present",
                            date = dateStr
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save attendance")
                Log.e("AttendanceViewModel", "Save failed: ${e.message}", e)
            }
        }
    }

    // STUDENT MANAGEMENT ACTIONS
    fun addStudent(
        name: String,
        fatherName: String = "",
        motherName: String = "",
        mobile: String = "",
        email: String = "",
        studentClass: String = "",
        section: String = "",
        rollNumber: String = "",
        gender: String = "",
        dob: String = "",
        address: String = "",
        aadhaar: String = "",
        admissionDate: String = "",
        photo: String = "",
        status: String = "Active",
        mainSubject: String = "",
        fatherMobile: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                val newStudent = Student(
                    name = name,
                    fatherName = fatherName,
                    motherName = motherName,
                    mobile = mobile,
                    email = email,
                    studentClass = studentClass,
                    section = section,
                    rollNumber = rollNumber,
                    gender = gender,
                    dob = dob,
                    address = address,
                    aadhaar = aadhaar,
                    admissionDate = admissionDate,
                    photo = photo,
                    status = status,
                    mainSubject = mainSubject,
                    fatherMobile = fatherMobile
                )
                val localId = repository.insertStudent(newStudent)
                _uiState.value = UiState.Success
                
                // Fetch the created student to get generated studentId & password and send welcome email
                val createdStudent = repository.getStudentById(localId.toInt())
                if (createdStudent != null && createdStudent.email.isNotEmpty()) {
                    sendStudentCredentialsEmail(createdStudent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to add student")
            }
        }
    }

    fun sendStudentCredentialsEmail(student: Student) {
        viewModelScope.launch {
            val rawHost = _smtpHost.value.trim()
            val host = rawHost.lowercase()
                .replace("smpt", "smtp")
                .replace("stmp", "smtp")
            val port = _smtpPort.value
            val user = _smtpUsername.value.trim()
            val pass = _smtpPassword.value.trim()
            val sender = _smtpSenderName.value.trim()

            if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Log.w("AttendanceViewModel", "SMTP not configured. Cannot send welcome email.")
                return@launch
            }

            val subject = "🎓 Welcome to Toppers Academy - Your Login Credentials"
            val htmlBody = """
                <div style="font-family: Arial, sans-serif; padding: 20px; background-color: #f4f4f5; max-width: 600px; margin: 0 auto; border-radius: 12px; border: 1px solid #e4e4e7;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #6366f1; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">Toppers Academy</h2>
                        <p style="color: #71717a; margin: 4px 0 0 0; font-size: 14px; font-weight: 600;">Student Portal Welcome Kit</p>
                    </div>
                    
                    <div style="background-color: #ffffff; padding: 24px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                        <p style="color: #18181b; font-size: 16px; font-weight: bold; margin: 0 0 12px 0;">Hello ${student.name},</p>
                        <p style="color: #3f3f46; font-size: 14px; line-height: 1.5; margin: 0 0 20px 0;">Welcome to Toppers Academy! Your student account has been successfully created. You can now log in to the student portal using the credentials below to track your attendance, view reports, and more.</p>
                        
                        <div style="background-color: #f8fafc; padding: 16px; border-radius: 8px; border: 1px solid #e2e8f0; margin-bottom: 20px;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; font-size: 13px; font-weight: 600; width: 120px;">Student ID:</td>
                                    <td style="padding: 6px 0; color: #0f172a; font-size: 14px; font-weight: 700;">${student.studentId}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; font-size: 13px; font-weight: 600;">PIN Password:</td>
                                    <td style="padding: 6px 0; color: #6366f1; font-size: 14px; font-weight: 700;">${student.password}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; font-size: 13px; font-weight: 600;">Class:</td>
                                    <td style="padding: 6px 0; color: #0f172a; font-size: 14px; font-weight: 500;">${student.studentClass}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 6px 0; color: #64748b; font-size: 13px; font-weight: 600;">Subject:</td>
                                    <td style="padding: 6px 0; color: #0f172a; font-size: 14px; font-weight: 500;">${student.mainSubject.ifEmpty { "General" }}</td>
                                </tr>
                            </table>
                        </div>
                        
                        <p style="color: #71717a; font-size: 12px; line-height: 1.4; margin: 0;">Please keep your login PIN safe. Do not share your login credentials with others.</p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 24px; color: #a1a1aa; font-size: 11px;">
                        <p style="margin: 0;">Toppers Academy Student Services</p>
                        <p style="margin: 4px 0 0 0;">This is an automated system email. Please do not reply directly.</p>
                    </div>
                </div>
            """.trimIndent()

            val result = com.example.util.SmtpSender.sendEmail(
                host = host,
                port = port,
                username = user,
                password = pass,
                senderName = sender,
                recipient = student.email,
                subject = subject,
                bodyText = htmlBody
            )
            if (result.isSuccess) {
                Log.d("AttendanceViewModel", "Credentials email sent successfully to ${student.email}")
            } else {
                Log.e("AttendanceViewModel", "Failed to send credentials email: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                repository.updateStudent(student)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update student")
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                repository.deleteStudent(student)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete student")
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                repository.syncUnsyncedRecords()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Sync failed")
            }
        }
    }

    fun fetchFromSupabase() {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                // Fetch allowed emails from Supabase and update local lists
                try {
                    val remoteAllowedList = repository.fetchAllowedEmailsFromSupabase()
                    if (remoteAllowedList != null) {
                        val current = _allowedEmails.value.toMutableSet()
                        remoteAllowedList.forEach { item ->
                            val itemTrimmed = item.trim().lowercase()
                            if (itemTrimmed.isNotEmpty()) {
                                current.add(itemTrimmed)
                            }
                        }
                        _allowedEmails.value = current
                        sharedPrefs.edit().putStringSet("allowed_emails_list", current).apply()
                    }
                } catch (ex: Exception) {
                    Log.e("AttendanceViewModel", "Failed to sync allowed emails during Supabase fetch: ${ex.message}")
                }

                val success = repository.fetchAndSyncFromSupabase()
                if (success) {
                    _uiState.value = UiState.Success
                } else {
                    val errMsg = supabaseLastError.value ?: "Failed to fetch data from Supabase. Ensure tables exist."
                    _uiState.value = UiState.Error(errMsg)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Supabase fetch failed")
            }
        }
    }

    fun clearUiState() {
        _uiState.value = UiState.Success
    }

    fun isCloudAvailable(): Boolean = repository.isCloudAvailable()
    

    fun loginStudent(studentId: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val student = repository.getStudentByStudentId(studentId.trim())
            if (student == null) {
                onResult(false, "Student ID not found.")
            } else {
                val enteredPassword = pin.trim()
                val actualPassword = student.password.trim()
                if (enteredPassword == actualPassword || (actualPassword.isEmpty() && enteredPassword == "123456")) {
                    saveLoggedInStudent(student)
                    _currentStudent.value = student
                    _isStudentLoggedIn.value = true
                    showWelcome(student.name)
                    onResult(true, "Welcome, ${student.name}!")
                } else {
                    onResult(false, "Invalid Password. Please try again.")
                }
            }
        }
    }

    fun reloadStudentPortalData(studentId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Saving
            try {
                // Fetch and sync everything from Supabase first
                val success = repository.fetchAndSyncFromSupabase()
                // Update current logged in student state to the newly fetched data
                val updatedStudent = repository.getStudentByStudentId(studentId)
                if (updatedStudent != null) {
                    _currentStudent.value = updatedStudent
                    saveLoggedInStudent(updatedStudent)
                }
                _uiState.value = UiState.Success
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to reload student data.")
                onComplete(false)
            }
        }
    }

    fun logoutStudent() {
        clearLoggedInStudent()
        _currentStudent.value = null
        _isStudentLoggedIn.value = false
    }

    fun updateCurrentStudentPhoto(newPhotoB64OrPreset: String) {
        val current = _currentStudent.value ?: return
        val updated = current.copy(photo = newPhotoB64OrPreset)
        _currentStudent.value = updated
        saveLoggedInStudent(updated)
        viewModelScope.launch {
            try {
                repository.updateStudent(updated)
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Failed to persist student photo: ${e.message}")
            }
        }
    }

    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecord>> {
        return repository.getAttendanceForStudent(studentId)
    }

    suspend fun uploadStudentPhoto(uri: Uri, name: String, onProgress: (Float) -> Unit = {}): String? {
        return storageHelper.uploadStudentPhoto(uri, name, onProgress)
    }

    suspend fun uploadAppLogo(uri: Uri, onProgress: (Float) -> Unit = {}): String? {
        return storageHelper.uploadStudentPhoto(uri, "app_logo", onProgress)
    }

    // --- NEW DYNAMIC FEATURES FOR USER REQUIREMENTS ---

    // 1. Batches state & management
    private val _batchesList = MutableStateFlow<List<String>>(getSavedBatches())
    val batchesList: StateFlow<List<String>> = _batchesList.asStateFlow()

    private fun getSavedBatches(): List<String> {
        val saved = sharedPrefs.getString("custom_batches_list", null)
        if (saved == null) {
            val defaultList = listOf("Class 10", "Class 11", "Class 12")
            sharedPrefs.edit().putString("custom_batches_list", defaultList.joinToString("|")).apply()
            return defaultList
        }
        if (saved.isEmpty()) return emptyList()
        return saved.split("|")
    }

    fun addBatch(batchName: String) {
        val trimmed = batchName.trim()
        if (trimmed.isEmpty()) return
        val current = _batchesList.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _batchesList.value = current
            sharedPrefs.edit().putString("custom_batches_list", current.joinToString("|")).apply()
        }
    }

    fun deleteBatch(batchName: String) {
        val trimmed = batchName.trim()
        val current = _batchesList.value.toMutableList()
        if (current.remove(trimmed)) {
            _batchesList.value = current
            sharedPrefs.edit().putString("custom_batches_list", current.joinToString("|")).apply()
        }
    }

    // 2. Staff state & management
    private val _staffList = MutableStateFlow<List<StaffItem>>(getSavedStaff())
    val staffList: StateFlow<List<StaffItem>> = _staffList.asStateFlow()

    private fun getSavedStaff(): List<StaffItem> {
        val saved = sharedPrefs.getString("custom_staff_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split(":")
            if (parts.size >= 2) {
                StaffItem(parts[0], parts[1], parts.getOrElse(2) { "Active" })
            } else null
        }
    }

    fun addStaff(name: String, role: String) {
        val trimmedName = name.trim()
        val trimmedRole = role.trim()
        if (trimmedName.isEmpty() || trimmedRole.isEmpty()) return
        val current = _staffList.value.toMutableList()
        current.add(StaffItem(trimmedName, trimmedRole, "Active"))
        _staffList.value = current
        saveStaffToPrefs(current)
    }

    fun deleteStaff(name: String, role: String) {
        val current = _staffList.value.toMutableList()
        val item = current.find { it.name == name && it.role == role }
        if (item != null) {
            current.remove(item)
            _staffList.value = current
            saveStaffToPrefs(current)
        }
    }

    private fun saveStaffToPrefs(list: List<StaffItem>) {
        val encoded = list.joinToString("||") { "${it.name}:${it.role}:${it.status}" }
        sharedPrefs.edit().putString("custom_staff_list", encoded).apply()
    }

    // 3. Exams state & management
    private val _examsList = MutableStateFlow<List<ExamSchedule>>(getSavedExams())
    val examsList: StateFlow<List<ExamSchedule>> = _examsList.asStateFlow()

    private fun getSavedExams(): List<ExamSchedule> {
        val saved = sharedPrefs.getString("custom_exams_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 5) {
                ExamSchedule(parts[0], parts[1], parts[2], parts[3], parts[4])
            } else null
        }
    }

    fun scheduleExam(subject: String, studentClass: String, date: String, time: String, maxMarks: String) {
        val current = _examsList.value.toMutableList()
        current.add(ExamSchedule(subject.trim(), studentClass.trim(), date.trim(), time.trim(), maxMarks.trim()))
        _examsList.value = current
        saveExamsToPrefs(current)
        
        // Push notification of the scheduled exam
        notificationHelper.sendExamScheduleNotification(
            title = "$subject Exam ($studentClass)",
            details = "Date: $date | Time: $time | Max Marks: $maxMarks"
        )
    }

    fun deleteExam(exam: ExamSchedule) {
        val current = _examsList.value.toMutableList()
        if (current.remove(exam)) {
            _examsList.value = current
            saveExamsToPrefs(current)
        }
    }

    private fun saveExamsToPrefs(list: List<ExamSchedule>) {
        val encoded = list.joinToString("||") { "${it.subject}|${it.studentClass}|${it.date}|${it.time}|${it.maxMarks}" }
        sharedPrefs.edit().putString("custom_exams_list", encoded).apply()
    }

    // 4. Announcements state & management
    private val _announcementsList = MutableStateFlow<List<AnnouncementItem>>(getSavedAnnouncements())
    val announcementsList: StateFlow<List<AnnouncementItem>> = _announcementsList.asStateFlow()

    private fun getSavedAnnouncements(): List<AnnouncementItem> {
        val saved = sharedPrefs.getString("custom_announcements_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                AnnouncementItem(parts[0], parts[1], parts[2], parts[3].toLongOrNull() ?: System.currentTimeMillis())
            } else null
        }
    }

    fun sendAnnouncement(title: String, message: String, targetClass: String) {
        val current = _announcementsList.value.toMutableList()
        val item = AnnouncementItem(title.trim(), message.trim(), targetClass.trim(), System.currentTimeMillis())
        current.add(0, item) // Newest first
        _announcementsList.value = current
        saveAnnouncementsToPrefs(current)

        // Dispatch parent alert push notification
        notificationHelper.sendAnnouncementNotification(title.trim(), message.trim())
    }

    fun deleteAnnouncement(item: AnnouncementItem) {
        val current = _announcementsList.value.toMutableList()
        if (current.remove(item)) {
            _announcementsList.value = current
            saveAnnouncementsToPrefs(current)
        }
    }

    private fun saveAnnouncementsToPrefs(list: List<AnnouncementItem>) {
        val encoded = list.joinToString("||") { "${it.title}|${it.message}|${it.targetClass}|${it.timestamp}" }
        sharedPrefs.edit().putString("custom_announcements_list", encoded).apply()
    }

    // 5. Community messages state & management
    private val _communityMessages = MutableStateFlow<List<CommunityMessage>>(getSavedCommunityMessages())
    val communityMessages: StateFlow<List<CommunityMessage>> = _communityMessages.asStateFlow()

    private fun getSavedCommunityMessages(): List<CommunityMessage> {
        val saved = sharedPrefs.getString("custom_community_messages", null)
        if (saved == null) {
            val defaultMsg = CommunityMessage("Principal", "Staff", "Welcome to Toppers Academy community space! Feel free to discuss here.", System.currentTimeMillis())
            val list = listOf(defaultMsg)
            saveCommunityMessagesToPrefs(list)
            return list
        }
        if (saved.isEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                CommunityMessage(parts[0], parts[1], parts[2], parts[3].toLongOrNull() ?: System.currentTimeMillis())
            } else null
        }
    }

    fun sendCommunityMessage(sender: String, role: String, message: String) {
        val current = _communityMessages.value.toMutableList()
        val msg = CommunityMessage(sender.trim(), role.trim(), message.trim(), System.currentTimeMillis())
        current.add(msg)
        _communityMessages.value = current
        saveCommunityMessagesToPrefs(current)
    }

    private fun saveCommunityMessagesToPrefs(list: List<CommunityMessage>) {
        val encoded = list.joinToString("||") { "${it.sender}|${it.role}|${it.message}|${it.timestamp}" }
        sharedPrefs.edit().putString("custom_community_messages", encoded).apply()
    }

    // 6. Homework state & management
    private val _homeworkList = MutableStateFlow<List<HomeworkItem>>(getSavedHomework())
    val homeworkList: StateFlow<List<HomeworkItem>> = _homeworkList.asStateFlow()

    fun saveHomeworkAttachment(uri: Uri, isPdf: Boolean): String? {
        return try {
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver
            val extension = if (isPdf) "pdf" else "jpg"
            val directory = java.io.File(context.filesDir, "homework_attachments")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val uniqueName = "hw_attach_${System.currentTimeMillis()}.$extension"
            val file = java.io.File(directory, uniqueName)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("AttendanceViewModel", "Failed to save homework attachment: ${e.message}", e)
            null
        }
    }

    fun getFileName(uri: Uri): String {
        val context = getApplication<Application>()
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e("AttendanceViewModel", "Error getting openable display name", e)
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "attachment"
    }

    private fun getSavedHomework(): List<HomeworkItem> {
        val saved = sharedPrefs.getString("custom_homework_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 6) {
                val photoPath = if (parts.size > 6) parts[6].replace("~", "|") else ""
                val pdfPath = if (parts.size > 7) parts[7].replace("~", "|") else ""
                val pdfName = if (parts.size > 8) parts[8].replace("~", "|") else ""
                HomeworkItem(
                    parts[0], parts[1], parts[2], parts[3], parts[4],
                    parts[5].toLongOrNull() ?: System.currentTimeMillis(),
                    photoPath, pdfPath, pdfName
                )
            } else null
        }
    }

    fun addHomework(title: String, description: String, studentClass: String, section: String, dueDate: String, photoPath: String = "", pdfPath: String = "", pdfName: String = "") {
        val current = _homeworkList.value.toMutableList()
        val cleanedTitle = title.trim().replace("|", " ").replace("\n", " ")
        val cleanedDesc = description.trim().replace("|", " ").replace("\n", " ")
        val cleanedPhotoPath = photoPath.trim().replace("|", "~")
        val cleanedPdfPath = pdfPath.trim().replace("|", "~")
        val cleanedPdfName = pdfName.trim().replace("|", "~")
        current.add(0, HomeworkItem(cleanedTitle, cleanedDesc, studentClass.trim(), section.trim(), dueDate.trim(), System.currentTimeMillis(), cleanedPhotoPath, cleanedPdfPath, cleanedPdfName))
        _homeworkList.value = current
        saveHomeworkToPrefs(current)
    }

    fun deleteHomework(item: HomeworkItem) {
        val current = _homeworkList.value.toMutableList()
        if (current.remove(item)) {
            _homeworkList.value = current
            saveHomeworkToPrefs(current)
        }
    }

    private fun saveHomeworkToPrefs(list: List<HomeworkItem>) {
        val encoded = list.joinToString("||") { "${it.title}|${it.description}|${it.studentClass}|${it.section}|${it.dueDate}|${it.timestamp}|${it.photoPath}|${it.pdfPath}|${it.pdfName}" }
        sharedPrefs.edit().putString("custom_homework_list", encoded).apply()
    }

    // 7. Classwork state & management
    private val _classworkList = MutableStateFlow<List<ClassworkItem>>(getSavedClasswork())
    val classworkList: StateFlow<List<ClassworkItem>> = _classworkList.asStateFlow()

    private fun getSavedClasswork(): List<ClassworkItem> {
        val saved = sharedPrefs.getString("custom_classwork_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 6) {
                ClassworkItem(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5].toLongOrNull() ?: System.currentTimeMillis())
            } else null
        }
    }

    fun addClasswork(title: String, description: String, studentClass: String, section: String, date: String) {
        val current = _classworkList.value.toMutableList()
        val cleanedTitle = title.trim().replace("|", " ").replace("\n", " ")
        val cleanedDesc = description.trim().replace("|", " ").replace("\n", " ")
        current.add(0, ClassworkItem(cleanedTitle, cleanedDesc, studentClass.trim(), section.trim(), date.trim(), System.currentTimeMillis()))
        _classworkList.value = current
        saveClassworkToPrefs(current)
    }

    fun deleteClasswork(item: ClassworkItem) {
        val current = _classworkList.value.toMutableList()
        if (current.remove(item)) {
            _classworkList.value = current
            saveClassworkToPrefs(current)
        }
    }

    private fun saveClassworkToPrefs(list: List<ClassworkItem>) {
        val encoded = list.joinToString("||") { "${it.title}|${it.description}|${it.studentClass}|${it.section}|${it.date}|${it.timestamp}" }
        sharedPrefs.edit().putString("custom_classwork_list", encoded).apply()
    }

    private val _leaveRequests = MutableStateFlow<List<LeaveRequest>>(getSavedLeaves())
    val leaveRequests: StateFlow<List<LeaveRequest>> = _leaveRequests.asStateFlow()

    private fun getSavedLeaves(): List<LeaveRequest> {
        val prefs = getApplication<Application>().getSharedPreferences("toppers_student_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("all_student_leaves_list", null)
        if (saved.isNullOrEmpty()) return emptyList()
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 7) {
                LeaveRequest(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6])
            } else null
        }
    }

    fun submitLeaveRequest(studentId: String, studentName: String, type: String, dates: String, reason: String) {
        val prefs = getApplication<Application>().getSharedPreferences("toppers_student_prefs", Context.MODE_PRIVATE)
        val current = getSavedLeaves().toMutableList()
        val cleanedReason = reason.trim().replace("|", " ").replace("\n", " ")
        val newLeave = LeaveRequest(
            id = System.currentTimeMillis().toString(),
            studentId = studentId.trim(),
            studentName = studentName.trim(),
            type = type.trim(),
            dates = dates.trim(),
            reason = cleanedReason,
            status = "Pending Approval"
        )
        current.add(0, newLeave)
        _leaveRequests.value = current
        saveLeavesToPrefs(prefs, current)

        // Trigger push notification for submission
        notificationHelper.sendLeaveNotification(studentName.trim(), "applied/submitted", "Pending Approval")
    }

    fun updateLeaveStatus(id: String, newStatus: String) {
        val prefs = getApplication<Application>().getSharedPreferences("toppers_student_prefs", Context.MODE_PRIVATE)
        var targetStudentName = "Student"
        val current = _leaveRequests.value.map {
            if (it.id == id) {
                targetStudentName = it.studentName
                it.copy(status = newStatus)
            } else it
        }
        _leaveRequests.value = current
        saveLeavesToPrefs(prefs, current)

        // Trigger push notification for approval/rejection
        val actionText = if (newStatus == "Approved") "approved" else "rejected"
        notificationHelper.sendLeaveNotification(targetStudentName, actionText, newStatus)
    }

    private fun saveLeavesToPrefs(prefs: android.content.SharedPreferences, list: List<LeaveRequest>) {
        val encoded = list.joinToString("||") { 
            "${it.id}|${it.studentId}|${it.studentName}|${it.type}|${it.dates}|${it.reason}|${it.status}" 
        }
        prefs.edit().putString("all_student_leaves_list", encoded).apply()
    }

    private val _feeStructureList = MutableStateFlow<List<FeeStructureItem>>(getSavedFeeStructure())
    val feeStructureList: StateFlow<List<FeeStructureItem>> = _feeStructureList.asStateFlow()

    private fun getSavedFeeStructure(): List<FeeStructureItem> {
        val saved = sharedPrefs.getString("custom_fee_structure", null)
        if (saved.isNullOrEmpty()) {
            val defaultList = listOf(
                FeeStructureItem("Admission & Registration Fee", 5000, true),
                FeeStructureItem("Quarterly Tuition Fee", 15000, true),
                FeeStructureItem("Annual Examination Fee", 1500, false),
                FeeStructureItem("Sports & Co-curricular Activities", 800, false)
            )
            saveFeeStructureToPrefs(defaultList)
            return defaultList
        }
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) {
                FeeStructureItem(
                    parts[0],
                    parts[1].toIntOrNull() ?: 0,
                    parts[2].toBoolean()
                )
            } else null
        }
    }

    fun addFeeStructureItem(name: String, amount: Int, isMandatory: Boolean) {
        val current = _feeStructureList.value.toMutableList()
        val cleanedName = name.trim().replace("|", " ").replace("\n", " ")
        current.add(FeeStructureItem(cleanedName, amount, isMandatory))
        _feeStructureList.value = current
        saveFeeStructureToPrefs(current)
    }

    fun deleteFeeStructureItem(item: FeeStructureItem) {
        val current = _feeStructureList.value.toMutableList()
        if (current.remove(item)) {
            _feeStructureList.value = current
            saveFeeStructureToPrefs(current)
        }
    }

    private fun saveFeeStructureToPrefs(list: List<FeeStructureItem>) {
        val encoded = list.joinToString("||") { "${it.name}|${it.amount}|${it.isMandatory}" }
        sharedPrefs.edit().putString("custom_fee_structure", encoded).apply()
    }

    fun getStudentFees(studentId: String): List<StudentFeeItem> {
        val saved = sharedPrefs.getString("student_fees_$studentId", null)
        if (saved.isNullOrEmpty()) {
            val global = getSavedFeeStructure()
            val oldPaidState = sharedPrefs.getBoolean("fees_paid_state_$studentId", false)
            val defaultList = global.map { item ->
                StudentFeeItem(
                    id = java.util.UUID.randomUUID().toString(),
                    name = item.name,
                    amount = item.amount,
                    isPaid = if (item.isMandatory) true else oldPaidState
                )
            }
            saveStudentFees(studentId, defaultList)
            return defaultList
        }
        return saved.split("||").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 4) {
                StudentFeeItem(
                    id = parts[0],
                    name = parts[1],
                    amount = parts[2].toIntOrNull() ?: 0,
                    isPaid = parts[3].toBoolean()
                )
            } else null
        }
    }

    fun saveStudentFees(studentId: String, list: List<StudentFeeItem>) {
        val encoded = list.joinToString("||") { "${it.id}|${it.name}|${it.amount}|${it.isPaid}" }
        sharedPrefs.edit().putString("student_fees_$studentId", encoded).apply()
        val allPaid = list.all { it.isPaid }
        sharedPrefs.edit().putBoolean("fees_paid_state_$studentId", allPaid).apply()
    }

    fun addStudentFeeItem(studentId: String, name: String, amount: Int) {
        val current = getStudentFees(studentId).toMutableList()
        val cleanedName = name.trim().replace("|", " ").replace("\n", " ")
        current.add(
            StudentFeeItem(
                id = java.util.UUID.randomUUID().toString(),
                name = cleanedName,
                amount = amount,
                isPaid = false
            )
        )
        saveStudentFees(studentId, current)
    }

    fun deleteStudentFeeItem(studentId: String, itemId: String) {
        val current = getStudentFees(studentId).filter { it.id != itemId }
        saveStudentFees(studentId, current)
    }

    fun toggleStudentFeePaidStatus(studentId: String, itemId: String, isPaid: Boolean) {
        val current = getStudentFees(studentId).map {
            if (it.id == itemId) it.copy(isPaid = isPaid) else it
        }
        saveStudentFees(studentId, current)
    }

    fun sendFeeReminderNotification(studentId: String, studentName: String, amount: Int) {
        notificationHelper.sendFeeReminderNotification(studentName, amount)
    }
}

// Data models for new dynamic features
data class StudentFeeItem(val id: String, val name: String, val amount: Int, val isPaid: Boolean) : java.io.Serializable
data class StaffItem(val name: String, val role: String, val status: String = "Active") : java.io.Serializable
data class ExamSchedule(val subject: String, val studentClass: String, val date: String, val time: String, val maxMarks: String) : java.io.Serializable
data class AnnouncementItem(val title: String, val message: String, val targetClass: String, val timestamp: Long) : java.io.Serializable
data class CommunityMessage(val sender: String, val role: String, val message: String, val timestamp: Long) : java.io.Serializable
data class HomeworkItem(val title: String, val description: String, val studentClass: String, val section: String, val dueDate: String, val timestamp: Long, val photoPath: String = "", val pdfPath: String = "", val pdfName: String = "") : java.io.Serializable
data class ClassworkItem(val title: String, val description: String, val studentClass: String, val section: String, val date: String, val timestamp: Long) : java.io.Serializable
data class LeaveRequest(val id: String, val studentId: String, val studentName: String, val type: String, val dates: String, val reason: String, val status: String) : java.io.Serializable
data class FeeStructureItem(val name: String, val amount: Int, val isMandatory: Boolean) : java.io.Serializable

sealed interface UiState {
    object Success : UiState
    data class SuccessWithSync(val syncedCount: Int, val totalCount: Int) : UiState
    object Saving : UiState
    data class Error(val message: String) : UiState
}
