package com.example.realtimecalltranslation.firebase

import com.example.realtimecalltranslation.util.Constants
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

object FirebaseProvider {

    // Initialize Firebase Realtime Database
    // This ensures that the database instance is initialized once and reused.
    // By default, Firebase SDK handles persistence and offline capabilities.
    // Make sure google-services.json is correctly set up in your project.
    private val database: FirebaseDatabase by lazy {
        // You can specify a database URL if it's not the default one associated with your google-services.json
        // For example: Firebase.database("https://<YOUR-PROJECT-ID>.firebaseio.com/")
        // However, usually, Firebase.database is enough if google-services.json is correct.
        Firebase.database
    }

    /**
     * Gets a reference to the root of the Firebase Realtime Database.
     */
    fun getRootRef(): DatabaseReference {
        return database.reference
    }

    /**
     * Gets a reference to the 'presence' node where user online status and lastSeen are stored.
     * Path: /presence/{userId}
     */
    fun getPresenceRef(userId: String): DatabaseReference {
        return database.getReference(Constants.FIREBASE_DB_PRESENCE_NODE).child(userId)
    }

    /**
     * Gets a reference to the specific user's node under 'users'.
     * This can store public user information like name, photoUrl, fcmToken.
     * Path: /users/{userId}
     */
    fun getUserInfoRef(userId: String): DatabaseReference {
        return database.getReference(Constants.FIREBASE_DB_USERS_NODE).child(userId)
    }

    /**
     * Gets a reference to the 'call_requests' node for a specific user (callee).
     * This is where incoming call requests for the user will be stored.
     * Path: /call_requests/{calleeId}/{callId}
     */
    fun getCallRequestsRef(calleeId: String): DatabaseReference {
        return database.getReference(Constants.FIREBASE_DB_CALL_REQUESTS_NODE).child(calleeId)
    }

    /**
     * Gets a specific call request reference using calleeId and a unique callId.
     * Path: /call_requests/{calleeId}/{callId}
     */
    fun getSpecificCallRequestRef(calleeId: String, callId: String): DatabaseReference {
        return database.getReference(Constants.FIREBASE_DB_CALL_REQUESTS_NODE).child(calleeId).child(callId)
    }

    /**
     * Gets the current server time offset. Useful for consistent timestamps.
     * Path: /.info/serverTimeOffset
     */
    fun getServerTimeOffsetRef(): DatabaseReference {
        return FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
    }
}
