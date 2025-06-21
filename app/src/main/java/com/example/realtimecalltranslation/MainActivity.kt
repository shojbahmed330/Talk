package com.example.realtimecalltranslation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog as AndroidCallLogHost // Alias for CallLog provider URI
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.amazonaws.regions.Regions
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.agora.DefaultRtcEngineEventHandler
import com.example.realtimecalltranslation.aws.S3Uploader
import com.example.realtimecalltranslation.ui.CallScreen
import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.example.realtimecalltranslation.ui.CallScreenViewModelFactory
import com.example.realtimecalltranslation.ui.LoginScreen
import com.example.realtimecalltranslation.ui.ProfileScreen
import com.example.realtimecalltranslation.ui.WelcomeScreen
import com.example.realtimecalltranslation.ui.theme.*
import com.example.realtimecalltranslation.util.AmazonTranscribeHelper
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import com.example.realtimecalltranslation.util.PollyTTSHelper
import kotlinx.coroutines.launch
import java.io.InputStream
import androidx.compose.material3.Text // Explicit Text import

// IMPORTANT: Replace with your actual RapidAPI Key
const val RAPID_API_KEY_PLACEHOLDER = "YOUR_RAPID_API_KEY"

// Stub helper classes that were at the top of your file previously ARE NOW REMOVED.
// We will rely on the actual implementations in the .util package.
// Ensure these are correctly implemented in your .util package:
// com.example.realtimecalltranslation.util.AudioRecorderHelper
// com.example.realtimecalltranslation.util.AmazonTranscribeHelper
// com.example.realtimecalltranslation.util.PollyTTSHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Log.d("MainActivityDebug", "--- MainActivity setContent: Entered ---")
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                val agoraAppId = "7b2d5eaf4312454dbc61d86f0361a5d2" // Your Agora App ID

                val awsAccessKey = "YOUR_AWS_ACCESS_KEY"
                val awsSecretKey = "YOUR_AWS_SECRET_KEY"
                val s3BucketName = "YOUR_S3_BUCKET_NAME"
                val s3Region = Regions.US_EAST_1

                val agoraManager = remember {
                    Log.d("MainActivityDebug", "Creating AgoraManager instance with AppID: $agoraAppId")
                    AgoraManager(applicationContext, agoraAppId, DefaultRtcEngineEventHandler)
                }
                var isAgoraEngineInitialized by remember { mutableStateOf(false) }

                LaunchedEffect(key1 = Unit) { // Key changed to Unit
                    Log.d("MainActivityDebug", "--- Agora Init LaunchedEffect (key=Unit): CREATED (lambda scope entered) ---")
                    if (agoraManager == null) {
                        Log.e("MainActivityDebug", "Agora Init LaunchedEffect: agoraManager INSTANCE IS NULL at start. This is unexpected.")
                        isAgoraEngineInitialized = false
                        return@LaunchedEffect
                    }
                    Log.d("MainActivityDebug", "Agora Init LaunchedEffect: agoraManager instance is available.")
                    try {
                        Log.d("MainActivityDebug", "Agora Init LaunchedEffect: Preparing to call agoraManager.init().")
                        agoraManager.init()
                        Log.d("MainActivityDebug", "Agora Init LaunchedEffect: agoraManager.init() call has completed (no immediate exception).")

                        if (agoraManager.isInitialized()) {
                            isAgoraEngineInitialized = true
                            Log.d("MainActivityDebug", "Agora Init LaunchedEffect: SUCCESS - agoraManager.isInitialized() is true. MainActivity's isAgoraEngineInitialized set to true.")
                        } else {
                            isAgoraEngineInitialized = false
                            Log.e("MainActivityDebug", "Agora Init LaunchedEffect: FAILED - agoraManager.isInitialized() is false AFTER init call. MainActivity's isAgoraEngineInitialized set to false.")
                        }
                    } catch (e: Exception) {
                        isAgoraEngineInitialized = false
                        Log.e("MainActivityDebug", "Agora Init LaunchedEffect: FAILED WITH EXCEPTION during agoraManager.init(): ${e.message}", e)
                    }
                    Log.d("MainActivityDebug", "--- Agora Init LaunchedEffect: Exiting. MainActivity's isAgoraEngineInitialized is now: $isAgoraEngineInitialized ---")
                }

                val audioRecorderHelper = remember { AudioRecorderHelper(applicationContext) }
                val s3Uploader = remember { S3Uploader(applicationContext, awsAccessKey, awsSecretKey, s3BucketName, s3Region) }
                // LaunchedEffect for S3Uploader init can be added back if needed, removed for brevity for now
                // LaunchedEffect(key1 = s3Uploader) { try { s3Uploader.initS3Client() } catch (e: Exception) { Log.e("MainActivity", "S3Uploader init failed: ${e.message}") } }

                val amazonTranscribeHelper = remember { AmazonTranscribeHelper(awsAccessKey, awsSecretKey, s3BucketName, s3Region) }
                val pollyHelper = remember { PollyTTSHelper(context = applicationContext, accessKey = awsAccessKey, secretKey = awsSecretKey) }

                val callScreenViewModelFactory = CallScreenViewModelFactory(applicationContext, audioRecorderHelper, s3Uploader, amazonTranscribeHelper, pollyHelper, agoraManager, RAPID_API_KEY_PLACEHOLDER)
                val callScreenViewModel: CallScreenViewModel = ViewModelProvider(this, callScreenViewModelFactory)[CallScreenViewModel::class.java]

                DisposableEffect(Unit) {
                    onDispose {
                        Log.d("MainActivityDebug", "MainActivity onDispose: Destroying AgoraManager.")
                        agoraManager.destroy()
                        pollyHelper.release()
                    }
                }

                // Demo Users definition (ensure User class is correctly imported from ui.theme)
                val demoUsers = listOf(
                    User("demouser1", "Demo User One", "01234567890", "https://randomuser.me/api/portraits/men/1.jpg"),
                    User("demouser2", "Demo User Two", "01234567891", null),
                    User("demouser3", "Demo User Three", "01234567892", "https://randomuser.me/api/portraits/women/2.jpg")
                )

                // Demo CallLogs definition (ensure CallLog and CallType are correctly imported from ui.theme)
                val demoCallLogsState = remember {
                    mutableStateListOf<CallLog>().apply {
                        val now = System.currentTimeMillis()
                        val callTypes = listOf(CallType.INCOMING, CallType.OUTGOING, CallType.MISSED)
                        val durations = listOf(120L, 30L, 0L, 240L, 90L) // In seconds

                        addAll(demoUsers.mapIndexed { index, user ->
                            val callType = callTypes[index % callTypes.size]
                            val isMissed = callType == CallType.MISSED
                            val duration = if (isMissed) 0L else durations[index % durations.size]
                            val callTimestamp = now - (index * 5 * 60 * 1000L) - (index * 15 * 1000L) // Vary timestamps

                            CallLog(
                                user = user,
                                message = when (callType) { // Simplified message
                                    CallType.INCOMING -> "Incoming Call"
                                    CallType.OUTGOING -> "Outgoing Call"
                                    CallType.MISSED -> "Missed Call"
                                },
                                callType = callType,
                                isMissed = isMissed,
                                formattedDateTime = android.text.format.DateFormat.format("dd MMM yyyy, h:mm a", callTimestamp).toString(),
                                timestamp = callTimestamp,
                                duration = duration
                            )
                        })
                    }
                }

                // Permissions Management
                var hasReadCallLogPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) }
                var hasReadContactsPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }

                val requestReadCallLogLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasReadCallLogPermission = isGranted
                        if (isGranted) {
                            callLogRefreshTrigger++ // Trigger refresh if permission granted
                        } else {
                            Toast.makeText(applicationContext, "Read Call Log permission denied. Call history may be incomplete.", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                val requestReadContactsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasReadContactsPermission = isGranted
                        if (isGranted) {
                            callLogRefreshTrigger++ // Trigger refresh to fetch contact details
                        } else {
                            Toast.makeText(applicationContext, "Read Contacts permission denied. Contact photos will not be shown.", Toast.LENGTH_LONG).show()
                        }
                    }
                )

                LaunchedEffect(Unit) { // Request permissions on initial launch if not granted
                    if (!hasReadCallLogPermission) {
                        requestReadCallLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                    if (!hasReadContactsPermission) {
                        // Optionally, delay this or tie it to a specific user action if preferred
                        // For now, request upfront along with call log permission
                        requestReadContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }

                var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) }
                var callLogRefreshTrigger by remember { mutableIntStateOf(0) }

                // Content Observer for Call Log changes
                val callLogObserver = remember {
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            super.onChange(selfChange)
                            Log.d("MainActivity", "CallLog ContentObserver onChange triggered. Refreshing call logs.")
                            callLogRefreshTrigger++ // Increment to trigger LaunchedEffect
                        }
                    }
                }

                DisposableEffect(Unit) {
                    Log.d("MainActivity", "Registering CallLog ContentObserver.")
                    applicationContext.contentResolver.registerContentObserver(
                        AndroidCallLogHost.Calls.CONTENT_URI,
                        true,
                        callLogObserver
                    )
                    onDispose {
                        Log.d("MainActivity", "Unregistering CallLog ContentObserver.")
                        applicationContext.contentResolver.unregisterContentObserver(callLogObserver)
                    }
                }

                LaunchedEffect(hasReadCallLogPermission, hasReadContactsPermission, callLogRefreshTrigger) {
                    Log.d("MainActivity", "Refreshing call logs. Trigger: $callLogRefreshTrigger, ReadCallLog: $hasReadCallLogPermission, ReadContacts: $hasReadContactsPermission")
                    if (hasReadCallLogPermission) { // READ_CALL_LOG is essential to get any logs
                        // getRealCallLogs now internally checks for READ_CONTACTS for fetching photo URIs
                        callLogsFromSource = getRealCallLogs(applicationContext)
                        Log.d("MainActivity", "Call logs fetched. Count: ${callLogsFromSource.size}")
                    } else {
                        callLogsFromSource = demoCallLogsState // Use demo logs if no READ_CALL_LOG permission
                        Log.d("MainActivity", "No READ_CALL_LOG permission, using demo logs. Count: ${callLogsFromSource.size}")
                    }
                }

                var userToLog by remember { mutableStateOf<User?>(null) } // This state will hold the user for the call screen
                var profileScreenImageDisplayData by remember { mutableStateOf<Any?>(null) }


                val callLogsToDisplay by remember(callLogsFromSource, demoCallLogsState) {
                    derivedStateOf {
                        Log.d("MainActivity", "Re-deriving callLogsToDisplay. Source size: ${callLogsFromSource.size}, Demo size: ${demoCallLogsState.size}")
                        if (callLogsFromSource.isNotEmpty()) callLogsFromSource else demoCallLogsState
                    }
                }
                val usersToDisplay by remember(callLogsFromSource, demoUsers) { // Ensure usersToDisplay also updates if callLogsFromSource changes
                    derivedStateOf {
                        if (callLogsFromSource.isNotEmpty()) callLogsFromSource.map { it.user }.distinctBy { it.id } else demoUsers
                    }
                }

                NavHost(navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(onGetStarted = { navController.navigate("login") }) }
                    composable("login") { LoginScreen(onLogin = { navController.navigate("callhistory") }, onFacebook = {}, onGoogle = {}, onSignUp = {}) }

                    composable("callhistory") {
                        CallHistoryScreen(
                            callLogs = callLogsToDisplay,
                            onProfile = { user ->
                                userToLog = user // Set userToLog for profile navigation
                                profileScreenImageDisplayData = user.profilePicUrl // Prepare image data for profile
                                navController.navigate("profile/${user.id}")
                            },
                            onCall = { user ->
                                if (isAgoraEngineInitialized) {
                                    userToLog = user // Set userToLog for the call
                                    Log.d("MainActivity", "CallHistoryScreen: Navigating to call screen for user ${user.name}")
                                    navController.navigate("call/${user.phone}")
                                } else {
                                    Toast.makeText(applicationContext, "Call service not ready. Please wait.", Toast.LENGTH_SHORT).show()
                                    Log.w("MainActivity", "Call attempt from CallHistoryScreen failed: Agora engine not initialized.")
                                }
                            },
                            onUserAvatar = { user ->
                                userToLog = user
                                profileScreenImageDisplayData = user.profilePicUrl
                                navController.navigate("profile/${user.id}")
                            },
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
                            onCallContact = { phoneNumber ->
                                if (isAgoraEngineInitialized) {
                                    val foundUser = usersToDisplay.find { it.phone == phoneNumber }
                                    userToLog = foundUser ?: User(id = phoneNumber, name = phoneNumber, phone = phoneNumber, profilePicUrl = null)
                                    Log.d("MainActivity", "ContactsScreen: Navigating to call screen for number $phoneNumber, User: ${userToLog?.name}")
                                    navController.navigate("call/$phoneNumber")
                                } else {
                                    Toast.makeText(applicationContext, "Call service not ready. Please wait.", Toast.LENGTH_SHORT).show()
                                    Log.w("MainActivity", "Call attempt from ContactsScreen failed: Agora engine not initialized.")
                                }
                            },
                            mainRed = mainRed, accentRed = accentRed, mainWhite = mainWhite, mainGreen = mainGreen, lightGreen = lightGreen, lightRed = lightRed
                        )
                    }
                    composable("profile/{userId}", arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
                        val currentUserId = backStackEntry.arguments?.getString("userId")
                        // Prioritize userToLog if it's already set for this profile, otherwise find from usersToDisplay
                        val userForProfile = if (userToLog?.id == currentUserId) userToLog else usersToDisplay.find { it.id == currentUserId }

                        if (userForProfile != null) {
                            // If userToLog was for a different user (e.g. a call was made), update it
                            if (userToLog?.id != userForProfile.id) {
                                userToLog = userForProfile
                            }
                            // Use profileScreenImageDisplayData if set (e.g. by image picker), otherwise fallback to user's URL
                            val currentImageDataSource = profileScreenImageDisplayData ?: userForProfile.profilePicUrl

                            ProfileScreen(
                                user = userForProfile,
                                callLogs = callLogsToDisplay.filter { it.user.id == userForProfile.id },
                                imageDataSource = currentImageDataSource, // Pass this to ProfileScreen
                                onNameUpdate = { newName ->
                                    // Update logic here
                                    callLogsFromSource = callLogsFromSource.map { log ->
                                        if (log.user.id == currentUserId) log.copy(user = log.user.copy(name = newName)) else log
                                    }
                                    if (userToLog?.id == currentUserId) userToLog = userToLog?.copy(name = newName)
                                },
                                onProfilePicUriSelected = { uriString ->
                                    profileScreenImageDisplayData = uriString // Update the specific state for image picker result
                                    if (userToLog?.id == currentUserId) { // If this is the currently active user profile
                                        userToLog = userToLog?.copy(profilePicUrl = uriString)
                                    }
                                    // Note: This doesn't update callLogsFromSource with the new URI,
                                    // so User objects in that list might have old URIs. This is a deeper state management issue.
                                },
                                onBack = {
                                    profileScreenImageDisplayData = null // Clear picker image data when leaving profile
                                    navController.popBackStack()
                                },
                                onCall = { userToCall ->
                                    if (isAgoraEngineInitialized) {
                                        userToLog = userToCall // Set userToLog for the call
                                        Log.d("MainActivity", "ProfileScreen: Navigating to call screen for user ${userToCall.name}")
                                        navController.navigate("call/${userToCall.phone}")
                                    } else {
                                        Toast.makeText(applicationContext, "Call service not ready. Please wait.", Toast.LENGTH_SHORT).show()
                                        Log.w("MainActivity", "Call attempt from ProfileScreen failed: Agora engine not initialized.")
                                    }
                                },
                                mainRed = mainRed, mainWhite = mainWhite
                            )
                        } else {
                            Text("User not found for ID: $currentUserId")
                        }
                    }
                    composable("dialer") {
                        DialerScreen(
                            onClose = { navController.popBackStack() },
                            mainRed = mainRed, mainWhite = mainWhite,
                            onNavigateToCall = { number ->
                                if (isAgoraEngineInitialized) {
                                    userToLog = usersToDisplay.find { it.phone == number }
                                        ?: User(id = number, name = number, phone = number, profilePicUrl = null)
                                    Log.d("MainActivity", "DialerScreen: Navigating to call screen for number $number, User: ${userToLog?.name}")
                                    navController.navigate("call/$number")
                                } else {
                                    Toast.makeText(applicationContext, "Call service not ready. Please wait.", Toast.LENGTH_SHORT).show()
                                    Log.w("MainActivity", "Call attempt from DialerScreen failed: Agora engine not initialized.")
                                }
                            }
                        )
                    }
                    composable(
                        route = "call/{number}",
                        arguments = listOf(navArgument("number") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val numberFromNav = backStackEntry.arguments?.getString("number") ?: ""

                        var finalUserForCallScreen = userToLog // Start with current userToLog

                        // If userToLog is not set, or doesn't match the numberFromNav, try to find/create the correct user
                        if (finalUserForCallScreen == null || finalUserForCallScreen.phone != numberFromNav) {
                            val foundUser = usersToDisplay.find { it.phone == numberFromNav }
                            finalUserForCallScreen = foundUser ?: User(id = numberFromNav, name = numberFromNav, phone = numberFromNav, profilePicUrl = null)
                            userToLog = finalUserForCallScreen // Update the shared userToLog state
                        }

                        // Attempt to fetch contact details if profilePicUrl is still null for the user intended for CallScreen
                        // This ensures that even if userToLog was already set (e.g. from a previous navigation),
                        // we try to get details if it's missing a picture.
                        if (finalUserForCallScreen != null && finalUserForCallScreen.profilePicUrl == null) {
                            Log.d("PicDebugMainActivity", "User ${finalUserForCallScreen.name} has no pic URI, attempting to fetch from contacts.")
                            val contactDetails = getContactDetailsByNumber(applicationContext, finalUserForCallScreen.phone)
                            if (contactDetails != null) {
                                Log.d("PicDebugMainActivity", "Found contact details: Name: ${contactDetails.name}, Photo URI: ${contactDetails.photoUri}")
                                finalUserForCallScreen = finalUserForCallScreen.copy(
                                    name = contactDetails.name, // Update name if different/better
                                    profilePicUrl = contactDetails.photoUri // This will be the content URI
                                )
                                userToLog = finalUserForCallScreen // Update the shared userToLog state again if changed
                            } else {
                                Log.d("PicDebugMainActivity", "No contact details found for ${finalUserForCallScreen.phone}")
                            }
                        }

                        Log.d("PicDebugMainActivity", "--- Preparing to navigate to CallScreen ---")
                        Log.d("PicDebugMainActivity", "Number from Nav: $numberFromNav")
                        Log.d("PicDebugMainActivity", "User being passed to CallScreen: Name: ${finalUserForCallScreen?.name}, Phone: ${finalUserForCallScreen?.phone}, ProfilePicUrl: ${finalUserForCallScreen?.profilePicUrl}")

                        if (finalUserForCallScreen != null) {
                            CallScreen(
                                channel = finalUserForCallScreen.phone,
                                token = null,
                                appId = agoraAppId,
                                localIsUsa = true,
                                onCallEnd = {
                                    navController.popBackStack()
                                    callLogRefreshTrigger++
                                },
                                mainRed = mainRed,
                                mainWhite = mainWhite,
                                callScreenViewModel = callScreenViewModel,
                                user = finalUserForCallScreen // Pass the final, potentially enriched user
                            )
                        } else {
                            Text("Error: User for call not found.")
                        }
                    }
                }
            }
        }
    }
}
