package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateFormat // Added import
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.realtimecalltranslation.ui.theme.getRealCallLogs
import androidx.core.content.ContextCompat
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.theme.CallHistoryScreen
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.theme.DialerScreen
import com.example.realtimecalltranslation.ui.theme.FavouritesScreen
import com.example.realtimecalltranslation.ui.theme.ContactsScreen
import com.example.realtimecalltranslation.ui.theme.User

@Composable
fun MainNavigation(
    callScreenViewModel: CallScreenViewModel,
    agoraManager: AgoraManager,
    token: String?,
    localIsUsa: Boolean,
    appId: String
) {
    var currentScreen by remember { mutableStateOf("history") }
    var callTo by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedNav by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    val now = System.currentTimeMillis()
    val demoUsers = listOf(
        User("1", "Demo User Nav", "01700000000", null),
        User("2", "Has Pic Nav", "01800000000", "https://randomuser.me/api/portraits/men/1.jpg"),
        User("3", "No Pic Nav", "01900000000", null)
    )

    val demoCallLogs = listOf(
        CallLog(
            user = demoUsers[0],
            message = "Incoming Call", // Simplified
            callType = CallType.INCOMING,
            isMissed = false,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 300000L).toString(), // 5 mins ago
            timestamp = now - 300000L,
            duration = 120L // 2 minutes
        ),
        CallLog(
            user = demoUsers[1],
            message = "Missed Call", // Simplified
            callType = CallType.MISSED,
            isMissed = true,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 600000L).toString(), // 10 mins ago
            timestamp = now - 600000L,
            duration = 0L
        ),
        CallLog(
            user = demoUsers[2],
            message = "Outgoing Call", // Simplified
            callType = CallType.OUTGOING,
            isMissed = false,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 1200000L).toString(), // 20 mins ago
            timestamp = now - 1200000L,
            duration = 300L // 5 minutes
        )
    )

    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED

    val realCallLogs = remember(hasPermission) {
        if (hasPermission) getRealCallLogs(context) else emptyList<CallLog>()
    }

    val activeCallLogs = if (realCallLogs.isNotEmpty()) realCallLogs else demoCallLogs

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
            mainWhite = mainWhite,
            onNavigateToCall = { number ->
                callTo = number
                currentScreen = "call"
            }
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
            onCallContact = { phoneNumber ->
                callTo = phoneNumber
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
            channel = callTo ?: "",
            token = token,
            appId = appId,
            localIsUsa = localIsUsa,
            onCallEnd = {
                selectedNav = 0
                currentScreen = "history"
            },
            mainRed = mainRed,
            mainWhite = mainWhite,
            callScreenViewModel = callScreenViewModel
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
                    onCall = { userToCall ->
                        callTo = userToCall.phone
                        currentScreen = "call"
                    },
                    mainRed = mainRed,
                    mainWhite = mainWhite,
                    onNameUpdate = { newName ->
                        selectedUser = selectedUser?.copy(name = newName)
                    },
                    onProfilePicUriSelected = { uriString ->
                        // প্রোফাইল ছবি আপডেটের লজিক যোগ করতে পারেন
                    },
                    imageDataSource = user.profilePicUrl
                )
            }
        }
    }
}