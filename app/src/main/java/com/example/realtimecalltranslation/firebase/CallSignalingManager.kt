package com.example.realtimecalltranslation.firebase

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.example.realtimecalltranslation.util.Constants
import com.example.realtimecalltranslation.firebase.CallRequest // Added import
import com.example.realtimecalltranslation.firebase.FirebaseProvider // Added import


class CallSignalingManager {

    private val tag = "CallSignalingManager"

    fun sendCallRequest(
        callRequest: CallRequest,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d(tag, "sendCallRequest called with: $callRequest")

        if (callRequest.calleeId.isBlank() || callRequest.callId.isBlank()) {
            Log.e(tag, "Callee ID ('${callRequest.calleeId}') or Call ID ('${callRequest.callId}') is blank in CallRequest. Aborting send.")
            onFailure("Callee ID or Call ID is blank.")
            return
        }

        val callRequestWithTimestamp = callRequest.copy(timestamp = ServerValue.TIMESTAMP)
        Log.d(tag, "CallRequest with timestamp: $callRequestWithTimestamp")

        val callRequestRef = FirebaseProvider.getSpecificCallRequestRef(
            calleeId = callRequest.calleeId,
            callId = callRequest.callId
        )
        Log.d(tag, "Firebase call_request ref: ${callRequestRef.toString()}") // Changed .path to .toString()

        Log.d(tag, "Attempting to set value in Firebase for callId ${callRequest.callId} to callee ${callRequest.calleeId}")
        callRequestRef.setValue(callRequestWithTimestamp)
            .addOnSuccessListener {
                Log.i(tag, "Successfully sent call request to Firebase for callee: ${callRequest.calleeId}, callId: ${callRequest.callId}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to send call request to Firebase for callee: ${callRequest.calleeId}, callId: ${callRequest.callId}. Error: ${e.message}", e)
                onFailure("Firebase setValue failed: ${e.message}")
            }
    }

    private val activeListeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    fun listenForIncomingCalls(
        myUserId: String,
        onIncomingCall: (callRequest: CallRequest) -> Unit,
        onCallRequestUpdated: (callRequest: CallRequest) -> Unit,
        onCallRequestRemoved: (callRequestId: String) -> Unit
    ) {
        if (myUserId.isBlank()) {
            Log.e(tag, "Cannot listen for calls, myUserId is blank.")
            return
        }
        val callRequestsRef = FirebaseProvider.getCallRequestsRef(myUserId)

        activeListeners[callRequestsRef]?.let { callRequestsRef.removeEventListener(it) }
        activeListeners.remove(callRequestsRef)

        val listener = object : ValueEventListener {
            private val seenCallIds = mutableSetOf<String>()

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d(tag, "No call requests for user $myUserId")
                    return
                }
                Log.d(tag, "Call requests data changed for $myUserId. Children count: ${snapshot.childrenCount}")
                val currentCallIdsOnServer = mutableSetOf<String>()

                snapshot.children.forEach { callSnapshot ->
                    val callRequest = callSnapshot.getValue(CallRequest::class.java)
                    callRequest?.callId = callSnapshot.key ?: ""

                    if (callRequest != null && callRequest.callId.isNotBlank()) {
                        currentCallIdsOnServer.add(callRequest.callId)
                        if (!seenCallIds.contains(callRequest.callId)) {
                            if (callRequest.status == Constants.CALL_STATUS_PENDING) {
                                Log.d(tag, "New incoming call detected: ${callRequest.callId} from ${callRequest.callerId}")
                                onIncomingCall(callRequest)
                            }
                            seenCallIds.add(callRequest.callId)
                        } else {
                            Log.d(tag, "Call request updated: ${callRequest.callId}, status: ${callRequest.status}")
                            onCallRequestUpdated(callRequest)
                        }
                    }
                }

                val removedCallIds = seenCallIds.filterNot { it in currentCallIdsOnServer }
                removedCallIds.forEach { removedId ->
                    Log.d(tag, "Call request removed: $removedId")
                    onCallRequestRemoved(removedId)
                    seenCallIds.remove(removedId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(tag, "Call request listener cancelled for $myUserId", error.toException())
            }
        }
        callRequestsRef.addValueEventListener(listener)
        activeListeners[callRequestsRef] = listener
        Log.d(tag, "Started listening for incoming calls for user: $myUserId")
    }

    fun updateCallRequestStatus(ownerId: String, callRequestId: String, newStatus: String) {
        if (ownerId.isBlank() || callRequestId.isBlank()) {
            Log.e(tag, "ownerId or callRequestId cannot be blank for updating status.")
            return
        }
        val callStatusRef = FirebaseProvider.getSpecificCallRequestRef(ownerId, callRequestId).child("status")
        callStatusRef.setValue(newStatus)
            .addOnSuccessListener {
                Log.d(tag, "Call request $callRequestId status updated to $newStatus for owner $ownerId")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to update call request $callRequestId status: ${e.message}", e)
            }
        FirebaseProvider.getSpecificCallRequestRef(ownerId, callRequestId).child("timestamp").setValue(ServerValue.TIMESTAMP)
    }

    fun removeCallRequest(ownerId: String, callRequestId: String) {
        if (ownerId.isBlank() || callRequestId.isBlank()) {
            Log.e(tag, "ownerId or callRequestId cannot be blank for removing request.")
            return
        }
        FirebaseProvider.getSpecificCallRequestRef(ownerId, callRequestId).removeValue()
            .addOnSuccessListener {
                Log.d(tag, "Call request $callRequestId removed for owner $ownerId")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to remove call request $callRequestId: ${e.message}", e)
            }
    }

    fun stopListeningForIncomingCalls(myUserId: String) {
        if (myUserId.isBlank()) return
        val callRequestsRef = FirebaseProvider.getCallRequestsRef(myUserId)
        activeListeners[callRequestsRef]?.let {
            callRequestsRef.removeEventListener(it)
            activeListeners.remove(callRequestsRef)
            Log.d(tag, "Stopped listening for incoming calls for user: $myUserId")
        }
    }

    fun cleanupAllListeners() {
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
        Log.d(tag, "All active call signaling listeners removed.")
    }
}