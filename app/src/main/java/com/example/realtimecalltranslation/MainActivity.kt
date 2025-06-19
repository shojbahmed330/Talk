package com.example.realtimecalltranslation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.net.Uri // Added
import java.io.ByteArrayOutputStream // Added
import kotlinx.coroutines.CoroutineScope // Added
import kotlinx.coroutines.launch // Added
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.LoginScreen
import com.example.realtimecalltranslation.ui.Message
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.WelcomeScreen
import com.example.realtimecalltranslation.ui.getRealCallLogs
import com.example.realtimecalltranslation.ui.theme.CallHistoryScreen
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.theme.ContactsScreen
import com.example.realtimecalltranslation.ui.theme.DialerScreen
import com.example.realtimecalltranslation.ui.theme.FavouritesScreen
import com.example.realtimecalltranslation.ui.theme.RealTimeCallTranslationTheme
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.accentRed
import com.example.realtimecalltranslation.ui.theme.lightGreen
import com.example.realtimecalltranslation.ui.theme.lightRed
import com.example.realtimecalltranslation.ui.theme.mainGreen
import com.example.realtimecalltranslation.ui.theme.mainRed
import com.example.realtimecalltranslation.ui.theme.mainWhite
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealTimeCallTranslationTheme {
                val navController = rememberNavController()
                var callHistoryRecomposeKey by remember { mutableStateOf(0) }

                var profileScreenImageDisplayData by remember { mutableStateOf<Any?>(null) } // Added
                val scope = rememberCoroutineScope() // Added

                var numberToLog by remember { mutableStateOf<String?>(null) }
                var userToLog by remember { mutableStateOf<User?>(null) }
                var callStartTimeForLog by remember { mutableStateOf(0L) }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            callHistoryRecomposeKey++
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) }
                val localContext = LocalContext.current

                val hasPermission = ContextCompat.checkSelfPermission(
                    localContext, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                LaunchedEffect(hasPermission, callHistoryRecomposeKey) {
                    try { // Add try
                        if (hasPermission) {
                            val logs = withContext(Dispatchers.IO) {
                                getRealCallLogs(localContext) // localContext should be defined
                            }
                            callLogsFromSource = logs
                        } else {
                            callLogsFromSource = emptyList()
                        }
                    } catch (e: Exception) { // Add catch
                        Log.e("MainActivityLoad", "Error loading call logs: ${e.message}", e)
                        callLogsFromSource = emptyList() // Set to a safe default on error
                    }
                }

                val demoUsers = listOf(
                    User("1", "Shojib", "017XXXXXXXX", "https://randomuser.me/api/portraits/men/96.jpg"),
                    User("2", "Sumi", "018XXXXXXXX", "https://randomuser.me/api/portraits/men/1.jpg"),
                    User("3", "Prithibi", "019XXXXXXXX", "https://randomuser.me/api/portraits/women/96.jpg"),
                    User("4", "JibOn", "016XXXXXXXX", "https://randomuser.me/api/portraits/women/2.jpg"),
                    User("5", "Maa GP", "015XXXXXXXX", "https://randomuser.me/api/portraits/women/26.jpg")
                )
                val demoCallLogs = listOf(
                    CallLog(demoUsers[0], "Can translate", CallType.INCOMING, false, "29 Oct 2023, 10:00 AM"),
                    CallLog(demoUsers[1], "Missed call", CallType.MISSED, true, "29 Oct 2023, 09:30 AM"),
                    CallLog(demoUsers[2], "Hello!", CallType.OUTGOING, false, "28 Oct 2023, 05:00 PM")
                )

                val finalCallLogsToDisplay = if (callLogsFromSource.isNotEmpty()) {
                    callLogsFromSource
                } else {
                    demoCallLogs
                }

                val usersToDisplay = (if (callLogsFromSource.isNotEmpty()) {
                    callLogsFromSource.map { it.user }
                } else {
                    demoUsers
                }).distinctBy { it.id }

                NavHost(navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(onGetStarted = { navController.navigate("login") })
                    }
                    composable("login") {
                        LoginScreen(
                            onLogin = { navController.navigate("callhistory") },
                            onFacebook = { /* TODO */ },
                            onGoogle = { /* TODO */ },
                            onSignUp = { /* TODO */ }
                        )
                    }
                    composable("callhistory") {
                        key(callHistoryRecomposeKey) {
                            CallHistoryScreen(
                                callLogs = finalCallLogsToDisplay,
                                onProfile = { user -> navController.navigate("profile/${user.id}") },
                                onCall = { user ->
                                    numberToLog = user.phone
                                    userToLog = user
                                    callStartTimeForLog = System.currentTimeMillis()
                                    navController.navigate("call/${user.phone}")
                                },
                                onUserAvatar = { user -> navController.navigate("profile/${user.id}") },
                                onFavourites = { navController.navigate("favourites") },
                                onDialer = { navController.navigate("dialer") },
                                onContacts = { navController.navigate("contacts") },
                                selectedNav = 0,
                                mainRed = mainRed,
                                mainWhite = mainWhite,
                                accentRed = accentRed,
                                lightRed = lightRed
                            )
                        }
                    }
                    composable("favourites") {
                        FavouritesScreen(
                            onBack = { navController.popBackStack() },
                            mainRed = mainRed,
                            mainWhite = mainWhite,
                            accentRed = accentRed,
                            lightRed = lightRed
                        )
                    }
                    composable("contacts") {
                        ContactsScreen(
                            onBack = { navController.popBackStack() },
                            onCallContact = { contactNameArg, phoneNumberArg -> // Correct signature
                                numberToLog = phoneNumberArg
                                userToLog = User(
                                    id = phoneNumberArg,
                                    name = contactNameArg,
                                    phone = phoneNumberArg,
                                    profilePicUrl = usersToDisplay.find { it.phone == phoneNumberArg }?.profilePicUrl
                                )
                                callStartTimeForLog = System.currentTimeMillis()
                                val encodedName = URLEncoder.encode(contactNameArg, StandardCharsets.UTF_8.name())
                                navController.navigate("call/$phoneNumberArg?name=$encodedName")
                            },
                            mainRed = mainRed,
                            accentRed = accentRed,
                            mainWhite = mainWhite,
                            mainGreen = mainGreen,
                            lightGreen = lightGreen,
                            lightRed = lightRed
                        )
                    }
                    composable(
                        route = "profile/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId") ?: ""
                        val user = usersToDisplay.find { it.id == userId }
                        if (user != null) {
                            LaunchedEffect(user.id) { // Keyed to user.id
                                profileScreenImageDisplayData = user.profilePicUrl
                            }
                            ProfileScreen(
                                user = user,
                                callLogs = finalCallLogsToDisplay.filter { it.user.id == user.id },
                                onBack = { navController.popBackStack() },
                                onCall = { u ->
                                    numberToLog = u.phone
                                    userToLog = u
                                    callStartTimeForLog = System.currentTimeMillis()
                                    navController.navigate("call/${u.phone}")
                                },
                                onNameUpdate = { newName ->
                                    val currentUserId = backStackEntry.arguments?.getString("userId")
                                    if (currentUserId != null) {
                                        // Existing name update logic...
                                        if (callLogsFromSource.any { log -> log.user.id == currentUserId }) {
                                            callLogsFromSource = callLogsFromSource.map { log ->
                                                if (log.user.id == currentUserId) {
                                                    log.copy(user = log.user.copy(name = newName))
                                                } else { log }
                                            }
                                        }
                                        if (userToLog?.id == currentUserId) {
                                            userToLog = userToLog?.copy(name = newName)
                                        }
                                    }
                                },
                                onProfilePicUriSelected = { uriStr ->
                                    val currentUserId = backStackEntry.arguments?.getString("userId")
                                    if (currentUserId != null) {
                                        // Step 1: Immediately set display data to null
                                        profileScreenImageDisplayData = null

                                        // Step 2: Update the underlying data model (user.profilePicUrl) with the URI string or null
                                        val newProfilePicUrl = uriStr

                                        if (callLogsFromSource.any { log -> log.user.id == currentUserId }) {
                                            callLogsFromSource = callLogsFromSource.map { log ->
                                                if (log.user.id == currentUserId) {
                                                    log.copy(user = log.user.copy(profilePicUrl = newProfilePicUrl))
                                                } else { log }
                                            }
                                        }
                                        if (userToLog?.id == currentUserId) {
                                            userToLog = userToLog?.copy(profilePicUrl = newProfilePicUrl)
                                        }
                                        // Note: The original 'else' for updating with null is covered by newProfilePicUrl being null.

                                        // Step 3: Asynchronously convert URI to ByteArray (if URI is not null)
                                        if (newProfilePicUrl != null) {
                                            scope.launch {
                                                Log.d("MainActivityByteReader", "Attempting to read/convert URI: $newProfilePicUrl")
                                                val byteArray = try {
                                                    withContext(Dispatchers.IO) {
                                                        localContext.contentResolver.openInputStream(Uri.parse(newProfilePicUrl))?.use { inputStream ->
                                                            val byteArrayOutputStream = ByteArrayOutputStream()
                                                            inputStream.copyTo(byteArrayOutputStream)
                                                            byteArrayOutputStream.toByteArray()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("MainActivityByteReader", "Failed to read/convert URI to ByteArray: $newProfilePicUrl", e)
                                                    null
                                                }

                                                if (byteArray != null) {
                                                    Log.d("MainActivityByteReader", "Successfully converted URI to ByteArray, size: ${byteArray.size}")
                                                    profileScreenImageDisplayData = byteArray
                                                } else {
                                                    profileScreenImageDisplayData = newProfilePicUrl // Fallback to URI string
                                                }
                                            }
                                        }
                                        // If newProfilePicUrl was null, profileScreenImageDisplayData remains null (set in Step 1)
                                    }
                                },
                                imageDataSource = profileScreenImageDisplayData, // Pass the new state
                                mainRed = mainRed,
                                mainWhite = mainWhite
                            )
                        } else {
                            // If user is not found (e.g., invalid userId), clear the display data.
                            LaunchedEffect(Unit) {
                                profileScreenImageDisplayData = null
                            }
                        }
                    }
                    composable("dialer") {
                        DialerScreen(
                            onClose = { navController.popBackStack() },
                            mainRed = mainRed,
                            mainWhite = mainWhite,
                            onNavigateToCall = { number ->
                                numberToLog = number
                                userToLog = usersToDisplay.find { it.phone == number }
                                callStartTimeForLog = System.currentTimeMillis()
                                navController.navigate("call/$number")
                            }
                        )
                    }
                    composable(
                        route = "call/{number}?name={name}",
                        arguments = listOf(
                            navArgument("number") { type = NavType.StringType },
                            navArgument("name") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        val nameFromRoute = backStackEntry.arguments?.getString("name")

                        val userFromLookup = usersToDisplay.find { it.phone == number }
                        val finalContactName = nameFromRoute ?: userFromLookup?.name ?: number
                        val finalContactProfilePicUrl = userFromLookup?.profilePicUrl

                        CallScreen(
                            channel = number,
                            contactName = finalContactName,
                            contactProfilePicUrl = finalContactProfilePicUrl,
                            token = null,
                            appId = "YOUR_APP_ID",
                            localIsUsa = true,
                            messages = emptyList(),
                            onCallEnd = {
                                navController.popBackStack()
                                if (numberToLog != null) {
                                    val callTime = callStartTimeForLog
                                    val callTimeFormatted = android.text.format.DateFormat.format(
                                        "dd MMM yyyy, h:mm a",
                                        callTime
                                    ).toString()

                                    val resolvedUser: User = userToLog?.takeIf { it.phone == numberToLog } ?: User(
                                        id = numberToLog!!,
                                        name = numberToLog!!,
                                        phone = numberToLog!!,
                                        profilePicUrl = null
                                    )

                                    val newCallLogEntry = CallLog(
                                        user = resolvedUser,
                                        message = "App Call",
                                        callType = CallType.OUTGOING,
                                        isMissed = false,
                                        time = callTimeFormatted
                                    )
                                    callLogsFromSource = listOf(newCallLogEntry) + callLogsFromSource

                                    numberToLog = null
                                    userToLog = null
                                    callStartTimeForLog = 0L
                                }
                            },
                            onToggleLoudspeaker = { /* TODO */ },
                            onToggleMute = { /* TODO */ },
                            mainRed = mainRed,
                            mainWhite = mainWhite
                        )
                    }
                }
            }
        }
    }
}