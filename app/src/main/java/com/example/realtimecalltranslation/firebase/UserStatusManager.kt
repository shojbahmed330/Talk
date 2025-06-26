package com.example.realtimecalltranslation.firebase

import android.util.Log
import com.example.realtimecalltranslation.util.Constants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class UserStatusManager {

    private val tag = "UserStatusManager"

    // Reference to the current user's presence node (e.g., /presence/{userId})
    // This needs to be set when the user logs in or the app starts with a known user.
    private var currentUserPresenceRef = FirebaseProvider.getRootRef() // Placeholder, needs to be specific
    private var currentUserIdGlobal: String? = null

    // Listener for Firebase's server time offset to calculate client-server time difference
    private var serverTimeOffsetEventListener: ValueEventListener? = null
    private var serverTimeOffsetInMillis: Long = 0L


    init {
        attachServerTimeOffsetListener()
    }

    private fun attachServerTimeOffsetListener() {
        val offsetRef = FirebaseProvider.getServerTimeOffsetRef()
        serverTimeOffsetEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java)
                if (offset != null) {
                    serverTimeOffsetInMillis = offset
                    Log.d(tag, "Server time offset updated: $serverTimeOffsetInMillis ms")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Server time offset listener was cancelled.", error.toException())
            }
        }
        offsetRef.addValueEventListener(serverTimeOffsetEventListener!!)
    }

    /**
     * Sets the current user for whom the status will be managed.
     * Call this after user authentication or when the app knows the current user.
     */
    fun setCurrentUser(userId: String) {
        if (userId.isBlank()) {
            Log.e(tag, "setCurrentUser: userId cannot be blank.")
            return
        }
        currentUserIdGlobal = userId
        currentUserPresenceRef = FirebaseProvider.getPresenceRef(userId)
        Log.d(tag, "UserStatusManager initialized for user: $userId")
    }

    /**
     * Updates the user's online status and last seen timestamp in Firebase Realtime Database.
     * Also updates the FCM token in the user's public profile if provided.
     */
    fun updateUserOnlineStatus(isOnline: Boolean, fcmToken: String? = null) {
        val currentUserId = currentUserIdGlobal
        if (currentUserId == null) {
            Log.w(tag, "Cannot update status, current user ID not set. Call setCurrentUser() first.")
            return
        }

        val status = if (isOnline) Constants.PRESENCE_STATUS_ONLINE else Constants.PRESENCE_STATUS_OFFLINE
        val presenceData = mapOf(
            "status" to status,
            "lastSeen" to ServerValue.TIMESTAMP // Use server timestamp for consistency
        )

        currentUserPresenceRef.setValue(presenceData)
            .addOnSuccessListener {
                Log.d(tag, "User ($currentUserId) status updated to: $status")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update user ($currentUserId) status to $status: ${e.message}", e)
            }

        // Update FCM token in the user's public profile node (/users/{userId}/fcmToken)
        if (fcmToken != null) {
            FirebaseProvider.getUserInfoRef(currentUserId).child("fcmToken").setValue(fcmToken)
                .addOnSuccessListener {
                    Log.d(tag, "FCM token updated for user: $currentUserId")
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to update FCM token for user $currentUserId: ${e.message}", e)
                }
        }
    }

    /**
     * Configures Firebase's onDisconnect() operations to set the user's status to offline
     * and update their lastSeen timestamp when the client disconnects abruptly.
     */
    fun setUserOfflineOnDisconnect() {
        val currentUserId = currentUserIdGlobal
        if (currentUserId == null) {
            Log.w(tag, "Cannot set onDisconnect, current user ID not set. Call setCurrentUser() first.")
            return
        }

        val offlinePresenceData = mapOf(
            "status" to Constants.PRESENCE_STATUS_OFFLINE,
            "lastSeen" to ServerValue.TIMESTAMP
        )

        currentUserPresenceRef.onDisconnect().setValue(offlinePresenceData)
            .addOnSuccessListener {
                Log.d(tag, "onDisconnect configured for user: $currentUserId to set status offline.")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to configure onDisconnect for user $currentUserId: ${e.message}", e)
            }
    }

    /**
     * Observes the online status of a specific user.
     * @param targetUserId The ID of the user whose status is to be observed.
     * @param onStatusChanged Callback function that receives the online status (Boolean).
     *                        The boolean will be true if status is "online", false otherwise.
     *                        It also provides the lastSeen timestamp.
     */
    fun observeUserOnlineStatus(targetUserId: String, onStatusChanged: (isOnline: Boolean, lastSeen: Long?) -> Unit): ValueEventListener {
        val presenceRef = FirebaseProvider.getPresenceRef(targetUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val status = snapshot.child("status").getValue(String::class.java)
                    val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java)
                    val isOnline = status == Constants.PRESENCE_STATUS_ONLINE

                    // Adjust lastSeen timestamp using serverTimeOffset if needed for display,
                    // though for simple online/offline, server timestamp is fine.
                    // val actualLastSeen = if (lastSeen != null) lastSeen + serverTimeOffsetInMillis else null

                    onStatusChanged(isOnline, lastSeen)
                    Log.d(tag, "Observed status for $targetUserId: $status, LastSeen: $lastSeen")
                } else {
                    // Node doesn't exist, assume offline
                    onStatusChanged(false, null)
                    Log.d(tag, "No presence data for $targetUserId, assuming offline.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Listener for user $targetUserId status cancelled.", error.toException())
                // Optionally notify about the error
                onStatusChanged(false, null) // Assume offline on error or cancellation
            }
        }
        presenceRef.addValueEventListener(listener)
        Log.d(tag, "Started observing status for user: $targetUserId")
        return listener // Return listener so it can be removed later if needed
    }

    /**
     * Stops observing a user's online status.
     */
    fun stopObservingUserStatus(targetUserId: String, listener: ValueEventListener) {
        FirebaseProvider.getPresenceRef(targetUserId).removeEventListener(listener)
        Log.d(tag, "Stopped observing status for user: $targetUserId")
    }

    /**
     * Cleans up the server time offset listener. Call when UserStatusManager is no longer needed.
     */
    fun cleanup() {
        serverTimeOffsetEventListener?.let {
            FirebaseProvider.getServerTimeOffsetRef().removeEventListener(it)
            Log.d(tag, "Server time offset listener removed.")
        }
        serverTimeOffsetEventListener = null
    }
}
