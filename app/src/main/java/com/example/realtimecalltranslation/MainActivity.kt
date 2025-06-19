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
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.Message
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.getRealCallLogs

// IMPORTANT: Replace with your actual RapidAPI Key
const val RAPID_API_KEY_PLACEHOLDER = "YOUR_RAPID_API_KEY"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext // Get application context
                val navController = rememberNavController()

                // Define Agora App ID - REPLACE WITH YOUR ACTUAL APP ID
                val agoraAppId = "YOUR_APP_ID" // Or fetch from string resources, build config, etc.

                // AWS S3 Configuration - REPLACE WITH YOUR ACTUAL CREDENTIALS AND BUCKET INFO
                // IMPORTANT: DO NOT COMMIT ACTUAL CREDENTIALS TO VERSION CONTROL
                // Consider using local.properties, BuildConfig, or a secure backend for these.
                val awsAccessKey = "YOUR_AWS_ACCESS_KEY"
                val awsSecretKey = "YOUR_AWS_SECRET_KEY"
                val s3BucketName = "YOUR_S3_BUCKET_NAME"
                val s3Region = Regions.US_EAST_1 // Example: Change to your bucket's region

                // Create and remember AgoraManager instance
                val agoraManager = remember {
                    AgoraManager(applicationContext, agoraAppId, DefaultRtcEngineEventHandler)
                }

                // Initialize AgoraManager once
                LaunchedEffect(key1 = agoraManager) {
                    try {
                        agoraManager.init()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Agora initialization failed: ${e.message}")
                    }
                }

                // Create and remember S3Uploader instance
                val s3Uploader = remember {
                    S3Uploader(applicationContext, awsAccessKey, awsSecretKey, s3BucketName, s3Region)
                }

                // Initialize S3Uploader once
                LaunchedEffect(key1 = s3Uploader) {
                    try {
                        s3Uploader.initS3Client()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "S3Uploader initialization failed: ${e.message}")
                    }
                }

                // Create and remember AudioRecorderHelper instance
                val audioRecorderHelper = remember {
                    AudioRecorderHelper(applicationContext)
                }

                // Create and remember AmazonTranscribeHelper instance
                // Note: Using s3BucketName also for Transcribe output. Ensure this bucket is configured for Transcribe.
                val amazonTranscribeHelper = remember {
                    AmazonTranscribeHelper(awsAccessKey, awsSecretKey, s3BucketName, s3Region)
                }

                // Create and remember PollyTTSHelper instance
                val pollyHelper = remember {
                    PollyTTSHelper(
                        context = applicationContext,
                        accessKey = awsAccessKey,
                        secretKey = awsSecretKey
                        // region can be defaulted in PollyTTSHelper or passed if needed
                    )
                }

                // Create ViewModel instance using the factory
                val callScreenViewModelFactory = CallScreenViewModelFactory(
                    audioRecorderHelper,
                    s3Uploader,
                    amazonTranscribeHelper,
                    pollyHelper,
                    agoraManager, // Pass AgoraManager to ViewModel factory
                    RAPID_API_KEY_PLACEHOLDER
                )
                val callScreenViewModel = ViewModelProvider(this, callScreenViewModelFactory)[CallScreenViewModel::class.java]

                // Ensure resources are released when the activity is destroyed
                DisposableEffect(Unit) {
                    onDispose {
                        agoraManager.destroy()
                        pollyHelper.release() // Release Polly resources
                        // s3Uploader does not have a specific destroy/release method
                        // ViewModel's onCleared will be called automatically
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

                // val context = LocalContext.current // applicationContext is already available

                // Real call logs (from Android)
                val hasPermission = ContextCompat.checkSelfPermission(
                    applicationContext, Manifest.permission.READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED

                val realCallLogs = remember(hasPermission) {
                    if (hasPermission) getRealCallLogs(applicationContext) else emptyList()
                }
                val realUsers = realCallLogs.map { it.user }.distinctBy { it.id }

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
                        // এখানে CallHistoryScreen ব্যবহার করুন
                        CallHistoryScreen(
                            userList = users,
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
                            selectedNav = 0
                        )
                    }
                    composable("favourites") {
                        FavouritesScreen()
                    }
                    composable("contacts") {
                        ContactsScreen()
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
                                onCall = { navController.navigate("call/${user.phone}") }
                            )
                        }
                    }
                    composable("dialer") {
                        DialerScreen(
                            onClose = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        val mainRedColor = MaterialTheme.colorScheme.primary // Example color
                        val mainWhiteColor = Color.White // Example color

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
                            mainRed = mainRedColor,
                            mainWhite = mainWhiteColor,
                            onCallEnd = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}