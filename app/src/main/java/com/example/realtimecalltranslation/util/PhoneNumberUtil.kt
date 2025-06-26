package com.example.realtimecalltranslation.util

import android.util.Log

object PhoneNumberUtil {

    private const val TAG = "PhoneNumberUtil"
    private const val BANGLADESHI_COUNTRY_CODE = "+880"
    private const val BANGLADESHI_NUMBER_LENGTH_WITHOUT_COUNTRY_CODE = 10 // e.g., 1xxxxxxxxx after removing leading 0
    private const val BANGLADESHI_NUMBER_LENGTH_WITH_COUNTRY_CODE_NO_PLUS = 13 // e.g., 8801xxxxxxxxx
    private const val BANGLADESHI_NUMBER_LENGTH_WITH_COUNTRY_CODE_AND_PLUS = 14 // e.g., +8801xxxxxxxxx
    private const val BANGLADESHI_LOCAL_PREFIX = "0"

    /**
     * Normalizes a given phone number to the E.164 format for Bangladesh (+880...).
     *
     * - If the number already starts with "+880", it's considered normalized.
     * - If the number starts with "0" and has 11 digits (e.g., "01712345678"),
     *   the leading "0" is replaced with "+880".
     * - If the number starts with "880" and has 13 digits (e.g., "8801712345678"),
     *   it's prefixed with "+".
     * - Other formats might not be correctly handled and will be returned as is.
     *
     * @param phoneNumber The phone number to normalize.
     * @return The normalized phone number, or the original number if it doesn't match known patterns.
     */
    fun normalizePhoneNumber(phoneNumber: String): String {
        val trimmedNumber = phoneNumber.filter { !it.isWhitespace() } // Remove any spaces

        Log.d(TAG, "Attempting to normalize phone number: '$trimmedNumber'")

        if (trimmedNumber.startsWith(BANGLADESHI_COUNTRY_CODE)) {
            if (trimmedNumber.length == BANGLADESHI_NUMBER_LENGTH_WITH_COUNTRY_CODE_AND_PLUS) {
                Log.d(TAG, "Number '$trimmedNumber' already starts with $BANGLADESHI_COUNTRY_CODE and has correct length. No normalization needed.")
                return trimmedNumber
            } else {
                Log.w(TAG, "Number '$trimmedNumber' starts with $BANGLADESHI_COUNTRY_CODE but has an unexpected length. Returning as is.")
                return trimmedNumber // Or handle error appropriately
            }
        }

        if (trimmedNumber.startsWith(BANGLADESHI_LOCAL_PREFIX) && trimmedNumber.length == (BANGLADESHI_NUMBER_LENGTH_WITHOUT_COUNTRY_CODE + BANGLADESHI_LOCAL_PREFIX.length)) {
            val nationalSignificantNumber = trimmedNumber.substring(BANGLADESHI_LOCAL_PREFIX.length)
            val normalized = BANGLADESHI_COUNTRY_CODE + nationalSignificantNumber
            Log.d(TAG, "Number '$trimmedNumber' starts with $BANGLADESHI_LOCAL_PREFIX. Normalized to '$normalized'.")
            return normalized
        }

        if (trimmedNumber.startsWith("880") && trimmedNumber.length == BANGLADESHI_NUMBER_LENGTH_WITH_COUNTRY_CODE_NO_PLUS) {
            val nationalSignificantNumber = trimmedNumber.substring("880".length)
            val normalized = BANGLADESHI_COUNTRY_CODE + nationalSignificantNumber
            Log.d(TAG, "Number '$trimmedNumber' starts with 880. Normalized to '$normalized'.")
            return normalized
        }

        Log.d(TAG, "Number '$trimmedNumber' does not match known patterns for normalization. Returning as is.")
        return trimmedNumber // Default: return original if no specific pattern matches
    }
}
