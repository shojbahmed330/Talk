package com.example.realtimecalltranslation.ui.theme

// Assuming User is already defined in com.example.realtimecalltranslation.ui.theme.User
// and CallType is defined above in this file.
// If User is not found by the subtask worker, it might indicate an issue with previous steps or file visibility for the worker.
// For now, proceed with the assumption User is accessible via its canonical path.

import com.example.realtimecalltranslation.ui.theme.User // Explicit import for clarity
import android.content.Context
import android.provider.CallLog as AndroidCallLog // Alias to avoid conflict if any
import android.Manifest // For permission check, good practice though not strictly part of the move
import android.content.pm.PackageManager // For permission check
import androidx.core.content.ContextCompat // For permission check
import android.util.Log // For logging errors

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

// getRealCallLogs function to be added:
fun getRealCallLogs(context: Context): List<CallLog> {
    val logs = mutableListOf<CallLog>()

    // Permission check - good practice to include
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        Log.w("getRealCallLogs", "READ_CALL_LOG permission not granted. Returning empty list.")
        return logs // Return empty if no permission
    }

    val resolver = context.contentResolver
    val cursor = resolver.query(
        AndroidCallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        AndroidCallLog.Calls.DATE + " DESC"
    )

    cursor?.use {
        val numberIdx = it.getColumnIndex(AndroidCallLog.Calls.NUMBER)
        val typeIdx = it.getColumnIndex(AndroidCallLog.Calls.TYPE)
        val nameIdx = it.getColumnIndex(AndroidCallLog.Calls.CACHED_NAME)
        val dateIdx = it.getColumnIndex(AndroidCallLog.Calls.DATE)

        while (it.moveToNext()) {
            val number = it.getString(numberIdx) ?: "Unknown Number"
            val name = it.getString(nameIdx) ?: number
            val callLogType = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                else -> CallType.MISSED
            }
            val time = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", it.getLong(dateIdx)).toString()

            val user = User(id = number, name = name, phone = number, profilePicUrl = null) // profilePicUrl is null

            logs.add(
                CallLog(
                    user = user,
                    message = if (callLogType == CallType.MISSED) "Missed Call from $name" else "Call with $name", // More descriptive message
                    callType = callLogType,
                    isMissed = (callLogType == CallType.MISSED),
                    time = time
                )
            )
        }
    } ?: run {
        Log.e("getRealCallLogs", "Cursor was null, cannot read call logs.")
    }
    return logs
}
