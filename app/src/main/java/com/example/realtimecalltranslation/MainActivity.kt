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
// import com.example.realtimecalltranslation.util.ImageStorageHelper // Import ImageStorageHelper - Removed as it's no longer used in MainActivity
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

    // State variables are defined inside onCreate using remember for proper lifecycle management with Compose.
    // private var userToLog by mutableStateOf<User?>(null) // Moved into setContent
    // private var callLogsFromSource by mutableStateOf<List<CallLog>>(emptyList()) // Moved into setContent

    // handleProfileUpdate will be defined inside setContent or receive states as parameters.
    // For simplicity with current structure, we'll ensure it's defined in a scope with access to the states.
    // The previous approach of making them class members was correct if handleProfileUpdate is a class method.
    // Let's stick to defining states within setContent and ensure handleProfileUpdate can access them.
    // This means handleProfileUpdate itself might need to be passed references or be defined within setContent.

    // Given ProfileScreen is called within setContent, and its lambda calls handleProfileUpdate,
    // handleProfileUpdate needs to be accessible. If it's a class method, it needs to operate on class member states.

    // Correcting the structure: States will be in setContent. handleProfileUpdate will be a lambda or local fun.
    // However, the error log implies ProfileScreen call is at line 674, which is deep inside setContent.
    // The `handleProfileUpdate` function at line 71 IS a class member.
    // The states `userToLog` (line 352) and `callLogsFromSource` (line 104) ARE `remember`ed inside `setContent`.
    // This is a mismatch.

    // SOLUTION: Make userToLog and callLogsFromSource class members as originally intended in the fix.
    // Then, ensure they are correctly updated by the class method handleProfileUpdate.
    // The `remember` calls inside `setContent` for these should be removed if they are class members.

    // Re-affirming: userToLog and callLogsFromSource should be MainActivity class members.
    private var userToLogState by mutableStateOf<User?>(null) // Renaming to avoid conflict with local remember
    private var callLogsFromSourceState by mutableStateOf<List<CallLog>>(emptyList())


    private fun handleProfileUpdate(updatedUser: User) {
        // Update userToLogState if it's the user whose profile was changed
        // Check against both id and phone for robustness, as user.id might be the phone number.
        if (userToLogState?.id == updatedUser.id || userToLogState?.phone == updatedUser.phone) {
            userToLogState = updatedUser
        }

        // Update callLogsFromSourceState
        callLogsFromSourceState = callLogsFromSourceState.map { log ->
            // Check against both id and phone for robustness
            if (log.user.id == updatedUser.id || log.user.phone == updatedUser.phone) {
                log.copy(user = updatedUser)
            } else {
                log
            }
        }
        // usersToDisplayState will automatically recompose as it's derived from callLogsFromSourceState
        Log.d("MainActivity", "User profile updated in MainActivity for ${updatedUser.id} (Phone: ${updatedUser.phone}). New Pic URL: ${updatedUser.profilePicUrl}, New Name: ${updatedUser.name}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Log.d("MainActivityDebug", "--- MainActivity setContent: Entered ---")
            RealTimeCallTranslationTheme {
                val applicationContext = LocalContext.current.applicationContext
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                // Using class member states: userToLogState and callLogsFromSourceState
                // The local `remember` for these are removed.
                // var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) } // Removed
                // Explicitly defining usersToDisplayState as State<List<User>>, derived from the class member state
                val usersToDisplayState: State<List<User>> = remember(callLogsFromSourceState) {
                    derivedStateOf {
                        callLogsFromSourceState.map { it.user }.distinctBy { it.id }
                    }
                }

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
                // Key updated to usersToDisplayState.value as usersToDisplayState is now State<List<User>>
                LaunchedEffect(currentUserId, usersToDisplayState.value) {
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
                                    val callerUser: User? = findUserInList(usersToDisplayState.value, callRequest.callerId, callRequest.callerId)
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

                // var callLogsFromSource by remember { mutableStateOf<List<CallLog>>(emptyList()) } // MOVED EARLIER

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
                        callLogsFromSourceState = logs // Update class member state
                        Log.d("MainActivity", "Call logs fetched. Count: ${logs.size}")
                    } else {
                        callLogsFromSourceState = emptyList() // Update class member state
                        Log.d("MainActivity", "Not enough permissions or permission denied, using empty logs. PermissionsGranted: $permissionsGranted, HasReadCallLog: $hasReadCallLogPerm")
                    }
                }

                // var userToLog by remember { mutableStateOf<User?>(null) } // Removed, using userToLogState (class member)
                // var profileScreenImageDisplayData by remember { mutableStateOf<Any?>(null) } // Removed as it's unused

                val callLogsToDisplay by remember(callLogsFromSourceState) { // Depends on class member state
                    derivedStateOf {
                        callLogsFromSource
                    }
                }
                // val usersToDisplay by remember(callLogsFromSourceState) { // MOVED EARLIER // Depends on class member state
                //     derivedStateOf {
                //         // If callLogsFromSource is empty, this will correctly produce an empty list of users.
                //         callLogsFromSource.map { it.user }.distinctBy { it.id }
                //     }
                // }
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

                        val foundCalleeUser: User? = findUserInList(usersToDisplayState.value, callerId, callerId)
                        val calleeUser = foundCalleeUser ?: User(callerId, callerName, callerId, callerProfilePicUrlFromNav)
                        // Set userToLogState for context if needed elsewhere, though IncomingCallScreen primarily uses its args
                        userToLogState = calleeUser

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
                                userToLogState = user // Update class member state
                                // profileScreenImageDisplayData = user.profilePicUrl // Removed as variable is deleted
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

                                userToLogState = calleeUser // Update class member state // Keep track of user for CallScreen
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
                                userToLogState = user // Update class member state
                                // profileScreenImageDisplayData = user.profilePicUrl // Removed as variable is deleted
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

                                val calleeUser = usersToDisplayState.value.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLogState = calleeUser // Update class member state

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

                                val calleeUser = usersToDisplayState.value.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLogState = calleeUser // Update class member state // Keep track of user
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
                        val userForProfile = if (userToLogState?.id == navigatedUserId) userToLogState else findUserInList(usersToDisplayState.value, navigatedUserId ?: "", navigatedUserId ?: "")

                        if (userForProfile != null) {
                            if (userToLogState?.id != userForProfile.id) userToLogState = userForProfile // Update class member state
                            // val currentImageDataSource = profileScreenImageDisplayData ?: userForProfile.profilePicUrl // Will be null -> This line is removed as ProfileScreen handles image loading.
                            // ProfileScreen is now imported from com.example.realtimecalltranslation.ui.theme
                            ProfileScreen( // No need for full package name if imported correctly
                                user = userForProfile,
                                callLogs = callLogsToDisplay.filter { it.user.id == userForProfile.id }, // Use userForProfile.id
                                // onNameUpdate is removed as ProfileScreen now handles name updates and informs via onProfileUpdated
                                // onUserProfileUpdated (old) is removed
                                onProfileUpdated = { updatedUserFromProfile: User -> // Explicitly typed lambda parameter
                                    handleProfileUpdate(updatedUserFromProfile) // Call the MainActivity's new handler
                                },
                                onBack = {
                                    // profileScreenImageDisplayData = null // This state variable will be removed entirely
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

                                    userToLogState = calleeUser // Update class member state // Keep track of user
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

                                val calleeUser = usersToDisplayState.value.find { it.phone == calleePhoneNumber }
                                    ?: User(id = calleePhoneNumber, name = calleePhoneNumber, phone = calleePhoneNumber, profilePicUrl = null)
                                userToLogState = calleeUser // Update class member state // Keep track of user
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

                        var finalUserForCallScreen = userToLogState // Use class member state

                        if (finalUserForCallScreen == null || finalUserForCallScreen.phone != numberFromNav) {
                            val foundUser = findUserInList(usersToDisplayState.value, numberFromNav, numberFromNav) // Using helper
                            finalUserForCallScreen = foundUser ?: User(id = numberFromNav, name = numberFromNav, phone = numberFromNav, profilePicUrl = null)
                            userToLogState = finalUserForCallScreen // Update class member state
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