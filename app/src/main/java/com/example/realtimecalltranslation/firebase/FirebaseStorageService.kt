package com.example.realtimecalltranslation.firebase

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object FirebaseStorageService {

    private const val PROFILE_PICTURES_PATH = "profile_pictures"

    // Function to upload an image and return the download URL
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String? {
        return try {
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"
            val storageRef = Firebase.storage.reference.child("$PROFILE_PICTURES_PATH/$fileName")
            storageRef.putFile(imageUri).await() // Upload the file
            val downloadUrl = storageRef.downloadUrl.await()?.toString()
            downloadUrl
        } catch (e: Exception) {
            // Log.e("FirebaseStorageService", "Error uploading profile picture", e)
            null
        }
    }

    // Function to delete an old profile picture if a new one is uploaded
    suspend fun deleteProfilePicture(fileUrl: String?): Boolean {
        if (fileUrl.isNullOrEmpty()) return false
        return try {
            val storageRef = Firebase.storage.getReferenceFromUrl(fileUrl)
            storageRef.delete().await()
            true
        } catch (e: Exception) {
            // Log.e("FirebaseStorageService", "Error deleting profile picture", e)
            false
        }
    }
}