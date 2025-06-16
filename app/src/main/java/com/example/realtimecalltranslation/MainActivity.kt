package com.example.realtimecalltranslation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog as AndroidCallLog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.realtimecalltranslation.ui.theme.CallHistoryScreen
import com.example.realtimecalltranslation.ui.theme.DialerScreen
import com.example.realtimecalltranslation.ui.theme.FavouritesScreen
import com.example.realtimecalltranslation.ui.theme.ContactsScreen
import com.example.realtimecalltranslation.ui.LoginScreen
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.WelcomeScreen
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.theme.RealTimeCallTranslationTheme
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.Message
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.getRealCallLogs
import com.example.realtimecalltranslation.ui.theme.mainRed
import com.example.realtimecalltranslation.ui.theme.mainWhite
import com.example.realtimecalltranslation.ui.theme.accentRed
import com.example.realtimecalltranslation.ui.theme.lightRed
import com.example.realtimecalltranslation.ui.theme.mainGreen
import com.example.realtimecalltranslation.ui.theme.lightGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealTimeCallTranslationTheme {
                val navController = rememberNavController()
                var callHistoryRecomposeKey by remember { mutableStateOf(0) }

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
                val localContext = LocalContext.current // Renamed from context to avoid conflict if any

                // Permission check moved up before its first use in LaunchedEffect
                val hasPermission = ContextCompat.checkSelfPermission(
                    localContext, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                LaunchedEffect(hasPermission, callHistoryRecomposeKey) {
                    if (hasPermission) {
                        val logs = withContext(Dispatchers.IO) {
                            getRealCallLogs(localContext)
                        }
                        callLogsFromSource = logs
                    } else {
                        callLogsFromSource = emptyList() // Clear if no permission
                    }
                }

                // Demo users & logs
                val demoUsers = listOf(
                    User("1", "Shojib", "017XXXXXXXX", "https://randomuser.me/api/portraits/men/96.jpg"),
                    User("2", "Sumi", "018XXXXXXXX", "https://randomuser.me/api/portraits/men/1.jpg"),
                    User("3", "Prithibi", "019XXXXXXXX", "https://randomuser.me/api/portraits/women/96.jpg"),
                    User("4", "JibOn", "016XXXXXXXX", "https://randomuser.me/api/portraits/women/2.jpg"),
                    User("5", "Maa GP", "015XXXXXXXX", "https://randomuser.me/api/portraits/women/26.jpg")
                )
                val demoCallLogs = listOf(
                    CallLog(demoUsers[0], "Can you translate this", CallType.INCOMING, false, "28 Oct 2023, 09:15 AM"),
                    CallLog(demoUsers[1], "Missed call", CallType.MISSED, true, "28 Oct 2023, 09:12 AM"),
                    CallLog(demoUsers[2], "Hello!", CallType.OUTGOING, false, "27 Oct 2023, 05:30 PM"),
                    CallLog(demoUsers[3], "Test call", CallType.INCOMING, false, "27 Oct 2023, 02:00 PM"),
                    CallLog(demoUsers[4], "Another call", CallType.OUTGOING, false, "26 Oct 2023, 11:00 AM")
                )

                // val context = LocalContext.current // Commented out as localContext is defined above

                // Permission check remains, used by LaunchedEffect and fallback logic
                // val hasPermission = ContextCompat.checkSelfPermission(...) // Definition moved up

                // Determine final data to display
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
                        WelcomeScreen(
                            onGetStarted = { navController.navigate("login") }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            onLogin = { navController.navigate("callhistory") },
                            onFacebook = { /* Facebook login logic */ },
                            onGoogle = { /* Google login logic */ },
                            onSignUp = { /* Signup logic */ }
                        )
                    }
                    composable("callhistory") {
                        key(callHistoryRecomposeKey) {
                            // 여기 CallHistoryScreen 사용 করুন
                            CallHistoryScreen(
                            callLogs = finalCallLogsToDisplay,
                                onProfile = { user ->
                                    navController.navigate("profile/${user.id}")
                            },
                            onCall = { user ->
                                numberToLog = user.phone
                                userToLog = user
                                callStartTimeForLog = System.currentTimeMillis()
                                navController.navigate("call/${user.phone}")
                            },
                            onUserAvatar = { user ->
                                navController.navigate("profile/${user.id}")
                            },
                            onFavourites = {
                                navController.navigate("favourites")
                            },
                            onDialer = {
                                navController.navigate("dialer")
                            },
                            onContacts = {
                                navController.navigate("contacts")
                            },
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
                            onCallContact = { number -> navController.navigate("call/$number") },
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
                        val user = usersToDisplay.find { it.id == userId } // Use usersToDisplay
                        if (user != null) {
                            ProfileScreen(
                                user = user,
                                callLogs = finalCallLogsToDisplay.filter { it.user.id == user.id }, // Use finalCallLogsToDisplay
                                onBack = { navController.popBackStack() },
                                onCall = { navController.navigate("call/${user.phone}") },
                                mainRed = mainRed,
                                mainWhite = mainWhite
                            )
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
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        val user = usersToDisplay.find { it.phone == number }
                        val contactName = user?.name ?: number // Use number as fallback name
                        val contactProfilePicUrl = user?.profilePicUrl

                        CallScreen(
                            channel = number,
                            contactName = contactName,
                            contactProfilePicUrl = contactProfilePicUrl,
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

                                    val resolvedUser = userToLog?.takeIf { it.phone == numberToLog } ?: User(
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
                                // callHistoryRecomposeKey++ // This line should remain commented out or removed
                            },
                            onToggleLoudspeaker = { /* TODO: Implement loudspeaker logic */ },
                            onToggleMute = { /* TODO: Implement mute logic */ },
                            mainRed = mainRed,
                            mainWhite = mainWhite
                        )
                    }
                }
            }
        }
    }
}