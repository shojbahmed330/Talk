package com.example.realtimecalltranslation.ui.theme

// Assuming User is already defined in com.example.realtimecalltranslation.ui.theme.User
// and CallType is defined above in this file.
// If User is not found by the subtask worker, it might indicate an issue with previous steps or file visibility for the worker.
// For now, proceed with the assumption User is accessible via its canonical path.

import com.example.realtimecalltranslation.ui.theme.User // Explicit import for clarity
import android.content.Context
import android.provider.CallLog as AndroidCallLog // Alias to avoid conflict if any
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import android.text.format.DateUtils
import android.provider.ContactsContract
import android.net.Uri

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

    val hasReadCallLogPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    val hasReadContactsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    if (!hasReadCallLogPermission) {
        Log.w("getRealCallLogs", "READ_CALL_LOG permission not granted. Returning empty list.")
        return logs
    }

    val projection = arrayOf(
        AndroidCallLog.Calls.NUMBER,
        AndroidCallLog.Calls.TYPE,
        AndroidCallLog.Calls.CACHED_NAME, // Name as cached by the system dialer
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
        val cachedNameIdx = it.getColumnIndex(AndroidCallLog.Calls.CACHED_NAME) // Use this for the cached name
        val dateIdx = it.getColumnIndex(AndroidCallLog.Calls.DATE)
        val durationIdx = it.getColumnIndex(AndroidCallLog.Calls.DURATION)

        while (it.moveToNext()) {
            val number = it.getString(numberIdx) ?: "Unknown Number"
            var contactName = it.getString(cachedNameIdx) // Get cached name from call log
            var photoUri: String? = null

            if (hasReadContactsPermission) {
                val contactDetails = getContactDetailsByNumber(context, number)
                if (contactDetails != null) {
                    if (!contactDetails.name.isNullOrBlank()) { // Prefer contact name if available
                        contactName = contactDetails.name
                    }
                    photoUri = contactDetails.photoUri
                }
            }

            // Fallback to number if contactName is still blank
            if (contactName.isNullOrBlank()) {
                contactName = number
            }

            val callLogType = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                AndroidCallLog.Calls.VOICEMAIL_TYPE -> CallType.MISSED
                AndroidCallLog.Calls.REJECTED_TYPE -> CallType.MISSED
                AndroidCallLog.Calls.BLOCKED_TYPE -> CallType.MISSED
                else -> CallType.MISSED
            }

            val dateTimestamp = it.getLong(dateIdx)
            val callDurationSeconds = it.getLong(durationIdx)

            val formattedDateTime = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", dateTimestamp).toString()

            val user = User(
                id = number, // Using phone number as ID for simplicity here
                name = contactName ?: number, // Use fetched contact name or fallback
                phone = number,
                profilePicUrl = photoUri
            )

            val simpleMessage = when (callLogType) {
                CallType.INCOMING -> "Incoming Call"
                CallType.OUTGOING -> "Outgoing Call"
                CallType.MISSED -> "Missed Call" // This also covers REJECTED, BLOCKED etc. as they map to CallType.MISSED
            }

            logs.add(
                CallLog(
                    user = user,
                    message = simpleMessage, // Use the simplified message
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

data class ContactDetails(val name: String?, val photoUri: String?)

fun getContactDetailsByNumber(context: Context, phoneNumber: String): ContactDetails? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        Log.w("getContactDetails", "READ_CONTACTS permission not granted. Cannot fetch contact details.")
        return null
    }

    if (phoneNumber.isBlank()) {
        return null
    }

    var contactName: String? = null
    var photoUriString: String? = null

    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI // URI for the contact's full-size photo
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoUriIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)

                if (nameIdx != -1) {
                    contactName = cursor.getString(nameIdx)
                }
                if (photoUriIdx != -1) {
                    photoUriString = cursor.getString(photoUriIdx)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("getContactDetails", "Error fetching contact details for $phoneNumber: ${e.message}", e)
        // Return null or default ContactDetails if an error occurs
        return null
    }

    // Only return details if at least one piece of information was found, or always return if you want to allow partials
    return if (contactName != null || photoUriString != null) {
        ContactDetails(contactName, photoUriString)
    } else {
        null // No details found
    }
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
