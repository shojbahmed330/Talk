package com.example.realtimecalltranslation.util

import java.util.UUID

object ChannelUtils {

    /**
     * Generates a unique and consistent channel name from two user IDs.
     * It sorts the user IDs alphabetically before joining them to ensure
     * that generateUniqueChannelName(userA, userB) and
     * generateUniqueChannelName(userB, userA) produce the same channel name.
     *
     * A more robust solution for very large scale systems might involve a
     * backend service generating truly unique call session IDs, but this
     * approach is suitable for many scenarios.
     *
     * @param userId1 The ID of the first user (e.g., phone number or Firebase UID).
     * @param userId2 The ID of the second user.
     * @return A unique string to be used as an Agora channel name.
     */
    fun generateUniqueChannelName(userId1: String, userId2: String): String {
        // Ensure IDs are not blank
        if (userId1.isBlank() || userId2.isBlank()) {
            // Fallback to a random UUID if one ID is blank, though this scenario should ideally be prevented
            return "channel_${UUID.randomUUID()}"
        }

        // Sanitize IDs: remove non-alphanumeric characters to ensure valid channel names
        // Agora channel names should typically be composed of a limited set of characters
        // (letters, numbers, and some symbols like ':', '_', '-').
        // For simplicity, we'll replace common problematic characters.
        // A more restrictive regex might be needed depending on Agora's exact rules.
        val sanitizedId1 = userId1.replace(Regex("[^a-zA-Z0-9_-]"), "")
        val sanitizedId2 = userId2.replace(Regex("[^a-zA-Z0-9_-]"), "")

        // Prevent same user ID from forming a predictable channel, though this is unlikely for a call
        if (sanitizedId1 == sanitizedId2) {
            return "self_channel_${sanitizedId1}_${UUID.randomUUID().toString().substring(0, 8)}"
        }

        // Sort IDs to ensure consistency (userA_userB is same as userB_userA)
        return if (sanitizedId1 < sanitizedId2) {
            "call_${sanitizedId1}_${sanitizedId2}"
        } else {
            "call_${sanitizedId2}_${sanitizedId1}"
        }
    }
}
