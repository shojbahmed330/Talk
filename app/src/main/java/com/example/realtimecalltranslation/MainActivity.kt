package com.example.realtimecalltranslation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.amazonaws.regions.Regions
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.agora.DefaultRtcEngineEventHandler
import com.example.realtimecalltranslation.aws.S3Uploader
import androidx.lifecycle.ViewModelProvider
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
import com.example.realtimecalltranslation.ui.theme.*
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.getRealCallLogs
import com.example.realtimecalltranslation.ui.theme.getContactDetailsByNumber // Added import
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import com.example.realtimecalltranslation.util.AmazonTranscribeHelper
import com.example.realtimecalltranslation.util.PollyTTSHelper
import kotlinx.coroutines.launch
import java.io.InputStream

// IMPORTANT: Replace with your actual RapidAPI Key
const val RAPID_API_KEY_PLACEHOLDER = "YOUR_RAPID_API_KEY"

// Stub implementations for unresolved classes
class AudioRecorderHelper(context: android.content.Context) {
    init {
        // Basic initialization to use context
        println("AudioRecorderHelper initialized with context: $context")
    }
}

class AmazonTranscribeHelper(accessKey: String, secretKey: String, bucketName: String, region: Regions) {
    init {
        // Basic initialization to use parameters
        println("AmazonTranscribeHelper initialized with accessKey: $accessKey, bucketName: $bucketName")
    }
}

