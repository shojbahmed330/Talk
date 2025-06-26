package com.example.realtimecalltranslation.firebase

import com.example.realtimecalltranslation.util.Constants

/**
 * Represents a call request or an active call session.
 * This data will be stored in Firebase Realtime Database under /call_requests/{calleeId}/{callId}
 *
 * @property callId A unique identifier for this specific call request/session.
 * @property callerId The unique ID (e.g., phone number) of the user initiating the call.
 * @property callerName Optional: The display name of the caller.
 * @property calleeId The unique ID (e.g., phone number) of the user receiving the call.
 * @property channelName The unique Agora channel name generated for this call.
 * @property status The current status of the call (e.g., "pending", "accepted", "rejected", "ongoing", "ended").
 *                  Uses constants from [Constants].
 * @property timestamp The server timestamp when the call request was created or last updated.
 * @property type Type of call, e.g., "audio", "video". For now, defaults to "audio". (Future enhancement)
 */
data class CallRequest(
    var callId: String = "", // Should be generated and set before saving
    var callerId: String = "",
    var callerName: String? = null,
    var calleeId: String = "",
    var channelName: String = "",
    var status: String = Constants.CALL_STATUS_PENDING,
    var timestamp: Any? = null, // Updated from 0L to null for ServerValue.TIMESTAMP
    var type: String = "audio" // Default to audio call
) {
    // No-argument constructor required by Firebase for deserialization
    constructor() : this("", "", null, "", "", Constants.CALL_STATUS_PENDING, null, "audio")
}