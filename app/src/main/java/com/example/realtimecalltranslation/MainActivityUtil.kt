package com.example.realtimecalltranslation

import com.example.realtimecalltranslation.ui.theme.User

// Helper function to be used by MainActivity
// This is defined in a separate file to avoid modifying MainActivity directly for this step initially,
// but ideally, it would be a private member or in a companion object of MainActivity.
// For the purpose of this tool, creating a new file is simpler.

fun findUserInList(users: List<User>, idToMatch: String, phoneToMatch: String): User? {
    return users.find { user -> user.id == idToMatch || user.phone == phoneToMatch }
}

// We might also need a helper for the profile picture URL encoding,
// but the primary error is with .find and property access.
// Let's focus on resolving that first.
