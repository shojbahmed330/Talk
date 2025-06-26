package com.example.realtimecalltranslation.ui.theme

import android.content.Context
import android.provider.CallLog as AndroidCallLog
import android.provider.ContactsContract
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import android.util.Log
import android.text.format.DateUtils // Already present, ensure it's used

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
    val formattedDateTime: String,
    val timestamp: Long,
    val duration: Long?
)

// Commented out User data class definition removed as it's defined in User.kt

private fun getContactNameByPhoneNumber(context: Context, phoneNumber: String): String? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        Log.w("CallModels", "READ_CONTACTS permission not granted. Cannot fetch contact name.")
        return null
    }
    if (phoneNumber.isBlank()) {
        return null
    }

    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
    val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
    var contactName: String? = null

    try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (nameIndex != -1) {
                    contactName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("CallModels", "Error querying contact name for $phoneNumber: ${e.message}")
    }
    return contactName
}

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
            var displayName: String? = it.getString(nameIdx) // Cached name from call log

            // Try to get a more accurate name from contacts if cached name is blank, null, or just the number itself
            if (displayName.isNullOrBlank() || displayName == number) {
                val contactName = getContactNameByPhoneNumber(context, number)
                if (!contactName.isNullOrBlank()) {
                    displayName = contactName
                }
            }

            // Fallback to number if no name could be found
            if (displayName.isNullOrBlank()) {
                displayName = number
            }


            val callLogType = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                AndroidCallLog.Calls.VOICEMAIL_TYPE -> CallType.MISSED // Consider how to handle voicemail
                AndroidCallLog.Calls.REJECTED_TYPE -> CallType.MISSED // Consider how to handle rejected
                AndroidCallLog.Calls.BLOCKED_TYPE -> CallType.MISSED  // Consider how to handle blocked
                else -> CallType.MISSED // Default for unknown types
            }

            val dateTimestamp = it.getLong(dateIdx)
            val callDurationSeconds = it.getLong(durationIdx)

            val formattedDateTime = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", dateTimestamp).toString()

            // In this restored version, profilePicUrl is always null.
            val user = User(id = number, name = displayName ?: number, phone = number, profilePicUrl = null) // Use displayName

            val simpleMessage = when (callLogType) {
                CallType.INCOMING -> "Incoming Call"
                CallType.OUTGOING -> "Outgoing Call"
                CallType.MISSED -> "Missed Call"
            }

            logs.add(
                CallLog(
                    user = user,
                    message = simpleMessage,
                    callType = callLogType,
                    isMissed = (callLogType == CallType.MISSED),
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

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    )
    return relativeTime.toString()
}
