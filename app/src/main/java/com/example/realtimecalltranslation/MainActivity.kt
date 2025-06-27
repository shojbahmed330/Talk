package com.example.realtimecalltranslation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri // Added import for Uri
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog as AndroidCallLogHost
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
// import androidx.core.net.toUri // Not strictly needed in this restored version for call log display
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.realtimecalltranslation.agora.AgoraManager
import com.example.realtimecalltranslation.ui.theme.CallScreen
import com.example.realtimecalltranslation.ui.CallScreenViewModel
import com.example.realtimecalltranslation.ui.CallScreenViewModelFactory
// import com.example.RealTimeCallTranslation.ui.LoginScreen // Will import from ui.theme
import com.example.realtimecalltranslation.ui.ProfileScreen // Correct import from ui package
import com.example.realtimecalltranslation.ui.WelcomeScreen
import com.example.realtimecalltranslation.ui.theme.* // Assuming getRealCallLogs is here
import com.example.realtimecalltranslation.ui.theme.LoginScreen // Import LoginScreen from ui.theme
// import com.example.RealTimeCallTranslation.ui.theme.ProfileScreen // Remove this if ProfileScreen is in ui package
import com.example.realtimecalltranslation.util.AudioRecorderHelper
import kotlinx.coroutines.Dispatchers // Added back
import kotlinx.coroutines.withContext // Added back
import androidx.compose.material3.Text
import com.example.realtimecalltranslation.util.Constants // Added import for Constants
import com.example.realtimecalltranslation.firebase.UserStatusManager
import com.example.realtimecalltranslation.firebase.CallSignalingManager
import com.example.realtimecalltranslation.firebase.CallRequest
import com.example.realtimecalltranslation.firebase.FirebaseProvider // Added FirebaseProvider import
import com.example.realtimecalltranslation.ui.theme.User // Import User from ui.theme
import com.example.realtimecalltranslation.ui.theme.CallLog // Import CallLog
// import com.example.realtimecalltranslation.ui.theme.CallType // Import CallType - Removed as unused in MainActivity
import com.example.realtimecalltranslation.ui.theme.FavouritesRepository
import com.example.realtimecalltranslation.ui.theme.getRealCallLogs // Import getRealCallLogs
// import com.example.realtimecalltranslation.util.ChannelUtils // Unused import
import com.example.realtimecalltranslation.util.ImageStorageHelper // Import ImageStorageHelper
import com.example.realtimecalltranslation.util.RingtonePlayer // Re-adding as it's used for variable type
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth

// Removed unused RAPID_API_KEY_PLACEHOLDER constant
// const val RAPID_API_KEY_PLACEHOLDER = "YOUR_RAPID_API_KEY"

