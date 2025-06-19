package com.example.realtimecalltranslation.ui.theme

// Assuming User is already defined in com.example.realtimecalltranslation.ui.theme.User
// and CallType is defined above in this file.
// If User is not found by the subtask worker, it might indicate an issue with previous steps or file visibility for the worker.
// For now, proceed with the assumption User is accessible via its canonical path.

import com.example.realtimecalltranslation.ui.theme.User // Explicit import for clarity

enum class CallType {
    INCOMING,
    MISSED,
    OUTGOING
}

data class CallLog(
    val user: User,
    val message: String, // Changed from 'title' to 'message' for consistency
    val callType: CallType,
    val isMissed: Boolean,
    val time: String
)
