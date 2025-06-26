package com.example.realtimecalltranslation.ui.theme // অথবা .ui.theme আপনার স্ট্রাকচার অনুযায়ী
import android.util.Log // এই import লাইনটি যোগ করুন যদি না থাকে
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.realtimecalltranslation.ui.CallScreenViewModel

@Composable
fun CallScreen(
    channel: String, // This is the Agora channel name (currently callee's phone number)
    token: String?,
    appId: String,
    callId: String?, // Firebase CallRequest ID
    remoteUserId: String?, // Firebase ID of the other user in the call
    localIsUsa: Boolean,
    onCallEnd: () -> Unit,
    mainRed: Color,
    mainWhite: Color,
    callScreenViewModel: CallScreenViewModel,
    user: User? = null // User details of the person being called/is on call (remote user)
) {
    // Log for debugging outgoing call screen entry
    Log.d("CallScreen", "Outgoing CallScreen: User: ${user?.name}, Channel: $channel, CallID: $callId, RemoteUserID: $remoteUserId")

    // When CallScreen is used for an outgoing call, it needs to initiate the call joining process.
    // This LaunchedEffect will run when the composable enters the composition.
    // Keying it with `Unit` makes it run once. If channel/callId can change while on screen, key with them.
    LaunchedEffect(key1 = channel, key2 = callId) {
        callScreenViewModel.joinCall(
            channel = channel,
            token = token, // Ensure token is correctly sourced or managed by ViewModel/Constants
            appId = appId,   // Ensure appId is correctly sourced or managed by ViewModel/Constants
            userName = user?.name, // This is the remote user's name for CallScreenViewModel perspective
            callId = callId,
            remoteUserId = remoteUserId,
            isLocalUserFromUSA = localIsUsa // Pass the localIsUsa parameter here
        )
    }

    // The onDispose logic for leaveCall is primarily handled by the onEndCall callback
    // passed to ActiveCallContent, which is triggered by the End Call button.
    // If direct back navigation without pressing "End Call" should also terminate the call,
    // then a DisposableEffect like below would be needed.
    // However, this might conflict if onEndCall also calls leaveCall and pops backstack.
    // For now, assume onEndCall is the primary mechanism.
    /*
    DisposableEffect(key1 = channel, key2 = callId) {
        onDispose {
            Log.d("CallScreen", "Leaving CallScreen for channel: $channel. Calling leaveCall.")
            callScreenViewModel.leaveCall()
        }
    }
    */

    // Delegate the entire UI to ActiveCallContent
    // ActiveCallContent is defined in com.example.realtimecalltranslation.ui.theme.IncomingCallScreen.kt
    ActiveCallContent(
        callScreenViewModel = callScreenViewModel,
        user = user, // This is the remote user for this call
        onEndCall = onCallEnd, // This lambda from MainActivity will handle navigation pop & potential Firebase cleanup via ViewModel
        mainRed = mainRed,
        mainWhite = mainWhite
    )
}