package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseAuthHelper private constructor(private val context: Context) {

    private var auth: FirebaseAuth? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null) {
                auth = FirebaseAuth.getInstance()
                Log.d("FirebaseAuthHelper", "Firebase Auth initialized successfully.")
            }
        } catch (e: Exception) {
            Log.w("FirebaseAuthHelper", "Firebase Auth could not be initialized: ${e.message}")
        }
    }

    val isFirebaseAvailable: Boolean
        get() = auth != null

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val currentEmail: String?
        get() = auth?.currentUser?.email

    val isUserLoggedIn: Boolean
        get() = auth?.currentUser != null

    /**
     * Protected Route/Access Validation logic.
     * Checks if the user is authorized as an Admin.
     */
    fun isAuthorizedAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val prefs = context.getSharedPreferences("toppers_admin_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed_emails_list", null)
        val allowedSet = saved ?: setOf("marghubalam000@gmail.com", "admin@toppers.com", "principal@toppers.com")
        val emailNormalized = email.trim().lowercase()
        return allowedSet.any { it.trim().lowercase() == emailNormalized }
    }

    /**
     * Validates email format according to standard RFC 5322 regex.
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return email.matches(emailPattern.toRegex())
    }

    /**
     * Securely performs Firebase sign in.
     */
    fun signIn(email: String, pass: String, onComplete: (Result<FirebaseUser>) -> Unit) {
        val currentAuth = auth
        if (currentAuth == null) {
            onComplete(Result.failure(IllegalStateException("Firebase Auth is not available (Offline/Local Mode)")))
            return
        }

        currentAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = currentAuth.currentUser
                    if (user != null) {
                        onComplete(Result.success(user))
                    } else {
                        onComplete(Result.failure(IllegalStateException("User is null after successful login")))
                    }
                } else {
                    onComplete(Result.failure(task.exception ?: Exception("Authentication failed")))
                }
            }
    }

    /**
     * Signs out the current user, clearing any cached session credentials.
     */
    fun signOut() {
        auth?.signOut()
    }

    /**
     * Dispatch password reset email.
     */
    fun sendPasswordReset(email: String, onComplete: (Boolean) -> Unit) {
        val currentAuth = auth
        if (currentAuth == null) {
            onComplete(false)
            return
        }
        currentAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * Update password for the currently signed-in user.
     */
    fun updatePassword(newPass: String, onComplete: (Boolean) -> Unit) {
        val user = auth?.currentUser
        if (user == null) {
            onComplete(false)
            return
        }
        user.updatePassword(newPass)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthHelper? = null

        fun getInstance(context: Context): FirebaseAuthHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseAuthHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
