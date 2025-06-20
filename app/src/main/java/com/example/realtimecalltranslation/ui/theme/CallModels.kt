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
import android.text.format.DateUtils // Added import
// android.net.Uri will be needed for the new getRealCallLogs if it uses Uri.withAppendedPath, but it's not in the provided snippet.
// The provided snippet for getRealCallLogs doesn't use Uri directly, so let's stick to what's in the provided snippet.

enum class CallType {
    INCOMING,
    MISSED,
    OUTGOING
}

data class CallLog(
    val user: User,
    val message: String,
    val callType: CallType,
    val isMissed: Boolean,
    val formattedDateTime: String, // Renamed from 'time'
    val timestamp: Long,           // New field for raw timestamp
    val duration: Long?            // New field for call duration in seconds
)

fun getRealCallLogs(context: Context): List<CallLog> {
    val logs = mutableListOf<CallLog>()

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
        Log.w("getRealCallLogs", "READ_CALL_LOG permission not granted. Returning empty list.")
        return logs
    }

    val projection = arrayOf(
        AndroidCallLog.Calls.NUMBER,
        AndroidCallLog.Calls.TYPE,
        AndroidCallLog.Calls.CACHED_NAME,
        AndroidCallLog.Calls.DATE,
        AndroidCallLog.Calls.DURATION
    )

    // Query all calls, sorted by date descending. No explicit limit here,
    // ProfileScreen will handle showing "at least 50" for a specific user.
    val cursor = context.contentResolver.query(
        AndroidCallLog.Calls.CONTENT_URI,
        projection,
        null,
        null,
        AndroidCallLog.Calls.DATE + " DESC"
    )

    cursor?.use {
        val numberIdx = it.getColumnIndex(AndroidCallLog.Calls.NUMBER)
        val typeIdx = it.getColumnIndex(AndroidCallLog.Calls.TYPE)
        val nameIdx = it.getColumnIndex(AndroidCallLog.Calls.CACHED_NAME)
        val dateIdx = it.getColumnIndex(AndroidCallLog.Calls.DATE)
        val durationIdx = it.getColumnIndex(AndroidCallLog.Calls.DURATION)

        while (it.moveToNext()) {
            val number = it.getString(numberIdx) ?: "Unknown Number"
            var name = it.getString(nameIdx) // Can be null
            if (name.isNullOrBlank()) {
                name = number // Use number if name is not available
            }

            val callLogType = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                AndroidCallLog.Calls.VOICEMAIL_TYPE -> CallType.MISSED // Treat voicemail as missed for simplicity
                AndroidCallLog.Calls.REJECTED_TYPE -> CallType.MISSED  // Treat rejected as missed
                AndroidCallLog.Calls.BLOCKED_TYPE -> CallType.MISSED   // Treat blocked as missed
                else -> CallType.MISSED
            }

            val dateTimestamp = it.getLong(dateIdx)
            val callDurationSeconds = it.getLong(durationIdx) // Duration in seconds

            val formattedDateTime = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", dateTimestamp).toString()

            val user = User(id = number, name = name ?: number, phone = number, profilePicUrl = null)

            // Construct a more informative message based on call type
            val message = when (callLogType) {
                CallType.INCOMING -> "Incoming call from $name"
                CallType.OUTGOING -> "Outgoing call to $name"
                CallType.MISSED -> if (it.getInt(typeIdx) == AndroidCallLog.Calls.REJECTED_TYPE) "Rejected call from $name" else "Missed call from $name"
                // else -> "Call with $name" // Fallback - This line is commented out as it makes the when exhaustive with current CallTypes
            }

            logs.add(
                CallLog(
                    user = user,
                    message = message,
                    callType = callLogType,
                    isMissed = (callLogType == CallType.MISSED), // isMissed can be more specific if needed
                    formattedDateTime = formattedDateTime,
                    timestamp = dateTimestamp,
                    duration = callDurationSeconds
                )
            )
        }
    } ?: run {
        Log.e("getRealCallLogs", "Cursor was null, cannot read call logs.")
    }
    return logs
}

// Add this import at the top of CallModels.kt if not already present
// import android.text.format.DateUtils
// Context might be needed for some DateUtils formatting options, but getRelativeTimeSpanString often doesn't.
// Let's try without Context first for a simpler signature.

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    // MIN_RESOLUTION is in milliseconds, so DateUtils.MINUTE_IN_MILLIS
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS, // Minimum resolution to display (e.g., "1 min ago")
        DateUtils.FORMAT_ABBREV_RELATIVE // Options like FORMAT_ABBREV_ALL, etc.
    )
    return relativeTime.toString()
}
