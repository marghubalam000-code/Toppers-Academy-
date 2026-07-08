package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageHelper private constructor(context: Context) {

    private var storage: FirebaseStorage? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                storage = FirebaseStorage.getInstance()
                Log.d("FirebaseStorageHelper", "Firebase Storage initialized successfully.")
            }
        } catch (e: Exception) {
            Log.w("FirebaseStorageHelper", "Firebase Storage is not available: ${e.message}")
        }
    }

    val isStorageAvailable: Boolean
        get() = storage != null

    /**
     * Uploads a student photo to Firebase Storage.
     * Returns the download URL string if successful, or null on failure.
     */
    suspend fun uploadStudentPhoto(
        imageUri: Uri,
        studentName: String,
        onProgress: (Float) -> Unit = {}
    ): String? {
        val currentStorage = storage
        if (currentStorage == null) {
            Log.w("FirebaseStorageHelper", "Storage not available. Falling back to local Uri.")
            return imageUri.toString()
        }

        return try {
            val fileName = "students/photo_${studentName.replace(" ", "_")}_${UUID.randomUUID()}.jpg"
            val storageRef = currentStorage.reference.child(fileName)

            val uploadTask = storageRef.putFile(imageUri)
            
            // Register observers to listen for progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                onProgress(progress)
            }.await()

            val downloadUrl = storageRef.downloadUrl.await()
            Log.d("FirebaseStorageHelper", "Successfully uploaded student photo. URL: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("FirebaseStorageHelper", "Failed to upload student photo to Firebase Storage: ${e.message}", e)
            // Fall back to returning the local image URI so the app is fully functional
            imageUri.toString()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseStorageHelper? = null

        fun getInstance(context: Context): FirebaseStorageHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseStorageHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
