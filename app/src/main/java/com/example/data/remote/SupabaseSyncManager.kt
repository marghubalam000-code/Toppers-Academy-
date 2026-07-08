package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseSyncManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val sharedPrefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE)

    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.LocalOnly("Not configured"))
    val syncStatus: StateFlow<SyncState> = _syncStatus

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    init {
        // If there are no credentials saved yet, pre-populate them with the user's credentials!
        val currentUrl = sharedPrefs.getString("supabase_url", "") ?: ""
        val currentKey = sharedPrefs.getString("supabase_key", "") ?: ""
        if (currentUrl.isEmpty() || currentKey.isEmpty()) {
            saveCredentials(
                "https://kwtpybvahhtyfraojpgr.supabase.co",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt3dHB5YnZhaGh0eWZyYW9qcGdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkxODI0NDEsImV4cCI6MjA5NDc1ODQ0MX0.ZMOf884tmVwSpfc0qcMt9J2vtIKdKYsGUgQB5s4f9nY"
            )
        } else {
            updateSyncStatus()
        }
    }

    fun getSupabaseUrl(): String {
        var url = sharedPrefs.getString("supabase_url", "https://kwtpybvahhtyfraojpgr.supabase.co") ?: "https://kwtpybvahhtyfraojpgr.supabase.co"
        if (url.isEmpty()) {
            url = "https://kwtpybvahhtyfraojpgr.supabase.co"
        }
        url = url.trim()
        if (url.endsWith("/rest/v1/")) {
            url = url.substringBefore("/rest/v1/")
        } else if (url.endsWith("/rest/v1")) {
            url = url.substringBefore("/rest/v1")
        }
        if (url.endsWith("/")) {
            url = url.removeSuffix("/")
        }
        return url
    }

    fun getSupabaseKey(): String {
        val key = sharedPrefs.getString("supabase_key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt3dHB5YnZhaGh0eWZyYW9qcGdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkxODI0NDEsImV4cCI6MjA5NDc1ODQ0MX0.ZMOf884tmVwSpfc0qcMt9J2vtIKdKYsGUgQB5s4f9nY")
            ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt3dHB5YnZhaGh0eWZyYW9qcGdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkxODI0NDEsImV4cCI6MjA5NDc1ODQ0MX0.ZMOf884tmVwSpfc0qcMt9J2vtIKdKYsGUgQB5s4f9nY"
        return if (key.isEmpty()) "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imt3dHB5YnZhaGh0eWZyYW9qcGdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkxODI0NDEsImV4cCI6MjA5NDc1ODQ0MX0.ZMOf884tmVwSpfc0qcMt9J2vtIKdKYsGUgQB5s4f9nY" else key
    }

    fun saveCredentials(url: String, key: String) {
        sharedPrefs.edit()
            .putString("supabase_url", url.trim())
            .putString("supabase_key", key.trim())
            .apply()
        updateSyncStatus()
    }

    fun clearCredentials() {
        sharedPrefs.edit()
            .remove("supabase_url")
            .remove("supabase_key")
            .apply()
        updateSyncStatus()
    }

    private fun updateSyncStatus() {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (url.isNotEmpty() && key.isNotEmpty() && url.startsWith("http")) {
            _syncStatus.value = SyncState.Connected
        } else {
            _syncStatus.value = SyncState.LocalOnly("Supabase URL or Key is missing")
        }
    }

    fun isSupabaseConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        return url.isNotEmpty() && key.isNotEmpty() && url.startsWith("http")
    }

    suspend fun syncStudent(student: Student): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) {
            Log.d("SupabaseSyncManager", "Supabase not configured, local simulation success for student: ${student.name}")
            return@withContext true
        }

        try {
            val jsonObject = JSONObject().apply {
                put("student_id", student.studentId)
                put("name", student.name)
                put("father_name", student.fatherName)
                put("mother_name", student.motherName)
                put("student_class", student.studentClass)
                put("section", student.section)
                put("roll_number", student.rollNumber)
                put("mobile", student.mobile)
                put("email", student.email)
                put("gender", student.gender)
                put("dob", student.dob)
                put("address", student.address)
                put("aadhaar", student.aadhaar)
                put("admission_date", student.admissionDate)
                put("photo", student.photo)
                put("status", student.status)
                put("password", student.password)
                put("main_subject", student.mainSubject)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            // We do an upsert in Supabase using POST with Prefer headers: resolution=merge-duplicates or on_conflict
            val request = Request.Builder()
                .url("$url/rest/v1/students")
                .post(requestBody)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseSyncManager", "Synced student successfully to Supabase.")
                    true
                } else {
                    Log.w("SupabaseSyncManager", "Supabase student sync failed: code ${response.code} - ${response.message}")
                    // Also try to read body if any
                    val body = response.body?.string()
                    Log.w("SupabaseSyncManager", "Error body: $body")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception syncing student to Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun deleteStudentRemote(studentId: String): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext true

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/students?student_id=eq.$studentId")
                .delete()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncAttendance(record: AttendanceRecord): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) {
            Log.d("SupabaseSyncManager", "Supabase not configured, local simulation success for attendance: ${record.studentName}")
            return@withContext true
        }

        try {
            val jsonObject = JSONObject().apply {
                put("attendance_id", record.attendanceId)
                put("student_id", record.studentId)
                put("student_name", record.studentName)
                put("student_class", record.studentClass)
                put("section", record.section)
                put("roll_number", record.rollNumber)
                put("date", record.date)
                put("status", record.status)
                put("created_at", record.createdAt)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$url/rest/v1/attendance")
                .post(requestBody)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseSyncManager", "Synced attendance successfully to Supabase.")
                    true
                } else {
                    Log.w("SupabaseSyncManager", "Supabase attendance sync failed: code ${response.code} - ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception syncing attendance to Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAttendanceRemote(attendanceId: String): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext true

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/attendance?attendance_id=eq.$attendanceId")
                .delete()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchStudents(): List<Student>? = withContext(Dispatchers.IO) {
        _lastError.value = null
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) {
            _lastError.value = "Supabase URL or API Key is not configured."
            return@withContext null
        }

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/students")
                .get()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyString)
                    val studentsList = mutableListOf<Student>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val student = Student(
                            id = 0,
                            studentId = jsonObject.optString("student_id"),
                            name = jsonObject.optString("name"),
                            fatherName = jsonObject.optString("father_name"),
                            motherName = jsonObject.optString("mother_name"),
                            mobile = jsonObject.optString("mobile"),
                            email = jsonObject.optString("email"),
                            studentClass = jsonObject.optString("student_class"),
                            section = jsonObject.optString("section"),
                            rollNumber = jsonObject.optString("roll_number"),
                            gender = jsonObject.optString("gender"),
                            dob = jsonObject.optString("dob"),
                            address = jsonObject.optString("address"),
                            aadhaar = jsonObject.optString("aadhaar"),
                            admissionDate = jsonObject.optString("admission_date"),
                            photo = jsonObject.optString("photo"),
                            status = jsonObject.optString("status"),
                            password = jsonObject.optString("password", ""),
                            mainSubject = jsonObject.optString("main_subject", "")
                        )
                        studentsList.add(student)
                    }
                    studentsList
                } else {
                    Log.w("SupabaseSyncManager", "Supabase students fetch failed: code ${response.code} - $bodyString")
                    if (response.code == 404 || bodyString.contains("PGRST205") || bodyString.contains("Could not find the table")) {
                        _lastError.value = "Table 'students' not found. Please create the table in Supabase SQL Editor. See the guide below."
                    } else if (response.code == 401 || bodyString.contains("Invalid API key") || bodyString.contains("JWT")) {
                        _lastError.value = "Unauthorized: Invalid Supabase Project URL or Service Role/Anon Key."
                    } else {
                        _lastError.value = "Failed to fetch 'students' table (Code ${response.code})."
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception fetching students from Supabase: ${e.message}", e)
            _lastError.value = "Network Connection Error: ${e.message ?: "Failed to connect to Supabase server."}"
            null
        }
    }

    suspend fun fetchAttendance(): List<AttendanceRecord>? = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/attendance")
                .get()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyString)
                    val recordsList = mutableListOf<AttendanceRecord>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val record = AttendanceRecord(
                            id = 0,
                            attendanceId = jsonObject.optString("attendance_id"),
                            studentId = jsonObject.optString("student_id"),
                            studentName = jsonObject.optString("student_name"),
                            studentClass = jsonObject.optString("student_class"),
                            section = jsonObject.optString("section"),
                            rollNumber = jsonObject.optString("roll_number"),
                            date = jsonObject.optString("date"),
                            status = jsonObject.optString("status"),
                            createdAt = jsonObject.optLong("created_at", System.currentTimeMillis()),
                            isSynced = true
                        )
                        recordsList.add(record)
                    }
                    recordsList
                } else {
                    Log.w("SupabaseSyncManager", "Supabase attendance fetch failed: code ${response.code} - $bodyString")
                    if (response.code == 404 || bodyString.contains("PGRST205") || bodyString.contains("Could not find the table")) {
                        _lastError.value = "Table 'attendance' not found. Please create the table in Supabase SQL Editor. See the guide below."
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception fetching attendance from Supabase: ${e.message}", e)
            null
        }
    }

    suspend fun fetchAllowedEmails(): List<String>? = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/allowed_users")
                .get()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(bodyString)
                    val list = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val email = jsonObject.optString("email", "")
                        if (email.isNotEmpty()) {
                            list.add(email.trim().lowercase())
                        }
                    }
                    Log.d("SupabaseSyncManager", "Fetched ${list.size} allowed emails from Supabase: $list")
                    list
                } else {
                    Log.w("SupabaseSyncManager", "Supabase allowed_users fetch returned code ${response.code}: $bodyString")
                    if (response.code == 404 || bodyString.contains("PGRST205") || bodyString.contains("Could not find the table")) {
                        _lastError.value = "Table 'allowed_users' not found. Please create the table in Supabase SQL Editor. See the guide below."
                    }
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception fetching allowed_users from Supabase: ${e.message}", e)
            null
        }
    }

    suspend fun addAllowedEmailRemote(email: String): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext false

        try {
            val jsonObject = JSONObject().apply {
                put("email", email.trim().lowercase())
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$url/rest/v1/allowed_users")
                .post(requestBody)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseSyncManager", "Successfully added $email to Supabase allowed_users")
                    true
                } else {
                    Log.w("SupabaseSyncManager", "Failed to add allowed email to Supabase: code ${response.code} - ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception adding allowed email to Supabase: ${e.message}", e)
            false
        }
    }

    suspend fun removeAllowedEmailRemote(email: String): Boolean = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) return@withContext false

        try {
            val request = Request.Builder()
                .url("$url/rest/v1/allowed_users?email=eq.${email.trim().lowercase()}")
                .delete()
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("SupabaseSyncManager", "Successfully deleted $email from Supabase allowed_users")
                    true
                } else {
                    Log.w("SupabaseSyncManager", "Failed to delete allowed email from Supabase: code ${response.code} - ${response.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception deleting allowed email from Supabase: ${e.message}", e)
            false
        }
    }

    fun getGoogleWebClientId(): String {
        return sharedPrefs.getString("google_web_client_id", "") ?: ""
    }

    fun saveGoogleWebClientId(clientId: String) {
        sharedPrefs.edit()
            .putString("google_web_client_id", clientId.trim())
            .apply()
    }

    suspend fun signInWithSupabaseGoogle(idToken: String): Result<String> = withContext(Dispatchers.IO) {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        if (!isSupabaseConfigured()) {
            return@withContext Result.failure(Exception("Supabase is not configured"))
        }

        try {
            val jsonObject = JSONObject().apply {
                put("provider", "google")
                put("id_token", idToken)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$url/auth/v1/token?grant_type=id_token")
                .post(requestBody)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val responseJson = JSONObject(bodyString)
                    val userJson = responseJson.optJSONObject("user")
                    val email = userJson?.optString("email") ?: ""
                    Log.d("SupabaseSyncManager", "Supabase Google OAuth authenticated user: $email")
                    Result.success(email)
                } else {
                    Log.e("SupabaseSyncManager", "Supabase Google auth token exchange failed: code ${response.code} - $bodyString")
                    val errorMsg = try {
                        JSONObject(bodyString).optString("error_description", response.message)
                    } catch (e: Exception) {
                        response.message
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception during Supabase Google OAuth: ${e.message}", e)
            Result.failure(e)
        }
    }
}
