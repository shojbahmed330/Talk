package com.example.realtimecalltranslation

import android.Manifest
// import android.content.Context // Not directly used, LocalContext.current.applicationContext is used
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CallLog as AndroidCallLog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.amazonaws.regions.Regions
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.agora.DefaultRtcEngineEventHandler
import com.example.realtimecalltranslation.aws.S3Uploader
import androidx.lifecycle.ViewModelProvider
import com.example.realtimecalltranslation.util.AmazonTranscribeHelper
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import com.example.realtimecalltranslation.util.PollyTTSHelper
import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.example.realtimecalltranslation.ui.CallScreenViewModelFactory
import com.example.realtimecalltranslation.ui.theme.CallHistoryScreen
import com.example.realtimecalltranslation.ui.theme.DialerScreen
import com.example.realtimecalltranslation.ui.theme.FavouritesScreen
import com.example.realtimecalltranslation.ui.theme.ContactsScreen
import com.example.realtimecalltranslation.ui.LoginScreen
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.WelcomeScreen
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.theme.RealTimeCallTranslationTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
// Import all from theme, including colors like mainRed, accentRed etc.
import com.example.realtimecalltranslation.ui.theme.*
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.Message
import com.example.realtimecalltranslation.ui.theme.User
// Removed: import com.example.realtimecalltranslation.ui.theme.getRealCallLogs
import com.example.realtimecalltranslation.ui.getRealCallLogs // Corrected import


// IMPORTANT: Replace with your actual RapidAPI Key
const val RAPID_API_KEY_PLACEHOLDER = "YOUR_RAPID_API_KEY"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext
                val navController = rememberNavController()

                val agoraAppId = "YOUR_APP_ID"

                val awsAccessKey = "YOUR_AWS_ACCESS_KEY"
                val awsSecretKey = "YOUR_AWS_SECRET_KEY"
                val s3BucketName = "YOUR_S3_BUCKET_NAME"
                val s3Region = Regions.US_EAST_1

                val agoraManager = remember {
                    AgoraManager(applicationContext, agoraAppId, DefaultRtcEngineEventHandler)
                }

                LaunchedEffect(key1 = agoraManager) {
                    try {
                        agoraManager.init()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Agora initialization failed: ${e.message}")
                    }
                }

                val s3Uploader = remember {
                    S3Uploader(applicationContext, awsAccessKey, awsSecretKey, s3BucketName, s3Region)
                }

                LaunchedEffect(key1 = s3Uploader) {
                    try {
                        s3Uploader.initS3Client()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "S3Uploader initialization failed: ${e.message}")
                    }
                }

                val audioRecorderHelper = remember {
                    AudioRecorderHelper(applicationContext)
                }

                val amazonTranscribeHelper = remember {
                    AmazonTranscribeHelper(awsAccessKey, awsSecretKey, s3BucketName, s3Region)
                }

                val pollyHelper = remember {
                    PollyTTSHelper(
                        context = applicationContext,
                        accessKey = awsAccessKey,
                        secretKey = awsSecretKey
                    )
                }

                val callScreenViewModelFactory = CallScreenViewModelFactory(
                    audioRecorderHelper,
                    s3Uploader,
                    amazonTranscribeHelper,
                    pollyHelper,
                    agoraManager,
                    RAPID_API_KEY_PLACEHOLDER
                )
                val callScreenViewModel = ViewModelProvider(this, callScreenViewModelFactory)[CallScreenViewModel::class.java]

                DisposableEffect(Unit) {
                    onDispose {
                        agoraManager.destroy()
                        pollyHelper.release()
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
                    CallLog(demoUsers[0], "Can you translate this", CallType.INCOMING, false, "12 min ago"),
                    CallLog(demoUsers[1], "Missed call", CallType.MISSED, true, "10 min ago"),
                    CallLog(demoUsers[2], "Hello!", CallType.OUTGOING, false, "8 min ago"),
                    CallLog(demoUsers[3], "Test call", CallType.INCOMING, false, "5 min ago"),
                    CallLog(demoUsers[4], "Another call", CallType.OUTGOING, false, "2 min ago")
                )

                val hasPermission = ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                val realCallLogs = remember(hasPermission) {
                    if (hasPermission) getRealCallLogs(applicationContext) else emptyList<CallLog>()
                }

                val users = if (realCallLogs.isNotEmpty()) realCallLogs.map { it.user }.distinctBy { it.id } else demoUsers
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
                        CallHistoryScreen(
                            callLogs = callLogs,
                            onProfile = { user ->
                                navController.navigate("profile/${user.id}")
                            },
                            onCall = { user ->
                                navController.navigate("call/${user.phone}")
                            },
                            onAddNew = {
                                navController.navigate("dialer")
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
                    composable("favourites") {
                        FavouritesScreen(
                           onBack = { navController.navigate("callhistory") { popUpTo("callhistory") { inclusive = true } } },
                           mainRed = mainRed,
                           mainWhite = mainWhite,
                           accentRed = accentRed,
                           lightRed = lightRed
                        )
                    }
                    composable("contacts") {
                        ContactsScreen(
                            onBack = { navController.popBackStack() },
                            onCallContact = { phoneNumber ->
                                navController.navigate("call/$phoneNumber")
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
                        val user = users.find { it.id == userId }
                        if (user != null) {
                            ProfileScreen(
                                user = user,
                                callLogs = callLogs.filter { it.user.id == user.id },
                                onBack = { navController.popBackStack() },
                                onCall = { userToCall ->
                                    navController.navigate("call/${userToCall.phone}")
                                },
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
                                navController.navigate("call/$number")
                            }
                        )
                    }
                    composable(
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        CallScreen(
                            callScreenViewModel = callScreenViewModel,
                            agoraManager = agoraManager,
                            channel = number,
                            token = null,
                            localIsUsa = true,
                            messages = listOf(
                                Message(
                                    fromUsa = true,
                                    original = "How are you?",
                                    translated = "কেমন আছো?"
                                ),
                                Message(
                                    fromUsa = false,
                                    original = "Ami bhalo achi.",
                                    translated = "I am fine."
                                )
                            ),
                            mainRed = mainRed,
                            mainWhite = mainWhite,
                            onCallEnd = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}