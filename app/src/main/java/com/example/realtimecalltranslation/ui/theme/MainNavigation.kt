package com.example.realtimecalltranslation.ui.theme

// Imports from original user file (potentially with some cleanup if unused after changes)
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog as AndroidCallLog // Keep this alias
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.* // This is not in user's original, but good practice
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
// Removed AgoraManager and CallScreenViewModel imports as they are no longer params
// import com.example.realtimecalltranslation.agora.AgoraManager
// import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.example.realtimecalltranslation.ui.CallScreen // Keep this for invocation
import com.example.realtimecalltranslation.ui.ProfileScreen // Keep this
// Assuming CallHistoryScreen, FavouritesScreen, DialerScreen, ContactsScreen are in this package or ui.theme
// Assuming User, CallLog, CallType, Message are in this package or ui.theme
// Assuming color constants like mainRed, mainWhite etc. are from ui.theme.* import
import com.example.realtimecalltranslation.ui.theme.* // Wildcard for colors etc.
import com.example.realtimecalltranslation.ui.getRealCallLogs // Corrected import from ui package


@Composable
fun MainNavigation(
    appId: String, // Re-added
    token: String?,
    localIsUsa: Boolean
    // callScreenViewModel: CallScreenViewModel, // Removed
    // agoraManager: AgoraManager,             // Removed
) {
    var currentScreen by remember { mutableStateOf("history") }
    var callTo by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(listOf<Message>()) } // Keep Message type
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedNav by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    val demoUsers = listOf(
        User("1", "Demo User", "017XXXXXXXX", null),
        User("2", "Has Pic", "018XXXXXXXX", "https://randomuser.me/api/portraits/men/1.jpg"),
        User("3", "No Pic", "019XXXXXXXX", null)
    )
    val demoCallLogs = listOf(
        CallLog(demoUsers[0], "Can you translate this", CallType.INCOMING, false, "5 min ago"),
        CallLog(demoUsers[1], "Missed call", CallType.MISSED, true, "10 min ago"),
        CallLog(demoUsers[2], "Outgoing call", CallType.OUTGOING, false, "20 min ago")
    )

    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED

    val realCallLogs = remember(hasPermission) {
        if (hasPermission) getRealCallLogs(context) else emptyList<CallLog>()
    }

    val activeCallLogs = if (realCallLogs.isNotEmpty()) realCallLogs else demoCallLogs
    // val activeUsers = activeCallLogs.map { it.user }.distinctBy { it.id } // This was in previous, but not in user's original. Removing for now to match.

    when (currentScreen) {
        "history" -> {
            CallHistoryScreen(
                callLogs = activeCallLogs,
                onProfile = { user ->
                    selectedUser = user
                    currentScreen = "profile"
                },
                onCall = { user ->
                    callTo = user.phone
                    messages = listOf( // Original demo messages for onCall
                        Message(fromUsa = true, original = "How are you?", translated = "কেমন আছো?"),
                        Message(fromUsa = false, original = "Ami bhalo achi.", translated = "I am fine.")
                    )
                    currentScreen = "call"
                },
                onAddNew = { // Ensured onAddNew is present
                    selectedNav = 1
                    currentScreen = "dialer"
                },
                onUserAvatar = { user -> // From user's original
                    selectedUser = user
                    currentScreen = "profile"
                },
                onFavourites = {
                    selectedNav = 0
                    currentScreen = "favourites"
                },
                onDialer = { // From user's original
                    selectedNav = 1
                    currentScreen = "dialer"
                },
                onContacts = { // From user's original
                    selectedNav = 2
                    currentScreen = "contacts"
                },
                selectedNav = selectedNav,
                mainRed = mainRed,
                mainWhite = mainWhite,
                accentRed = accentRed,
                lightRed = lightRed
            )
        }
        "dialer" -> DialerScreen(
            onClose = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            onNavigateToCall = { number -> // This was updated in a previous step and is correct
                callTo = number
                messages = listOf(
                    Message(fromUsa = true, original = "Dialing...", translated = "ডায়াল হচ্ছে...")
                )
                currentScreen = "call"
            }
        )
        "favourites" -> FavouritesScreen( // Assuming it takes these based on user's context for other screens
            onBack = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            accentRed = accentRed,
            lightRed = lightRed
        )
        "contacts" -> ContactsScreen( // Assuming it takes these
            onBack = {
                selectedNav = 0
                currentScreen = "history"
            },
            onCallContact = { phoneNumber -> // This was updated in a previous step
                callTo = phoneNumber
                messages = listOf(
                    Message(fromUsa = true, original = "Calling contact...", translated = "কন্টাক্টকে কল করা হচ্ছে...")
                )
                currentScreen = "call"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            accentRed = accentRed,
            lightRed = lightRed,
            mainGreen = mainGreen,
            lightGreen = lightGreen
        )
        "call" -> CallScreen(
            // callScreenViewModel and agoraManager removed
            appId = appId, // Re-added
            channel = callTo ?: "",
            token = token,
            localIsUsa = localIsUsa,
            messages = messages, // Uses state from MainNavigation
            onCallEnd = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite
        )
        "profile" -> {
            selectedUser?.let { user ->
                ProfileScreen(
                    user = user,
                    callLogs = activeCallLogs.filter { it.user.id == user.id },
                    imageDataSource = selectedUser?.profilePicUrl,
                    onNameUpdate = { newName ->
                        selectedUser = selectedUser?.copy(name = newName)
                    },
                    onProfilePicUriSelected = { uriString ->
                        selectedUser = selectedUser?.copy(profilePicUrl = uriString)
                    },
                    onBack = {
                        selectedNav = 0
                        currentScreen = "history"
                    },
                    onCall = { userToCall ->
                        callTo = userToCall.phone
                        messages = listOf( // Original demo messages from user's ProfileScreen onCall
                            Message(fromUsa = true, original = "How are you?", translated = "কেমন আছো?"),
                            Message(fromUsa = false, original = "Ami bhalo achi.", translated = "I am fine.")
                        )
                        currentScreen = "call"
                    },
                    mainRed = mainRed,
                    mainWhite = mainWhite
                )
            }
        }
    }
}

// getRealCallLogs function is defined here in user's original MainNavigation.kt
fun getRealCallLogs(context: Context): List<CallLog> {
    val logs = mutableListOf<CallLog>()
    val resolver = context.contentResolver
    val cursor = resolver.query(
        AndroidCallLog.Calls.CONTENT_URI,
        null, null, null, AndroidCallLog.Calls.DATE + " DESC"
    )
    cursor?.use {
        val numberIdx = it.getColumnIndex(AndroidCallLog.Calls.NUMBER)
        val typeIdx = it.getColumnIndex(AndroidCallLog.Calls.TYPE)
        val nameIdx = it.getColumnIndex(AndroidCallLog.Calls.CACHED_NAME)
        val dateIdx = it.getColumnIndex(AndroidCallLog.Calls.DATE)
        while (it.moveToNext()) {
            val number = it.getString(numberIdx)
            val name = it.getString(nameIdx) ?: number // Use number if name is null
            val type = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                else -> CallType.MISSED // Default for unknown types
            }
            // Simple time formatting, can be improved
            val time = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", it.getLong(dateIdx)).toString()
            val user = User(id = number, name = name, phone = number) // Using number as ID for simplicity
            logs.add(
                CallLog(
                    user = user,
                    message = if (type == CallType.MISSED) "Missed" else type.name, // Example message
                    callType = type,
                    isMissed = (type == CallType.MISSED),
                    time = time
                )
            )
        }
    }
    return logs
}