// Helper function moved directly into MainActivity.kt to avoid resolution issues
private fun findUserInList(users: List<User>, idToMatch: String, phoneToMatch: String): User? {
    return users.find { user -> user.id == idToMatch || user.phone == phoneToMatch }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Log.d("MainActivityDebug", "--- MainActivity setContent: Entered ---")
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                // --- Permissions Handling ---
                val applicationContextForPermissions = applicationContext // Capture for use in remember block
                val permissionsToRequest = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE, // Often needed with RECORD_AUDIO for call state
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.READ_CONTACTS
                    // Add other permissions your app needs, e.g., Manifest.permission.CAMERA if video calling
                )

                var permissionsGranted by remember {
                    mutableStateOf(
                        permissionsToRequest.all {
                            ContextCompat.checkSelfPermission(applicationContextForPermissions, it) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissionsMap ->
                        permissionsGranted = permissionsMap.values.all { it }
                        if (!permissionsGranted) {
                            Toast.makeText(applicationContextForPermissions, "Some permissions were denied. Features may be limited.", Toast.LENGTH_LONG).show()
                            permissionsMap.forEach { (permission, isGranted) ->
                                Log.d("Permissions", "$permission granted: $isGranted")
                            }
                        } else {
                            Toast.makeText(applicationContextForPermissions, "All permissions granted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        Log.d("Permissions", "Requesting permissions as not all are granted initially.")
                        multiplePermissionsLauncher.launch(permissionsToRequest)
                    } else {
                        Log.d("Permissions", "All permissions already granted.")
                    }
                }
                // --- End Permissions Handling ---

                // --- Placeholder for Current User ---
                // In a real app, this would come from your authentication system (e.g., Firebase Auth)
                // For now, using a hardcoded placeholder. Replace with actual user ID.
                // Ensure this ID is consistent with how users are identified in your User objects (e.g., phone number)
                val currentUserId = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                // TODO: Replace with actual logged-in user ID
                val currentUserName = "My Name" // TODO: Replace with actual logged-in user name

                // Instantiate Managers
                val userStatusManager = remember { UserStatusManager() }
                val callSignalingManager = remember { CallSignalingManager() }
                val ringtonePlayer = remember { RingtonePlayer(applicationContext) }

                // Initialize UserStatusManager for the current user and set up lifecycle observers
                LaunchedEffect(currentUserId) {
                    if (currentUserId.isNotBlank()) {
                        userStatusManager.setCurrentUser(currentUserId)
                        userStatusManager.updateUserOnlineStatus(isOnline = true) // TODO: Pass FCM token if available
                        userStatusManager.setUserOfflineOnDisconnect()

                        // Start listening for incoming calls
                        callSignalingManager.listenForIncomingCalls(
                            myUserId = currentUserId,
                            onIncomingCall = { callRequest ->
                                Log.d("MainActivity", "Incoming call detected: From ${callRequest.callerId} (Name: ${callRequest.callerName}) for channel ${callRequest.channelName}, CallID: ${callRequest.callId}")
                                coroutineScope.launch(Dispatchers.Main) {
                                    ringtonePlayer.startRingtone()
                                    val callerUser: User? = findUserInList(usersToDisplay.value, callRequest.callerId, callRequest.callerId)
                                    val callerProfilePicUrl = callerUser?.profilePicUrl?.let { picUrl -> Uri.encode(picUrl) } // Encode URL
                                    // Pass localIsUsa=false for incoming calls to this user (assumed non-USA based on current setup)
                                    var route = "incoming_call_screen/${callRequest.callerId}/${callRequest.callerName ?: callRequest.callerId}/${callRequest.channelName}/${callRequest.callId}/false"
                                    if (callerProfilePicUrl != null) {
                                        route += "?callerProfilePicUrl=$callerProfilePicUrl"
                                    }
                                    navController.navigate(route)
                                }
                            },
                            onCallRequestUpdated = { callRequest ->
                                Log.d("MainActivity", "Call request updated: ${callRequest.callId}, New Status: ${callRequest.status}")
                                // If call is no longer pending (e.g. accepted elsewhere, rejected, cancelled, ended)
                                if (callRequest.status != Constants.CALL_STATUS_PENDING) {
                                    ringtonePlayer.stopRingtone()
                                    if (navController.currentBackStackEntry?.destination?.route?.startsWith("incoming_call_screen") == true &&
                                        navController.currentBackStackEntry?.arguments?.getString("callId") == callRequest.callId) {
                                        navController.popBackStack()
                                        Log.d("MainActivity", "Dismissed incoming call UI as call request ${callRequest.callId} is no longer pending.")
                                    }
                                }
                            },
                            onCallRequestRemoved = { callRequestId ->
                                Log.d("MainActivity", "Call request removed: $callRequestId")
                                ringtonePlayer.stopRingtone() // Stop ringtone if the request is removed
                                if (navController.currentBackStackEntry?.destination?.route?.startsWith("incoming_call_screen") == true &&
                                    navController.currentBackStackEntry?.arguments?.getString("callId") == callRequestId) {
                                    navController.popBackStack()
                                    Log.d("MainActivity", "Dismissed incoming call UI as call request $callRequestId was removed.")
                                }
                            }
                        )
                    }
                }

                // val agoraAppId = "7b2d5eaf4312454dbc61d86f0361a5d2" // Replaced by Constants

// Removed misplaced import com.example.RealTimeCallTranslation.util.Constants from here

                // Initialize AgoraManager Singleton
                LaunchedEffect(key1 = Unit) { // Initialize once
                    AgoraManager.initialize(applicationContext, Constants.AGORA_APP_ID)
                }

                var isAgoraEngineInitialized by remember { mutableStateOf(AgoraManager.isInitialized()) }

                // LaunchedEffect to check Agora Engine initialization status, depends on permissionsGranted
                LaunchedEffect(key1 = permissionsGranted) {
                    val hasRecordAudioPerm = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (permissionsGranted && hasRecordAudioPerm) {
                        if (!AgoraManager.isInitialized()) {
                            try {
                                Log.d("MainActivityDebug", "Attempting Agora Init as permissions are granted.")
                                // AgoraManager.initialize might be called again if it failed or context changed,
                                // but primary initialization is above. Here we just re-check.
                                // If it needs re-init due to permissions, it should be handled inside AgoraManager or by re-calling initialize.
                                // For now, we assume initial initialize call is sufficient if permissions are later granted.
                                // The crucial part is that AgoraManager needs the context and app ID.
                                AgoraManager.initialize(applicationContext, Constants.AGORA_APP_ID) // Ensure it's initialized
                                isAgoraEngineInitialized = AgoraManager.isInitialized()
                                Log.d("MainActivityDebug", "Agora Init check complete. Initialized: $isAgoraEngineInitialized")
                            } catch (e: Exception) {
                                isAgoraEngineInitialized = false
                                Log.e("MainActivityDebug", "Agora Init Failed during permission check: ${e.message}", e)
                            }
                        } else {
                            isAgoraEngineInitialized = true
                            Log.d("MainActivityDebug", "Agora already initialized and permissions granted.")
                        }
                    } else {
                        isAgoraEngineInitialized = false
                        if (!hasRecordAudioPerm) {
                            Log.w("MainActivityDebug", "Agora potentially not operational: RECORD_AUDIO permission not granted.")
                        } else {
                            Log.w("MainActivityDebug", "Agora potentially not operational: Not all required permissions are granted.")
                        }
                        // Consider if AgoraManager.destroy() should be called if permissions are revoked.
                        // This can be complex due to app lifecycle.
                    }
                }

                val audioRecorderHelper = remember { AudioRecorderHelper(applicationContext) }

                val callScreenViewModelFactory = CallScreenViewModelFactory(applicationContext, audioRecorderHelper, callSignalingManager)

                val callScreenViewModel: CallScreenViewModel = ViewModelProvider(this, callScreenViewModelFactory)[CallScreenViewModel::class.java]

                // Instantiate FavouritesRepository
                val favouritesRepository = remember { FavouritesRepository(applicationContext) }


                // Set the audio frame listener on AgoraManager
                LaunchedEffect(callScreenViewModel) { // AgoraManager is now an object, no need to pass as key if it doesn't change
                    AgoraManager.setAudioFrameListener(callScreenViewModel)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        // AgoraManager.setAudioFrameListener(null) // Clear listener
                        // AgoraManager.destroy() // Singleton's lifecycle might be managed differently, e.g., by Application class or never explicitly destroyed if app-wide
                        // For now, let's not destroy it here as the Service might be using it.
                        // The service will manage its lifecycle with Agora.
                        userStatusManager.cleanup() // Cleanup server time offset listener
                        callSignalingManager.cleanupAllListeners() // Stop listening for calls
                        ringtonePlayer.release() // Release ringtone player
                        Log.d("MainActivity", "MainActivity onDispose. AgoraManager listener NOT cleared, AgoraEngine NOT destroyed here.")
                    }
                }

                // Duplicate permission handling block removed. The correct one is earlier in the code.

                // triggerRefresh function and callLogRefreshTrigger state are kept for ContentObserver
                var callLogRefreshTrigger by remember { mutableIntStateOf(0) }
                fun triggerRefresh() {
                    callLogRefreshTrigger++
                }


                var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) }

                val callLogObserver = remember {
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            super.onChange(selfChange)
                            Log.d("MainActivity", "CallLog ContentObserver onChange. Refreshing.")
                            // Check if READ_CALL_LOG is granted before triggering refresh
                            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                                triggerRefresh()
                            } else {
                                Log.d("MainActivity", "ContentObserver: READ_CALL_LOG not granted, not refreshing.")
                            }
                        }
                    }
                }

                DisposableEffect(Unit) {
                    // Only register observer if permission is granted
                    if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                        applicationContext.contentResolver.registerContentObserver(
                            AndroidCallLogHost.Calls.CONTENT_URI,
                            true,
                            callLogObserver
                        )
                        Log.d("MainActivity", "CallLog ContentObserver registered.")
                    }
                    onDispose {
                        applicationContext.contentResolver.unregisterContentObserver(callLogObserver)
                        Log.d("MainActivity", "CallLog ContentObserver unregistered.")
                    }
                }

                // LaunchedEffect to load call logs, depends on permissionsGranted and callLogRefreshTrigger
                LaunchedEffect(permissionsGranted, callLogRefreshTrigger) {
                    val hasReadCallLogPerm = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                    val hasReadContactsPerm = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

                    Log.d("MainActivity", "Refreshing call logs. PermissionsGranted: $permissionsGranted, ReadCallLogPerm: $hasReadCallLogPerm, ReadContactsPerm: $hasReadContactsPerm, Trigger: $callLogRefreshTrigger")

                    if (permissionsGranted && hasReadCallLogPerm) { // Ensure READ_CALL_LOG is specifically granted
                        val logs = withContext(Dispatchers.IO) {
                            // getRealCallLogs uses READ_CONTACTS internally if available (checked by hasReadContactsPerm implicitly by the function)
                            getRealCallLogs(applicationContext)
                        }
                        callLogsFromSource = logs
                        Log.d("MainActivity", "Call logs fetched. Count: ${logs.size}")
                    } else {
                        callLogsFromSource = emptyList() // Fallback to empty list if permissions are not sufficient
                        Log.d("MainActivity", "Not enough permissions or permission denied, using empty logs. PermissionsGranted: $permissionsGranted, HasReadCallLog: $hasReadCallLogPerm")
                    }
                }

                var userToLog by remember { mutableStateOf<User?>(null) }
                var profileScreenImageDisplayData by remember { mutableStateOf<Any?>(null) }

                val callLogsToDisplay by remember(callLogsFromSource) {
                    derivedStateOf {
                        callLogsFromSource
                    }
                }
                val usersToDisplay by remember(callLogsFromSource) {
                    derivedStateOf {
                        // If callLogsFromSource is empty, this will correctly produce an empty list of users.
                        callLogsFromSource.map { it.user }.distinctBy { it.id }
                    }
                }
                // Attempt to simplify access for debugging persistent errors
                // val currentUsers: List<User> = usersToDisplay.value // Removed this intermediate variable


                // Assuming CallLog and CallType are now imported from ui.theme or a similar package
                // If not, their definitions need to be present or imported.
                // For now, I'll assume they are available in the scope.

                val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
                    "callhistory"
                } else {
                    "welcome"
                }

                NavHost(navController, startDestination = startDestination) {
                    composable("welcome") {
                        WelcomeScreen(onGetStarted = { navController.navigate("login") })
                    }
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("callhistory") {
                                    popUpTo("welcome") { inclusive = true } // Clear back stack
                                }
                            }
                        )
                    }
                    composable(
                        "incoming_call_screen/{callerId}/{callerName}/{channelName}/{callId}/{localIsUsa}?callerProfilePicUrl={callerProfilePicUrl}",
                        arguments = listOf(
                            navArgument("callerId") { type = NavType.StringType },
                            navArgument("callerName") { type = NavType.StringType; nullable = true },
                            navArgument("channelName") { type = NavType.StringType },
                            navArgument("callId") { type = NavType.StringType },
                            navArgument("localIsUsa") { type = NavType.BoolType; defaultValue = false },
                            navArgument("callerProfilePicUrl") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { backStackEntry ->
                        val callerId = backStackEntry.arguments?.getString("callerId") ?: "Unknown"
                        val callerName = backStackEntry.arguments?.getString("callerName") ?: callerId
                        val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                        val callId = backStackEntry.arguments?.getString("callId") ?: ""
                        val localIsUsaFromNav = backStackEntry.arguments?.getBoolean("localIsUsa") ?: false
                        val callerProfilePicUrlFromNav = backStackEntry.arguments?.getString("callerProfilePicUrl")?.let { url -> Uri.decode(url) }

                        val foundCalleeUser: User? = findUserInList(usersToDisplay.value, callerId, callerId)
                        val calleeUser = foundCalleeUser ?: User(callerId, callerName, callerId, callerProfilePicUrlFromNav)
                        // Set userToLog for context if needed elsewhere, though IncomingCallScreen primarily uses its args
                        userToLog = calleeUser

                        IncomingCallScreen(
                            callerId = callerId,
                            callerName = callerName,
                            callerProfilePicUrl = callerProfilePicUrlFromNav, // Pass the decoded URL
                            channelName = channelName,
                            callId = callId,
                            callScreenViewModel = callScreenViewModel,
                            localIsUsa = localIsUsaFromNav, // Pass the new parameter
                            onAcceptCall = { _channel, _callId, _remoteUserId, _callerName ->
                                ringtonePlayer.stopRingtone()
                                // The primary responsibility of joining the call and changing UI state
                                // is now within IncomingCallScreen.kt itself via its own onClick for accept.
                                // This callback can be used for any additional actions MainActivity needs to take upon acceptance,
                                // like logging or specific Firebase state updates if not handled by ViewModel.
                                Log.d("MainActivity", "Call with ID: $_callId has been accepted by the user in IncomingCallScreen.")
                                // No navigation needed here as IncomingCallScreen handles the UI transition.
                                // userToLog = calleeUser // This is still important if IncomingCallScreen relies on it for User object.
                                // Ensure calleeUser (which is the caller in incoming context) is correctly scoped and available.
                                // Or, ensure IncomingCallScreen constructs its User object from passed IDs.
                                // In IncomingCallScreen, we are already creating 'callerUser' from passed parameters.
                                // So, setting userToLog here might only be relevant if navigating to a different CallScreen instance,
                                // which we are no longer doing.
                                // Let's verify if userToLog is used by IncomingCallScreen's ActiveCallContent.
                                // ActiveCallContent takes a `user: User?` parameter.
                                // IncomingCallScreen creates `callerUser` and passes it to ActiveCallContent.
                                // So, direct update of `userToLog` from here for `IncomingCallScreen` is not strictly necessary
                                // as long as `IncomingCallScreen` correctly passes the `callerUser` to `ActiveCallContent`.
                                // However, if `CallScreen.kt` (for outgoing calls) still uses `userToLog`, keep it.
                                // For incoming calls, `calleeUser` (which is the caller) is passed to IncomingCallScreen's route arguments.
                                // So, `userToLog = calleeUser` is correct for context.
                                userToLog = calleeUser // Set for the context, though IncomingCallScreen should use its args.
                            },
                            onRejectCall = {
                                ringtonePlayer.stopRingtone()
                                callSignalingManager.removeCallRequest(currentUserId, callId)
                                navController.popBackStack()
                            },
                            onEndCall = { // This will be used by IncomingCallScreen when call is active and user ends it
                                ringtonePlayer.stopRingtone() // Ensure ringtone is stopped if somehow active
                                callSignalingManager.removeCallRequest(currentUserId, callId) // Or update status to ended
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("callhistory") {
                        // Assuming CallHistoryScreen is in ui.theme package
                        CallHistoryScreen(
                            callLogs = callLogsToDisplay,
                            onProfile = { user ->
                                userToLog = user
                                profileScreenImageDisplayData = user.profilePicUrl // Will be null here
                                navController.navigate("profile/${user.id}")
                            },
                            onCall = { calleeUser ->
                                Log.d("CallDebug", "Initiating call from CallHistoryScreen to: ${calleeUser.phone}")
                                Log.d("CallDebug", "Agora Engine Initialized: $isAgoraEngineInitialized")
                                Log.d("CallDebug", "Current User ID: $currentUserId, Name: $currentUserName")

                                if (!isAgoraEngineInitialized) {
                                    Toast.makeText(applicationContext, "Call service not ready.", Toast.LENGTH_SHORT).show()
                                    Log.w("CallDebug", "Call attempt failed: Agora engine not initialized.")
                                    return@CallHistoryScreen
                                }
                                if (currentUserId.isBlank()) {
                                    Toast.makeText(applicationContext, "Cannot make call: Current user not identified.", Toast.LENGTH_LONG).show()
                                    Log.w("CallDebug", "Call attempt failed: CurrentUser ID is blank.")
                                    return@CallHistoryScreen
                                }

                                userToLog = calleeUser // Keep track of user for CallScreen
                                val calleePhoneNumber = calleeUser.phone
                                Log.d("CallDebug", "Callee: ${calleeUser.name}, Phone: $calleePhoneNumber")

                                coroutineScope.launch {
                                    var statusListener: ValueEventListener? = null
                                    Log.d("CallDebug", "Observing online status for $calleePhoneNumber")
                                    statusListener = userStatusManager.observeUserOnlineStatus(calleePhoneNumber) { isOnline: Boolean, lastSeen: Long? ->
                                        Log.d("CallDebug", "Online status for $calleePhoneNumber: isOnline=$isOnline, lastSeen=$lastSeen")
                                        statusListener?.let { userStatusManager.stopObservingUserStatus(calleePhoneNumber, it) }
                                        statusListener = null

                                        if (isOnline) {
                                            val channelToJoin = calleePhoneNumber // Using callee's number as channel for now
                                            val callId = FirebaseProvider.getCallRequestsRef(calleePhoneNumber).push().key ?: UUID.randomUUID().toString()
                                            val callRequest = CallRequest(
                                                callId = callId,
                                                callerId = currentUserId,
                                                callerName = currentUserName,
                                                calleeId = calleePhoneNumber,
                                                channelName = channelToJoin,
                                                status = Constants.CALL_STATUS_PENDING
                                            )
                                            Log.d("CallDebug", "CallRequest created: $callRequest")
                                            Log.d("CallDebug", "Sending call request to $calleePhoneNumber via CallSignalingManager")
                                            callSignalingManager.sendCallRequest(callRequest,
                                                onSuccess = {
                                                    Log.i("CallDebug", "Call request sent successfully to $calleePhoneNumber. Navigating to CallScreen.")
                                                    // Pass localIsUsa=false for outgoing calls initiated by this user (assumed non-USA based on current setup)
                                                    navController.navigate("call/${calleePhoneNumber}?callId=${callRequest.callId}&remoteUserId=${callRequest.calleeId}&localIsUsa=false")
                                                },
                                                onFailure = { errorMsg ->
                                                    Log.e("CallDebug", "Failed to send call request to $calleePhoneNumber: $errorMsg")
                                                    Toast.makeText(applicationContext, "Failed to send call request: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            Log.w("CallDebug", "Call attempt failed: User $calleePhoneNumber (${calleeUser.name}) is offline. Last seen: $lastSeen")
                                            Toast.makeText(applicationContext, "${calleeUser.name} is currently offline.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            onUserAvatar = { user -> // Same as onProfile for this version
                                userToLog = user
                                profileScreenImageDisplayData = user.profilePicUrl
                                navController.navigate("profile/${user.id}")
                            },
                            onFavourites = { navController.navigate("favourites") },
                            onDialer = { navController.navigate("dialer") },
                            onContacts = { navController.navigate("contacts") },
                            selectedNav = 0, // Default selected nav item
                            mainRed = mainRed, mainWhite = mainWhite, accentRed = accentRed, lightRed = lightRed
                        )
                    }
                    composable("favourites") {
                        FavouritesScreen(
                            onBack = { navController.popBackStack() },
                            favouritesRepository = favouritesRepository,
                            onCall = { calleePhoneNumber ->
                                // Logic to initiate a call, similar to other call initiation points
                                Log.d("CallDebug", "Initiating call from FavouritesScreen to phone: $calleePhoneNumber")
                                if (!isAgoraEngineInitialized) {
                                    Toast.makeText(applicationContext, "Call service not ready.", Toast.LENGTH_SHORT).show()
                                    return@FavouritesScreen
                                }
                                if (currentUserId.isBlank()) {
                                    Toast.makeText(applicationContext, "Cannot make call: Current user not identified.", Toast.LENGTH_LONG).show()
                                    return@FavouritesScreen
                                }

                                val calleeUser = usersToDisplay.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLog = calleeUser

                                coroutineScope.launch {
                                    var statusListener: ValueEventListener? = null
                                    statusListener = userStatusManager.observeUserOnlineStatus(calleePhoneNumber) { isOnline: Boolean, lastSeen: Long? ->
                                        statusListener?.let { userStatusManager.stopObservingUserStatus(calleePhoneNumber, it) }
                                        statusListener = null
                                        if (isOnline) {
                                            val channelToJoin = calleePhoneNumber
                                            val callId = FirebaseProvider.getCallRequestsRef(calleePhoneNumber).push().key ?: UUID.randomUUID().toString()
                                            val callRequest = CallRequest(
                                                callId = callId,
                                                callerId = currentUserId,
                                                callerName = currentUserName,
                                                calleeId = calleePhoneNumber,
                                                channelName = channelToJoin,
                                                status = Constants.CALL_STATUS_PENDING
                                            )
                                            callSignalingManager.sendCallRequest(callRequest,
                                                onSuccess = {
                                                    navController.navigate("call/${calleePhoneNumber}?callId=${callRequest.callId}&remoteUserId=${callRequest.calleeId}&localIsUsa=false")
                                                },
                                                onFailure = { errorMsg ->
                                                    Toast.makeText(applicationContext, "Failed to send call request: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            Toast.makeText(applicationContext, "${calleeUser.name} is currently offline.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            mainRed = mainRed,
                            mainWhite = mainWhite,
                            accentRed = accentRed,
                            lightRed = lightRed
                        )
                    }
                    composable("contacts") {
                        ContactsScreen(
                            onBack = { navController.popBackStack() },
                            onCallContact = { calleePhoneNumber ->
                                Log.d("CallDebug", "Initiating call from ContactsScreen to phone: $calleePhoneNumber")
                                Log.d("CallDebug", "Agora Engine Initialized: $isAgoraEngineInitialized")
                                Log.d("CallDebug", "Current User ID: $currentUserId, Name: $currentUserName")

                                if (!isAgoraEngineInitialized) {
                                    Toast.makeText(applicationContext, "Call service not ready.", Toast.LENGTH_SHORT).show()
                                    Log.w("CallDebug", "Call attempt failed: Agora engine not initialized.")
                                    return@ContactsScreen
                                }
                                if (currentUserId.isBlank()) {
                                    Toast.makeText(applicationContext, "Cannot make call: Current user not identified.", Toast.LENGTH_LONG).show()
                                    Log.w("CallDebug", "Call attempt failed: CurrentUser ID is blank.")
                                    return@ContactsScreen
                                }

                                val calleeUser = usersToDisplay.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLog = calleeUser // Keep track of user
                                Log.d("CallDebug", "Callee: ${calleeUser.name}, Phone: $calleePhoneNumber")

                                coroutineScope.launch {
                                    var statusListener: ValueEventListener? = null
                                    Log.d("CallDebug", "Observing online status for $calleePhoneNumber")
                                    statusListener = userStatusManager.observeUserOnlineStatus(calleePhoneNumber) { isOnline: Boolean, lastSeen: Long? ->
                                        Log.d("CallDebug", "Online status for $calleePhoneNumber: isOnline=$isOnline, lastSeen=$lastSeen")
                                        statusListener?.let { userStatusManager.stopObservingUserStatus(calleePhoneNumber, it) }
                                        statusListener = null

                                        if (isOnline) {
                                            val channelToJoin = calleePhoneNumber
                                            val callId = FirebaseProvider.getCallRequestsRef(calleePhoneNumber).push().key ?: UUID.randomUUID().toString()
                                            val callRequest = CallRequest(
                                                callId = callId,
                                                callerId = currentUserId,
                                                callerName = currentUserName,
                                                calleeId = calleePhoneNumber,
                                                channelName = channelToJoin,
                                                status = Constants.CALL_STATUS_PENDING
                                            )
                                            Log.d("CallDebug", "CallRequest created: $callRequest")
                                            Log.d("CallDebug", "Sending call request to $calleePhoneNumber via CallSignalingManager")
                                            callSignalingManager.sendCallRequest(callRequest,
                                                onSuccess = {
                                                    Log.i("CallDebug", "Call request sent successfully to $calleePhoneNumber. Navigating to CallScreen.")
                                                    navController.navigate("call/${calleePhoneNumber}?callId=${callRequest.callId}&remoteUserId=${callRequest.calleeId}&localIsUsa=false")
                                                },
                                                onFailure = { errorMsg ->
                                                    Log.e("CallDebug", "Failed to send call request to $calleePhoneNumber: $errorMsg")
                                                    Toast.makeText(applicationContext, "Failed to send call request: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            Log.w("CallDebug", "Call attempt failed: User $calleePhoneNumber (${calleeUser.name}) is offline. Last seen: $lastSeen")
                                            Toast.makeText(applicationContext, "${calleeUser.name} is currently offline.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            mainRed = mainRed, accentRed = accentRed, mainWhite = mainWhite, mainGreen = mainGreen, lightGreen = lightGreen, lightRed = lightRed
                        )
                    }
                    composable("profile/{userId}", arguments = listOf(navArgument("userId") { type = NavType.StringType })) { backStackEntry ->
                        val navigatedUserId = backStackEntry.arguments?.getString("userId") // Renamed to avoid conflict
                        val userForProfile = if (userToLog?.id == navigatedUserId) userToLog else usersToDisplay.find { it.id == navigatedUserId }

                        if (userForProfile != null) {
                            if (userToLog?.id != userForProfile.id) userToLog = userForProfile
                            val currentImageDataSource = profileScreenImageDisplayData ?: userForProfile.profilePicUrl // Will be null
                            // ProfileScreen is now imported from com.example.realtimecalltranslation.ui.theme
                            ProfileScreen( // No need for full package name if imported correctly
                                user = userForProfile,
                                callLogs = callLogsToDisplay.filter { it.user.id == userForProfile.id }, // Use userForProfile.id
                                imageDataSource = currentImageDataSource,
                                onNameUpdate = { newName: String -> // Explicitly typed
                                    callLogsFromSource = callLogsFromSource.map { log ->
                                        if (log.user.id == navigatedUserId) log.copy(user = log.user.copy(name = newName)) else log
                                    }
                                    if (userToLog?.id == navigatedUserId) userToLog = userToLog?.copy(name = newName)
                                },
                                onProfilePicUriSelected = { uriString: String? ->
                                    if (uriString != null) {
                                        val newUri = android.net.Uri.parse(uriString)
                                        val savedImageUri = ImageStorageHelper.saveImageToInternalStorage(applicationContext, newUri)
                                        if (savedImageUri != null) {
                                            val savedImageUriString = savedImageUri.toString()
                                            profileScreenImageDisplayData = savedImageUriString // Update display data with local file URI

                                            // Update userToLog
                                            if (userToLog?.id == navigatedUserId) {
                                                // If there was an old picture, try to delete it
                                                ImageStorageHelper.deleteImageFromInternalStorage(userToLog?.profilePicUrl)
                                                userToLog = userToLog?.copy(profilePicUrl = savedImageUriString)
                                            }

                                            // Update callLogsFromSource to reflect the change for persistence within the session
                                            val updatedLogs = callLogsFromSource.map { log ->
                                                if (log.user.id == navigatedUserId) {
                                                    // Also attempt to delete old image if replacing for this user in logs
                                                    if (log.user.profilePicUrl != null && log.user.profilePicUrl != savedImageUriString) {
                                                        ImageStorageHelper.deleteImageFromInternalStorage(log.user.profilePicUrl)
                                                    }
                                                    log.copy(user = log.user.copy(profilePicUrl = savedImageUriString))
                                                } else {
                                                    log
                                                }
                                            }
                                            callLogsFromSource = updatedLogs

                                        } else {
                                            Toast.makeText(applicationContext, "Failed to save image.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // Handle case where uriString is null (e.g., user wants to remove picture)
                                        if (userToLog?.id == navigatedUserId) {
                                            ImageStorageHelper.deleteImageFromInternalStorage(userToLog?.profilePicUrl)
                                            userToLog = userToLog?.copy(profilePicUrl = null)
                                        }
                                        profileScreenImageDisplayData = null
                                        // Update callLogsFromSource to remove the picture
                                        val updatedLogs = callLogsFromSource.map { log ->
                                            if (log.user.id == navigatedUserId) {
                                                ImageStorageHelper.deleteImageFromInternalStorage(log.user.profilePicUrl)
                                                log.copy(user = log.user.copy(profilePicUrl = null))
                                            } else {
                                                log
                                            }
                                        }
                                        callLogsFromSource = updatedLogs
                                    }
                                },
                                onBack = {
                                    // profileScreenImageDisplayData = null // No longer strictly needed to nullify here as ProfileScreen will take from user.profilePicUrl
                                    navController.popBackStack()
                                },
                                onCall = { calleeUser: User ->
                                    Log.d("CallDebug", "Initiating call from ProfileScreen to: ${calleeUser.phone}")
                                    Log.d("CallDebug", "Agora Engine Initialized: $isAgoraEngineInitialized")
                                    Log.d("CallDebug", "Current User ID: $currentUserId, Name: $currentUserName")

                                    if (!isAgoraEngineInitialized) {
                                        Toast.makeText(applicationContext, "Call service not ready.", Toast.LENGTH_SHORT).show()
                                        Log.w("CallDebug", "Call attempt failed: Agora engine not initialized.")
                                        return@ProfileScreen
                                    }
                                    if (currentUserId.isBlank()) {
                                        Toast.makeText(applicationContext, "Cannot make call: Current user not identified.", Toast.LENGTH_LONG).show()
                                        Log.w("CallDebug", "Call attempt failed: CurrentUser ID is blank.")
                                        return@ProfileScreen
                                    }

                                    userToLog = calleeUser // Keep track of user
                                    val calleePhoneNumber = calleeUser.phone
                                    Log.d("CallDebug", "Callee: ${calleeUser.name}, Phone: $calleePhoneNumber")

                                    coroutineScope.launch {
                                        var statusListener: ValueEventListener? = null
                                        Log.d("CallDebug", "Observing online status for $calleePhoneNumber")
                                        statusListener = userStatusManager.observeUserOnlineStatus(calleePhoneNumber) { isOnline: Boolean, lastSeen: Long? ->
                                            Log.d("CallDebug", "Online status for $calleePhoneNumber: isOnline=$isOnline, lastSeen=$lastSeen")
                                            statusListener?.let { userStatusManager.stopObservingUserStatus(calleePhoneNumber, it) }
                                            statusListener = null

                                            if (isOnline) {
                                                val channelToJoin = calleePhoneNumber
                                                val callId = FirebaseProvider.getCallRequestsRef(calleePhoneNumber).push().key ?: UUID.randomUUID().toString()
                                                val callRequest = CallRequest(
                                                    callId = callId,
                                                    callerId = currentUserId,
                                                    callerName = currentUserName,
                                                    calleeId = calleePhoneNumber,
                                                    channelName = channelToJoin,
                                                    status = Constants.CALL_STATUS_PENDING
                                                )
                                                Log.d("CallDebug", "CallRequest created: $callRequest")
                                                Log.d("CallDebug", "Sending call request to $calleePhoneNumber via CallSignalingManager")
                                                callSignalingManager.sendCallRequest(callRequest,
                                                    onSuccess = {
                                                        Log.i("CallDebug", "Call request sent successfully to $calleePhoneNumber. Navigating to CallScreen.")
                                                        navController.navigate("call/${calleePhoneNumber}?callId=${callRequest.callId}&remoteUserId=${callRequest.calleeId}")
                                                    },
                                                    onFailure = { errorMsg ->
                                                        Log.e("CallDebug", "Failed to send call request to $calleePhoneNumber: $errorMsg")
                                                        Toast.makeText(applicationContext, "Failed to send call request: $errorMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } else {
                                                Log.w("CallDebug", "Call attempt failed: User $calleePhoneNumber (${calleeUser.name}) is offline. Last seen: $lastSeen")
                                                Toast.makeText(applicationContext, "${calleeUser.name} is currently offline.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                mainRed = mainRed, mainWhite = mainWhite
                            )
                        } else {
                            Text("User not found for ID: $navigatedUserId") // Use renamed variable
                        }
                    }
                    composable("dialer") {
                        // Assuming DialerScreen is in ui.theme package
                        DialerScreen(
                            onClose = { navController.popBackStack() },
                            mainRed = mainRed, mainWhite = mainWhite,
                            onNavigateToCall = { calleePhoneNumber ->
                                Log.d("CallDebug", "Initiating call from DialerScreen to phone: $calleePhoneNumber")
                                Log.d("CallDebug", "Agora Engine Initialized: $isAgoraEngineInitialized")
                                Log.d("CallDebug", "Current User ID: $currentUserId, Name: $currentUserName")

                                if (!isAgoraEngineInitialized) {
                                    Toast.makeText(applicationContext, "Call service not ready.", Toast.LENGTH_SHORT).show()
                                    Log.w("CallDebug", "Call attempt failed: Agora engine not initialized.")
                                    return@DialerScreen
                                }
                                if (currentUserId.isBlank()) {
                                    Toast.makeText(applicationContext, "Cannot make call: Current user not identified.", Toast.LENGTH_LONG).show()
                                    Log.w("CallDebug", "Call attempt failed: CurrentUser ID is blank.")
                                    return@DialerScreen
                                }

                                val calleeUser = usersToDisplay.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLog = calleeUser // Keep track of user
                                Log.d("CallDebug", "Callee: ${calleeUser.name}, Phone: $calleePhoneNumber")

                                coroutineScope.launch {
                                    var statusListener: ValueEventListener? = null
                                    Log.d("CallDebug", "Observing online status for $calleePhoneNumber")
                                    statusListener = userStatusManager.observeUserOnlineStatus(calleePhoneNumber) { isOnline: Boolean, lastSeen: Long? ->
                                        Log.d("CallDebug", "Online status for $calleePhoneNumber: isOnline=$isOnline, lastSeen=$lastSeen")
                                        statusListener?.let { userStatusManager.stopObservingUserStatus(calleePhoneNumber, it) }
                                        statusListener = null

                                        if (isOnline) {
                                            val channelToJoin = calleePhoneNumber
                                            val callId = FirebaseProvider.getCallRequestsRef(calleePhoneNumber).push().key ?: UUID.randomUUID().toString()
                                            val callRequest = CallRequest(
                                                callId = callId,
                                                callerId = currentUserId,
                                                callerName = currentUserName,
                                                calleeId = calleePhoneNumber,
                                                channelName = channelToJoin,
                                                status = Constants.CALL_STATUS_PENDING
                                            )
                                            Log.d("CallDebug", "CallRequest created: $callRequest")
                                            Log.d("CallDebug", "Sending call request to $calleePhoneNumber via CallSignalingManager")
                                            callSignalingManager.sendCallRequest(callRequest,
                                                onSuccess = {
                                                    Log.i("CallDebug", "Call request sent successfully to $calleePhoneNumber. Navigating to CallScreen.")
                                                    navController.navigate("call/${calleePhoneNumber}?callId=${callRequest.callId}&remoteUserId=${callRequest.calleeId}&localIsUsa=false")
                                                },
                                                onFailure = { errorMsg ->
                                                    Log.e("CallDebug", "Failed to send call request to $calleePhoneNumber: $errorMsg")
                                                    Toast.makeText(applicationContext, "Failed to send call request: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            Log.w("CallDebug", "Call attempt failed: User $calleePhoneNumber (${calleeUser.name}) is offline. Last seen: $lastSeen")
                                            Toast.makeText(applicationContext, "${calleeUser.name} is currently offline.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        route = "call/{number}?callId={callId}&remoteUserId={remoteUserId}&localIsUsa={localIsUsa}",
                        arguments = listOf(
                            navArgument("number") { type = NavType.StringType },
                            navArgument("callId") { type = NavType.StringType; nullable = true },
                            navArgument("remoteUserId") { type = NavType.StringType; nullable = true },
                            navArgument("localIsUsa") { type = NavType.BoolType; defaultValue = false } // Default to false (non-USA user)
                        )
                    ) { backStackEntry ->
                        val numberFromNav = backStackEntry.arguments?.getString("number") ?: ""
                        val callIdFromNav = backStackEntry.arguments?.getString("callId")
                        val remoteUserIdFromNav = backStackEntry.arguments?.getString("remoteUserId")
                        val localIsUsaFromNav = backStackEntry.arguments?.getBoolean("localIsUsa") ?: false

                        var finalUserForCallScreen = userToLog

                        if (finalUserForCallScreen == null || finalUserForCallScreen.phone != numberFromNav) {
                            val foundUser = usersToDisplay.find { it.phone == numberFromNav }
                            finalUserForCallScreen = foundUser ?: User(id = numberFromNav, name = numberFromNav, phone = numberFromNav, profilePicUrl = null)
                            userToLog = finalUserForCallScreen
                        }

                        if (finalUserForCallScreen != null) {
                            // The 'channel' for CallScreen is still numberFromNav as it's used for Agora channel joining directly.
                            // The uniqueChannelName (which might be different, e.g. sorted user IDs) is in CallRequest.
                            // For current logic where channelName in CallRequest IS numberFromNav, this is consistent.
                            CallScreen(
                                channel = numberFromNav,
                                token = Constants.AGORA_TOKEN,
                                appId = Constants.AGORA_APP_ID,
                                callId = callIdFromNav,
                                remoteUserId = remoteUserIdFromNav,
                                localIsUsa = localIsUsaFromNav, // Pass the value from Nav argument
                                onCallEnd = {
                                    navController.popBackStack()
                                    triggerRefresh()
                                },
                                mainRed = mainRed,
                                mainWhite = mainWhite,
                                callScreenViewModel = callScreenViewModel,
                                user = finalUserForCallScreen
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

// Placeholder definitions and functions removed.
// Actual implementations from ui.theme or ui packages should be used.