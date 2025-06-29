package com.example.realtimecalltranslation.firebase

import com.example.realtimecalltranslation.ui.theme.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseUserService {

    private val database = Firebase.database.reference.child("users")

    // Function to save/update user profile data (name and profilePicUrl)
    fun updateUserProfile(userId: String, name: String, profilePicUrl: String?) {
        val userData = mapOf(
            "name" to name,
            "profilePicUrl" to profilePicUrl
        )
        database.child(userId).updateChildren(userData)
            .addOnFailureListener {
                // Log.e("FirebaseUserService", "Failed to update user profile for $userId", it)
            }
    }

    // Function to get a user's profile picture URL
    suspend fun getUserProfilePicUrl(userId: String): String? =
        suspendCancellableCoroutine { continuation ->
            database.child(userId).child("profilePicUrl")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val url = snapshot.getValue(String::class.java)
                        continuation.resume(url)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log.e("FirebaseUserService", "Failed to get profile pic URL for $userId", error.toException())
                        continuation.resume(null) // or resumeWithException(error.toException())
                    }
                })
        }

    // Function to get a full User object (name and profilePicUrl)
    suspend fun getUser(userId: String): User? = suspendCancellableCoroutine { continuation ->
        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                        ?: userId // Fallback name
                    val profilePicUrl = snapshot.child("profilePicUrl").getValue(String::class.java)
                    // Assuming phone number is the userId for creating User object locally
                    continuation.resume(
                        User(
                            id = userId,
                            name = name,
                            phone = userId,
                            profilePicUrl = profilePicUrl
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        })
    }
}