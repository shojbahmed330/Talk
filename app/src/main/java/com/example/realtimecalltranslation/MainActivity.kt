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

                // Demo users & logs
                val demoUsers = listOf(
                    User("1", "Shojib", "017XXXXXXXX", "https://randomuser.me/api/portraits/men/96.jpg"),
                    User("2", "Sumi", "018XXXXXXXX", "https://randomuser.me/api/portraits/men/1.jpg"),
                    User("3", "Prithibi", "019XXXXXXXX", "https://randomuser.me/api/portraits/women/96.jpg"),
                    User("4", "JibOn", "016XXXXXXXX", "https://randomuser.me/api/portraits/women/2.jpg"),
                    User("5", "Maa GP", "015XXXXXXXX", "https://randomuser.me/api/portraits/women/26.jpg")
                )
                val demoCallLogs = listOf(
                    CallLog(demoUsers[0], "Can you translate this", CallType.INCOMING, false, "12 min ago"),
                    CallLog(demoUsers[1], "Missed call", CallType.MISSED, true, "10 min ago"),
                    CallLog(demoUsers[2], "Hello!", CallType.OUTGOING, false, "8 min ago"),
                    CallLog(demoUsers[3], "Test call", CallType.INCOMING, false, "5 min ago"),
                    CallLog(demoUsers[4], "Another call", CallType.OUTGOING, false, "2 min ago")
                )

                val context = LocalContext.current

                // Real call logs (from Android)
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                val realCallLogs = remember(hasPermission) {
                    if (hasPermission) getRealCallLogs(context) else emptyList()
                }
                val realUsers: List<User> = realCallLogs.map { callLog: CallLog -> callLog.user }.distinctBy { user: User -> user.id }

                // Fallback: real log থাকলে ওটাই, না থাকলে demo
                val users = if (realCallLogs.isNotEmpty()) realUsers else demoUsers
                val callLogs = if (realCallLogs.isNotEmpty()) realCallLogs else demoCallLogs

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
                                callLogs = callLogs,
                                onProfile = { user ->
                                    navController.navigate("profile/${user.id}")
                            },
                            onCall = { user ->
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
                        val user = users.find { it.id == userId }
                        if (user != null) {
                            ProfileScreen(
                                user = user,
                                callLogs = callLogs.filter { it.user.id == user.id },
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
                            mainWhite = mainWhite
                        )
                    }
                    composable(
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        CallScreen(
                            channel = number,
                            token = null,
                            appId = "YOUR_APP_ID",
                            localIsUsa = true,
                            messages = emptyList(),
                            onCallEnd = {
                                navController.popBackStack()
                                // callHistoryRecomposeKey++ // Removed as per Subtask 16
                            },
                            mainRed = mainRed,
                            mainWhite = mainWhite
                        )
                    }
                }
            }
        }
    }
}