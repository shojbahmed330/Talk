package com.example.realtimecalltranslation.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat

object CallLogUtils { // Changed to an object for utility functions

    fun addCallToDeviceLog(
        context: Context,
        number: String?,
        type: Int,
        startTime: Long,
        durationSeconds: Long,
        cachedName: String? // Added parameter
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CallLogUtils", "WRITE_CALL_LOG permission not granted. Cannot add call to log.")
            // Optionally, you could throw an exception or notify the user/system here.
            // For now, just logging an error.
            return
        }

        if (number.isNullOrBlank()) {
            Log.e("CallLogUtils", "Phone number is null or blank. Cannot add call to log.")
            return
        }

        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, number)
            put(CallLog.Calls.TYPE, type)
            put(CallLog.Calls.DATE, startTime)
            put(CallLog.Calls.DURATION, durationSeconds)
            if (!cachedName.isNullOrBlank()) { // Add this condition and put
                put(CallLog.Calls.CACHED_NAME, cachedName)
            }
            // put(CallLog.Calls.NEW, 1) // Optional: consider adding this
            // put(CallLog.Calls.IS_READ, 0) // Optional: consider adding this
        }

        try {
            val uri = context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            if (uri != null) {
                Log.d("CallLogUtils", "Call added to device log: $number, URI: $uri")
            } else {
                Log.e("CallLogUtils", "Failed to add call to device log for number: $number. URI is null.")
            }
        } catch (e: SecurityException) {
            Log.e("CallLogUtils", "SecurityException while adding call to log for $number. Check WRITE_CALL_LOG permission.", e)
        } catch (e: Exception) {
            Log.e("CallLogUtils", "Exception while adding call to log for $number.", e)
        }
    }
}
