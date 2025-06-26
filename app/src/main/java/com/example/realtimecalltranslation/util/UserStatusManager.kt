package com.shojbahmed.androidrtc.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class UserStatusManager {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef: DatabaseReference = database.getReference("users")
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // Call this when the user logs in or opens the app
    fun setUserOnline() {
        currentUserId?.let { userId ->
            val userStatusRef = usersRef.child(userId).child("status")
            val presenceRef = usersRef.child(userId).child("lastOnline")

            userStatusRef.setValue("online")
            // Set a disconnect handler to mark the user as offline when they disconnect
            userStatusRef.onDisconnect().setValue("offline")
            presenceRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
        }
    }

    // Call this when the user logs out or closes the app
    fun setUserOffline() {
        currentUserId?.let { userId ->
            usersRef.child(userId).child("status").setValue("offline")
            usersRef.child(userId).child("lastOnline").setValue(ServerValue.TIMESTAMP)
        }
    }

    // TODO: Implement a function to observe the status of a specific user
    // fun observeUserStatus(userId: String): LiveData<String> { ... }

    // TODO: Implement a function to observe the status of all users or a list of users
    // fun observeMultipleUsersStatus(userIds: List<String>): LiveData<Map<String, String>> { ... }

    companion object {
        @Volatile
        private var INSTANCE: UserStatusManager? = null

        fun getInstance(): UserStatusManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserStatusManager().also { INSTANCE = it }
            }
    }
}
