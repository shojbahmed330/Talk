package com.shojbahmed.androidrtc.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.ServerValue

// Correct imports for WebRTC classes
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class CallSignalingManager(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val callsRef: DatabaseReference = database.getReference("calls")
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private var activeCallId: String? = null // To keep track of the current call being handled

    // Callbacks for signaling events
    var onNewCallOfferListener: ((offerSdp: SessionDescription, callerId: String, callId: String) -> Unit)? = null
    var onCallAnswerListener: ((answerSdp: SessionDescription, callId: String) -> Unit)? = null
    var onIceCandidateListener: ((iceCandidate: IceCandidate, callId: String) -> Unit)? = null
    var onCallRejectedListener: ((callId: String, rejecterId: String) -> Unit)? = null
    var onCallEndedListener: ((callId: String) -> Unit)? = null

    // To manage listeners and clean them up
    private val activeListeners = mutableMapOf<String, MutableList<Pair<DatabaseReference, Any>>>()

    companion object {
        private const val TAG = "CallSignalingManager"

        @Volatile
        private var INSTANCE: CallSignalingManager? = null

        fun getInstance(context: Context): CallSignalingManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallSignalingManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    /**
     * Sends a call offer to the target user.
     * The offer is stored under /calls/{calleeId}/{callId}
     */
    fun sendCallOffer(targetUserId: String, sdp: SessionDescription) {
        currentUserId?.let { callerId ->
            val callId = "offer_${callerId}_to_${targetUserId}_${System.currentTimeMillis()}"
            this.activeCallId = callId

            val offerNodePath = "$targetUserId/$callId"
            val offerData = hashMapOf(
                "type" to "OFFER",
                "sdp" to sdp.description,
                "callerId" to callerId,
                "calleeId" to targetUserId,
                "callId" to callId,
                "status" to "pending",
                "timestamp" to ServerValue.TIMESTAMP
            )

            callsRef.child(offerNodePath).setValue(offerData).addOnSuccessListener {
                Log.d(TAG, "Offer sent to $targetUserId for call $callId at $offerNodePath")
                listenForCallUpdates(callerId, callId, targetUserId)
            }.addOnFailureListener {
                Log.e(TAG, "Failed to send offer for call $callId to $targetUserId", it)
                this.activeCallId = null
            }
        }
    }

    /**
     * Sends an answer to the call offer.
     * The answer is stored under /calls/{callerId}/{callId}/answer
     */
    fun sendCallAnswer(callerId: String, callId: String, sdp: SessionDescription) {
        val answerData = hashMapOf(
            "type" to "ANSWER",
            "sdp" to sdp.description,
            "timestamp" to ServerValue.TIMESTAMP
        )

        val answerPath = "$callerId/$callId/answer"
        callsRef.child(answerPath).setValue(answerData).addOnSuccessListener {
            Log.d(TAG, "Answer sent for call $callId to $callerId at $answerPath")
            val originalOfferCalleeId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknownCallee"
            callsRef.child(originalOfferCalleeId).child(callId).child("status").setValue("answered")
            this.activeCallId = callId
        }.addOnFailureListener {
            Log.e(TAG, "Failed to send answer for call $callId", it)
        }
    }

    /**
     * Sends a call rejection.
     * The rejection is stored under /calls/{callerId}/{callId}/rejection
     */
    fun sendCallRejection(callerId: String, callId: String) {
        val rejecterId = currentUserId ?: "unknown"
        val rejectionData = hashMapOf(
            "type" to "REJECTION",
            "rejecterId" to rejecterId,
            "timestamp" to ServerValue.TIMESTAMP
        )
        val rejectionPath = "$callerId/$callId/rejection"
        callsRef.child(rejectionPath).setValue(rejectionData).addOnSuccessListener {
            Log.d(TAG, "Rejection sent for call $callId to $callerId")
            val originalOfferCalleeId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknownCallee"
            callsRef.child(originalOfferCalleeId).child(callId).child("status").setValue("rejected")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to send rejection for call $callId", it)
        }
        cleanupCallListeners(callId)
        if (this.activeCallId == callId) this.activeCallId = null
    }

    /**
     * Sends an ICE candidate to the peer.
     * ICE candidates are stored under /calls/{peerId}/{callId}/iceCandidates/{pushId}
     */
    fun sendIceCandidate(peerId: String, callId: String, candidate: IceCandidate) {
        val iceCandidateData = hashMapOf(
            "type" to "ICE_CANDIDATE",
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp,
            "senderId" to currentUserId
        )
        val icePath = "$peerId/$callId/iceCandidates"
        callsRef.child(icePath).push().setValue(iceCandidateData).addOnSuccessListener {
            // Log.d(TAG, "ICE candidate sent for call $callId to $peerId")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to send ICE for call $callId to $peerId", it)
        }
    }

    /**
     * Listens for incoming call offers under /calls/{myUserId}/{callId}
     */
    fun listenForIncomingOffers() {
        currentUserId?.let { myId ->
            val userCallsRef = callsRef.child(myId)
            val offerListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val callData = snapshot.value as? Map<*, *> ?: return
                    val type = callData["type"] as? String
                    val status = callData["status"] as? String
                    val callId = callData["callId"] as? String ?: snapshot.key

                    if (type == "OFFER" && status == "pending" && callId != null) {
                        val sdpString = callData["sdp"] as? String
                        val callerId = callData["callerId"] as? String
                        if (sdpString != null && callerId != null) {
                            Log.d(TAG, "Incoming call offer $callId from $callerId")
                            val offerSdp = SessionDescription(SessionDescription.Type.OFFER, sdpString)
                            onNewCallOfferListener?.invoke(offerSdp, callerId, callId)
                            listenForCallUpdates(myId, callId, callerId)
                        }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val callData = snapshot.value as? Map<*, *> ?: return
                    val status = callData["status"] as? String
                    val callId = callData["callId"] as? String ?: snapshot.key
                    if (callId != null && (status == "ended" || status == "cancelled")) {
                        Log.d(TAG, "Incoming call $callId was $status by caller.")
                        onCallEndedListener?.invoke(callId)
                        cleanupCallListeners(callId)
                        if (activeCallId == callId) activeCallId = null
                    }
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val callId = snapshot.child("callId").getValue(String::class.java) ?: snapshot.key
                    callId?.let {
                        Log.d(TAG, "Incoming call offer $it removed (likely ended/cancelled by caller).")
                        onCallEndedListener?.invoke(it)
                        cleanupCallListeners(it)
                        if (activeCallId == it) activeCallId = null
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Listener for incoming offers on $myId cancelled", error.toException())
                }
            }
            userCallsRef.addChildEventListener(offerListener)
            addManagedListener(myId, "offers_listener", userCallsRef, offerListener)
            Log.d(TAG, "Listening for incoming offers on /calls/$myId")
        }
    }

    /**
     * Listens for updates on a specific call node: answer, rejection, ICE candidates, status changes.
     * This is typically called by the user who is expecting these updates for a call.
     * Path: /calls/{myUserId}/{callId}/
     */
    fun listenForCallUpdates(myUserId: String, callId: String, peerId: String) {
        val myCallNodeRef = callsRef.child(myUserId).child(callId)
        this.activeCallId = callId

        // Listener for Answer or Rejection (direct children of callId node)
        val callStateListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                when (snapshot.key) {
                    "answer" -> {
                        val sdpString = snapshot.child("sdp").getValue(String::class.java)
                        sdpString?.let {
                            val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, it)
                            Log.d(TAG, "Received ANSWER for call $callId")
                            onCallAnswerListener?.invoke(answerSdp, callId)
                        }
                    }
                    "rejection" -> {
                        val rejecterId = snapshot.child("rejecterId").getValue(String::class.java) ?: peerId
                        Log.d(TAG, "Received REJECTION for call $callId from $rejecterId")
                        onCallRejectedListener?.invoke(callId, rejecterId)
                        cleanupCallListeners(callId)
                        if (activeCallId == callId) activeCallId = null
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Call state listener for $callId cancelled", error.toException())
            }
        }
        myCallNodeRef.addChildEventListener(callStateListener)
        addManagedListener(myUserId, callId + "_state", myCallNodeRef, callStateListener)

        // Listener for ICE Candidates from the peer
        val iceCandidatesRef = myCallNodeRef.child("iceCandidates")
        val iceListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val iceData = snapshot.value as? Map<*, *> ?: return
                val sender = iceData["senderId"] as? String
                if (sender == peerId) {
                    val sdpMid = iceData["sdpMid"] as? String
                    val sdpMLineIndex = (iceData["sdpMLineIndex"] as? Long)?.toInt()
                    val candidateSdp = iceData["candidate"] as? String
                    if (sdpMid != null && sdpMLineIndex != null && candidateSdp != null) {
                        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidateSdp)
                        Log.d(TAG, "Received ICE candidate for call $callId from $peerId")
                        onIceCandidateListener?.invoke(iceCandidate, callId)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "ICE listener for $callId (peer $peerId) cancelled", error.toException())
            }
        }
        iceCandidatesRef.addChildEventListener(iceListener)
        addManagedListener(myUserId, callId + "_ice", iceCandidatesRef, iceListener)
        Log.d(TAG, "Listening for updates on call $callId at ${myCallNodeRef.toString()}")
    }

    /**
     * Ends the call by updating status on the original offer node.
     * The original offer is at /calls/{calleeId}/{callId}
     */
    fun endCall(originalCalleeId: String, callId: String) {
        Log.d(TAG, "Attempting to end call $callId (original callee: $originalCalleeId)")
        val offerPath = "$originalCalleeId/$callId/status"
        callsRef.child(offerPath).setValue("ended").addOnCompleteListener {
            if (it.isSuccessful) Log.d(TAG, "Call $callId status set to ended on offer node.")
            else Log.e(TAG, "Failed to set call $callId status to ended on offer node.")
        }

        currentUserId?.let {
            callsRef.child(it).child(callId).child("status").setValue("ended")
        }

        onCallEndedListener?.invoke(callId)
        cleanupCallListeners(callId)
        if (activeCallId == callId) activeCallId = null
    }

    private fun addManagedListener(ownerId: String, listenerTag: String, ref: DatabaseReference, listener: Any) {
        val key = "${ownerId}_${listenerTag}"
        val listenersForOwner = activeListeners.getOrPut(key) { mutableListOf() }
        listenersForOwner.add(Pair(ref, listener))
    }

    private fun cleanupCallListeners(id: String) {
        Log.d(TAG, "Cleaning up listeners for ID: $id")
        val keysToRemove = activeListeners.keys.filter { it.contains(id) }
        keysToRemove.forEach { key ->
            activeListeners.remove(key)?.forEach { pair ->
                val ref = pair.first
                when (val listener = pair.second) {
                    is ValueEventListener -> ref.removeEventListener(listener)
                    is ChildEventListener -> ref.removeEventListener(listener)
                }
                Log.d(TAG, "Removed listener on ${ref.toString()} for key $key")
            }
        }
    }

    fun globalCleanup() {
        Log.d(TAG, "Performing global cleanup of CallSignalingManager")
        activeListeners.keys.toList().forEach { key ->
            activeListeners.remove(key)?.forEach { pair ->
                val ref = pair.first
                when (val listener = pair.second) {
                    is ValueEventListener -> ref.removeEventListener(listener)
                    is ChildEventListener -> ref.removeEventListener(listener)
                }
                Log.d(TAG, "Globally removed listener on ${ref.toString()} for key $key")
            }
        }
        activeListeners.clear()
        activeCallId = null
        INSTANCE = null
        Log.d(TAG, "Global cleanup complete.")
    }
}