package com.example.realtimecalltranslation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog as AndroidCallLog
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallHistoryScreen
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.FavouritesScreen
import com.example.realtimecalltranslation.ui.theme.DialerScreen
import com.example.realtimecalltranslation.ui.theme.ContactsScreen
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.theme.* // <-- Import all color constants from Color.kt

@Composable
fun MainNavigation(
    appId: String,
    token: String?,
    localIsUsa: Boolean
) {
    var currentScreen by remember { mutableStateOf("history") }
    var callTo by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedNav by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    // Demo user & call log, fallback only
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

    // Permission check
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED

    // Real call log fetcher (memoized)
    val realCallLogs = remember(hasPermission) {
        if (hasPermission) getRealCallLogs(context) else emptyList()
    }

    val activeCallLogs = if (realCallLogs.isNotEmpty()) realCallLogs else demoCallLogs
    val activeUsers = activeCallLogs.map { it.user }.distinctBy { it.id }

    // --- Navigation Logic ---
    when (currentScreen) {
        "history" -> {
            CallHistoryScreen(
                // userList removed, only pass callLogs
                callLogs = activeCallLogs,
                onProfile = { user ->
                    selectedUser = user
                    currentScreen = "profile"
                },
                onCall = { user ->
                    callTo = user.phone
                    messages = listOf(
                        Message(fromUsa = true, original = "How are you?", translated = "কেমন আছো?"),
                        Message(fromUsa = false, original = "Ami bhalo achi.", translated = "I am fine.")
                    )
                    currentScreen = "call"
                },
                onUserAvatar = { user ->
                    selectedUser = user
                    currentScreen = "profile"
                },
                onFavourites = {
                    selectedNav = 0
                    currentScreen = "favourites"
                },
                onDialer = {
                    selectedNav = 1
                    currentScreen = "dialer"
                },
                onContacts = {
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
            mainWhite = mainWhite
        )
        "favourites" -> FavouritesScreen(
            onBack = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            accentRed = accentRed,
            lightRed = lightRed
        )
        "contacts" -> ContactsScreen(
            onBack = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            accentRed = accentRed,
            lightRed = lightRed,
            mainGreen = mainGreen,
            lightGreen = lightGreen
        )
        "call" -> CallScreen(
            channel = callTo ?: "",
            token = token,
            appId = appId,
            localIsUsa = localIsUsa,
            messages = messages,
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
                    onBack = {
                        selectedNav = 0
                        currentScreen = "history"
                    },
                    onCall = { user2 ->
                        callTo = user2.phone
                        messages = listOf(
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

// --- Real Call Log fetcher ---
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
            val name = it.getString(nameIdx) ?: number
            val type = when (it.getInt(typeIdx)) {
                AndroidCallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                AndroidCallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                AndroidCallLog.Calls.MISSED_TYPE -> CallType.MISSED
                else -> CallType.MISSED
            }
            val time = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", it.getLong(dateIdx)).toString()
            val user = User(id = number, name = name, phone = number)
            logs.add(
                CallLog(
                    user = user,
                    message = if (type == CallType.MISSED) "Missed" else type.name,
                    callType = type,
                    isMissed = (type == CallType.MISSED),
                    time = time
                )
            )
        }
    }
    return logs
}
