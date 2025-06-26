package com.example.realtimecalltranslation.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.util.Constants // Import Constants
import com.example.realtimecalltranslation.ui.CallScreenViewModel // Import ViewModel

// Required Compose imports
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay // For delay
import androidx.compose.foundation.border // For border modifier
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SpeakerPhone
// import androidx.compose.ui.draw.scale // Already imported via foundation.layout or animation.core
// androidx.compose.material.icons.Icons (already implicitly available or via specific icon imports)
// Specific icons are already imported

@Composable
fun IncomingCallScreen(
    callerId: String,
    callerName: String,
    channelName: String,
    callId: String,
    callScreenViewModel: CallScreenViewModel, // Added ViewModel
    onAcceptCall: (channel: String, callId: String, remoteUserId: String, callerName: String) -> Unit, // Modified for more specific action
    onRejectCall: () -> Unit, // Renamed for clarity
    onEndCall: () -> Unit, // For ending the call after accept
    localIsUsa: Boolean, // New parameter
    mainRed: Color = Color(0xFFD32F2F),
    mainWhite: Color = Color.White,
    mainGreen: Color = Color(0xFF388E3C)
) {
    var isCallAccepted by remember { mutableStateOf(false) }
    val callerUser = User(id = callerId, name = callerName, phone = callerId, profilePicUrl = null) // Create a User object for avatar etc.

    if (!isCallAccepted) {
        // UI for incoming call (ringing state)
        val infiniteTransition = rememberInfiniteTransition(label = "incoming_call_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale_accept"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(mainWhite)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    user = callerUser,
                    size = 120.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Text(
                    text = callerName, // Display caller's name
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Incoming Call...",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 60.dp)
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = onRejectCall, // From MainActivity
                        containerColor = mainRed,
                        contentColor = mainWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Filled.CallEnd, "Reject Call", modifier = Modifier.size(36.dp))
                    }

                    FloatingActionButton(
                        onClick = {
                            isCallAccepted = true
                            callScreenViewModel.joinCall(
                                channel = channelName,
                                token = Constants.AGORA_TOKEN,
                                appId = Constants.AGORA_APP_ID,
                                userName = callerName, // This is the remote user's name for the local CallScreenViewModel perspective
                                callId = callId,
                                remoteUserId = callerId,
                                isLocalUserFromUSA = localIsUsa // Pass the localIsUsa parameter here
                            )
                            onAcceptCall(channelName, callId, callerId, callerName) // Notify MainActivity
                        },
                        containerColor = mainGreen,
                        contentColor = mainWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp).scale(pulseScale)
                    ) {
                        Icon(Icons.Filled.Call, "Accept Call", modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    } else {
        // UI for active call (after accept)
        // This will be similar to CallScreen.kt's content
        // We need to pass necessary parameters and viewModel
        ActiveCallContent(
            callScreenViewModel = callScreenViewModel,
            user = callerUser, // This is the remote user (caller)
            onEndCall = onEndCall, // From MainActivity to handle cleanup and popBackStack
            mainRed = mainRed,
            mainWhite = mainWhite
        )
    }
}

@Composable
fun ActiveCallContent(
    callScreenViewModel: CallScreenViewModel,
    user: User?, // User object of the remote person
    onEndCall: () -> Unit,
    mainRed: Color,
    mainWhite: Color
) {
    // Observe call connection state for timer
    val isRemoteUserJoined by callScreenViewModel.isRemoteUserJoined.collectAsState()
    var seconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRemoteUserJoined) {
        if (isRemoteUserJoined) {
            seconds = 0 // Reset timer
            while (true) { // Keep this loop running as long as isRemoteUserJoined is true and composable is active
                delay(1000)
                seconds++
            }
        } else {
            seconds = 0 // Reset if remote user leaves or before joining
        }
    }

    // Pulsing animation for avatar (similar to CallScreen.kt)
    val infiniteTransition = rememberInfiniteTransition(label = "active_call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale_active"
    )

    var isMuted by rememberSaveable { mutableStateOf(false) }
    var isSpeakerOn by rememberSaveable { mutableStateOf(false) }
    var isOnHold by rememberSaveable { mutableStateOf(false) } // Ensured mutableStateOf
    var buttonScale by remember { mutableStateOf(1f) }


    LaunchedEffect(isMuted, isSpeakerOn, isOnHold) {
        buttonScale = 0.9f
        delay(100) // Short delay for press animation
        buttonScale = 1f
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainWhite)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "In Call: ${user?.name ?: "Unknown"}",
            style = MaterialTheme.typography.titleLarge,
            color = mainRed,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user?.phone ?: "...",
            style = MaterialTheme.typography.bodyLarge,
            color = mainRed.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(150.dp)
                .border(4.dp, mainRed.copy(alpha = if(isRemoteUserJoined) pulseScale else 1f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                user = user ?: User("0", "UnknownFallback", "", null),
                size = 120.dp,
                modifier = Modifier.size(120.dp).scale(if(isRemoteUserJoined) pulseScale else 1f)
            )
        }
        Spacer(Modifier.height(24.dp))

        Text(
            text = if (isRemoteUserJoined) String.format("%02d:%02d", seconds / 60, seconds % 60) else "Connecting...",
            style = MaterialTheme.typography.bodyLarge,
            color = mainRed,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(16.dp))

        // Column for displaying transcriptions, translations, and status (from CallScreen.kt)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
                .border(1.dp, Color.LightGray)
                .padding(8.dp)
        ) {
            Text("Status: ${callScreenViewModel.currentStatusMessage}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            callScreenViewModel.errorMessage?.let {
                Text("Error: $it", style = MaterialTheme.typography.bodySmall, color = Color.Red)
            }
            Spacer(Modifier.height(8.dp))
            Text("You (Local): ${callScreenViewModel.localUserTranscribedText}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Translation (for remote): ${callScreenViewModel.localUserTranslatedText}", style = MaterialTheme.typography.bodyMedium, color = mainRed.copy(alpha = 0.8f))
            Spacer(Modifier.height(16.dp))
            Text("Remote (${user?.name ?: ""}): ${callScreenViewModel.remoteUserTranscribedText}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Translation (for you): ${callScreenViewModel.remoteUserTranslatedText}", style = MaterialTheme.typography.bodyMedium, color = mainRed.copy(alpha = 0.8f))
        }

        // Control buttons (from CallScreen.kt)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { isMuted = !isMuted; callScreenViewModel.toggleMute(isMuted) },
                modifier = Modifier.size(64.dp).background(if (isMuted) mainRed else mainWhite, CircleShape).border(2.dp, mainRed, CircleShape).scale(buttonScale)) {
                Icon(if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, if (isMuted) "Unmute" else "Mute", tint = if (isMuted) mainWhite else mainRed, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { isSpeakerOn = !isSpeakerOn; callScreenViewModel.toggleSpeaker(isSpeakerOn) },
                modifier = Modifier.size(64.dp).background(if (isSpeakerOn) mainRed else mainWhite, CircleShape).border(2.dp, mainRed, CircleShape).scale(buttonScale)) {
                Icon(Icons.Filled.SpeakerPhone, if (isSpeakerOn) "Speaker Off" else "Speaker On", tint = if (isSpeakerOn) mainWhite else mainRed, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { isOnHold = !isOnHold; callScreenViewModel.toggleHold(isOnHold) },
                modifier = Modifier.size(64.dp).background(if (isOnHold) mainRed else mainWhite, CircleShape).border(2.dp, mainRed, CircleShape).scale(buttonScale)) {
                Icon(Icons.Filled.Pause, if (isOnHold) "Resume" else "Hold", tint = if (isOnHold) mainWhite else mainRed, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        IconButton(
            onClick = {
                callScreenViewModel.leaveCall() // ViewModel handles Agora leave
                onEndCall() // MainActivity handles navigation pop and Firebase cleanup
            },
            modifier = Modifier.size(80.dp).background(mainRed, CircleShape).border(2.dp, mainWhite, CircleShape)
        ) {
            Icon(Icons.Filled.CallEnd, "End Call", tint = mainWhite, modifier = Modifier.size(40.dp))
        }
    }
}