class PollyTTSHelper(context: android.content.Context, accessKey: String, secretKey: String) {
    fun release() {
        // Basic release logic
        println("PollyTTSHelper released")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                val agoraAppId = "YOUR_APP_ID"

                val awsAccessKey = "YOUR_AWS_ACCESS_KEY"
                val awsSecretKey = "YOUR_AWS_SECRET_KEY"
                val s3BucketName = "YOUR_S3_BUCKET_NAME"
                val s3Region = Regions.US_EAST_1

                val agoraManager = remember {
                    AgoraManager(applicationContext, agoraAppId, DefaultRtcEngineEventHandler)
                }
                LaunchedEffect(key1 = agoraManager) { try { agoraManager.init() } catch (e: Exception) { android.util.Log.e("MainActivity", "Agora init failed: ${e.message}") } }

                val s3Uploader = remember { S3Uploader(applicationContext, awsAccessKey, awsSecretKey, s3BucketName, s3Region) }
                LaunchedEffect(key1 = s3Uploader) { try { s3Uploader.initS3Client() } catch (e: Exception) { android.util.Log.e("MainActivity", "S3Uploader init failed: ${e.message}") } }

                val audioRecorderHelper = remember { AudioRecorderHelper(applicationContext) }
                val amazonTranscribeHelper = remember { AmazonTranscribeHelper(awsAccessKey, awsSecretKey, s3BucketName, s3Region) }
                val pollyHelper = remember { PollyTTSHelper(context = applicationContext, accessKey = awsAccessKey, secretKey = awsSecretKey) }

                val callScreenViewModelFactory = CallScreenViewModelFactory(applicationContext, audioRecorderHelper, s3Uploader, amazonTranscribeHelper, pollyHelper, agoraManager, RAPID_API_KEY_PLACEHOLDER)
                val callScreenViewModel: CallScreenViewModel = ViewModelProvider(this, callScreenViewModelFactory)[CallScreenViewModel::class.java]

                DisposableEffect(Unit) { onDispose { agoraManager.destroy(); pollyHelper.release() } }

                val demoUsers = listOf(
                    User("1", "Shojib", "017XXXXXXXX", "https://randomuser.me/api/portraits/men/96.jpg"),
                    User("2", "Sumi", "018XXXXXXXX", "https://randomuser.me/api/portraits/men/1.jpg"),
                    User("3", "Prithibi", "019XXXXXXXX", "https://randomuser.me/api/portraits/women/96.jpg"),
                    User("4", "JibOn", "016XXXXXXXX", "https://randomuser.me/api/portraits/women/2.jpg"),
                    User("5", "Maa GP", "015XXXXXXXX", "https://randomuser.me/api/portraits/women/26.jpg")
                )
                val demoCallLogsState = remember {
                    mutableStateListOf<CallLog>().apply {
                        val now = System.currentTimeMillis()
                        val demoDurations = listOf(60L, 125L, 0L, 300L, 15L) // Sample durations in seconds
                        val demoMessages = listOf(
                            "Discussing project details",
                            "Quick check-in",
                            "Missed this one",
                            "Long conversation about weekend",
                            "Regarding the upcoming event"
                        )

                        addAll(demoUsers.mapIndexed { index, user ->
                            val callTimestamp = now - (index * 5 * 60 * 1000L) - (index * 30000L) // Offset by index for variety
                            val callType = if (index % 2 == 0) CallType.INCOMING else CallType.OUTGOING
                            val isMissedCall = index == 2 // Let's make the 3rd call a missed call for variety
                            val durationSeconds = if (isMissedCall) 0L else demoDurations.getOrElse(index) { 30L }

                            CallLog(
                                user = user,
                                message = demoMessages.getOrElse(index) { "Sample call ${index + 1}" },
                                callType = if (isMissedCall) CallType.MISSED else callType,
                                isMissed = isMissedCall,
                                formattedDateTime = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", callTimestamp).toString(),
                                timestamp = callTimestamp,
                                duration = durationSeconds
                            )
                        })
                    }
                }

                val hasPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) }
                LaunchedEffect(hasPermission) {
                    callLogsFromSource = if (hasPermission) getRealCallLogs(applicationContext) else demoCallLogsState
                }

                var userToLog by remember { mutableStateOf<User?>(null) }
                var profileScreenImageDisplayData by remember { mutableStateOf<Any?>(null) }

                val callLogsToDisplay = if (callLogsFromSource.isNotEmpty()) callLogsFromSource else demoCallLogsState
                val usersToDisplay = if (callLogsFromSource.isNotEmpty()) callLogsFromSource.map { it.user }.distinctBy { it.id } else demoUsers

                NavHost(navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(onGetStarted = { navController.navigate("login") }) }
                    composable("login") { LoginScreen(onLogin = { navController.navigate("callhistory") }, onFacebook = {}, onGoogle = {}, onSignUp = {}) }

                    composable("callhistory") {
                        CallHistoryScreen(
                            callLogs = callLogsToDisplay,
                            onProfile = { user -> userToLog = user; profileScreenImageDisplayData = user.profilePicUrl; navController.navigate("profile/${user.id}") },
                            onCall = { user -> userToLog = user; navController.navigate("call/${user.phone}") },
                            onUserAvatar = { user -> userToLog = user; profileScreenImageDisplayData = user.profilePicUrl; navController.navigate("profile/${user.id}") },
                            onFavourites = { navController.navigate("favourites") },
                            onDialer = { navController.navigate("dialer") },
                            onContacts = { navController.navigate("contacts") },
                            selectedNav = 0,
                            mainRed = mainRed, mainWhite = mainWhite, accentRed = accentRed, lightRed = lightRed
                        )
                    }
                    composable("favourites") {
                        FavouritesScreen(
                            onBack = { navController.navigate("callhistory") { popUpTo("callhistory") { inclusive = true } } },
                            mainRed = mainRed, mainWhite = mainWhite, accentRed = accentRed, lightRed = lightRed
                        )
                    }
                    composable("contacts") {
                        ContactsScreen(
                            onBack = { navController.popBackStack() },
                            onCallContact = { phoneNumber -> userToLog = usersToDisplay.find { it.phone == phoneNumber }; navController.navigate("call/$phoneNumber") },
                            mainRed = mainRed, accentRed = accentRed, mainWhite = mainWhite, mainGreen = mainGreen, lightGreen = lightGreen, lightRed = lightRed
                        )
                    }
                    composable(
                        route = "profile/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val currentUserId = backStackEntry.arguments?.getString("userId")
                        val userForProfile = usersToDisplay.find { it.id == currentUserId } ?: userToLog?.takeIf { it.id == currentUserId }

                        if (userForProfile != null) {
                            if (userToLog == null || userToLog?.id != currentUserId) {
                                userToLog = userForProfile
                                profileScreenImageDisplayData = userForProfile.profilePicUrl
                            }
                            ProfileScreen(
                                user = userForProfile,
                                callLogs = callLogsToDisplay.filter { it.user.id == currentUserId },
                                imageDataSource = profileScreenImageDisplayData,
                                onNameUpdate = { newName ->
                                    callLogsFromSource = callLogsFromSource.map { log ->
                                        if (log.user.id == currentUserId) { log.copy(user = log.user.copy(name = newName)) } else log
                                    }
                                    if (userToLog?.id == currentUserId) { userToLog = userToLog?.copy(name = newName) }
                                },
                                onProfilePicUriSelected = { uriString ->
                                    profileScreenImageDisplayData = null
                                    callLogsFromSource = callLogsFromSource.map { log ->
                                        if (log.user.id == currentUserId) { log.copy(user = log.user.copy(profilePicUrl = uriString)) } else log
                                    }
                                    if (userToLog?.id == currentUserId) { userToLog = userToLog?.copy(profilePicUrl = uriString) }
                                    if (uriString != null) {
                                        scope.launch {
                                            try {
                                                val inputStream: InputStream? = applicationContext.contentResolver.openInputStream(uriString.toUri())
                                                profileScreenImageDisplayData = inputStream?.readBytes() ?: uriString
                                                inputStream?.close()
                                            } catch (e: Exception) {
                                                android.util.Log.e("MainActivity", "Error reading image URI: $uriString", e)
                                                profileScreenImageDisplayData = uriString
                                            }
                                        }
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                onCall = { userToCall -> userToLog = userToCall; navController.navigate("call/${userToCall.phone}") },
                                mainRed = mainRed,
                                mainWhite = mainWhite
                            )
                        }
                    }
                    composable("dialer") {
                        DialerScreen(
                            onClose = { navController.popBackStack() },
                            mainRed = mainRed, mainWhite = mainWhite,
                            onNavigateToCall = { number ->
                                userToLog = usersToDisplay.find { it.phone == number } ?: User(id = number, name = number, phone = number)
                                navController.navigate("call/$number")
                            }
                        )
                    }
                    composable(
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val number = backStackEntry.arguments?.getString("number") ?: ""
                        // Use a local variable for intermediate updates to avoid multiple recompositions if userToLog is frequently changed
                        var updatedUserToLog = userToLog

                        if (updatedUserToLog == null || updatedUserToLog.phone != number) {
                            updatedUserToLog = usersToDisplay.find { it.phone == number }
                                ?: User(id = number, name = number, phone = number, profilePicUrl = null)
                        }

                        // Attempt to fetch contact details if profilePicUrl is null
                        if (updatedUserToLog != null && updatedUserToLog.profilePicUrl == null) {
                            val contactDetails = getContactDetailsByNumber(applicationContext, updatedUserToLog.phone)
                            if (contactDetails != null) {
                                updatedUserToLog = updatedUserToLog.copy(
                                    name = contactDetails.name, // Update name from contact details
                                    profilePicUrl = contactDetails.photoUri ?: updatedUserToLog.profilePicUrl // Use new photoUri if available
                                )
                            }
                        }

                        // Update the actual state variable once all modifications are done
                        userToLog = updatedUserToLog

                        if (userToLog != null) {
                            CallScreen(
                                channel = userToLog!!.phone, // Use phone from userToLog
                                token = null,
                                appId = agoraAppId,
                                localIsUsa = true,
                                onCallEnd = { navController.popBackStack() },
                                mainRed = mainRed,
                                mainWhite = mainWhite,
                                callScreenViewModel = callScreenViewModel,
                                user = userToLog // Pass the updated user object
                            )
                        } else {
                            // Optional: Handle the case where userToLog is still null, e.g., pop back or show error
                            // navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}