package com.example.realtimecalltranslation.ui // অথবা .ui.theme আপনার স্ট্রাকচার অনুযায়ী

import android.util.Log // এই import লাইনটি যোগ করুন যদি না থাকে
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.ui.theme.User // User.kt থেকে
import com.example.realtimecalltranslation.ui.theme.UserAvatar // User.kt থেকে
import kotlinx.coroutines.delay
// CallScreenViewModel import টা CallScreenViewModel.kt ফাইলের প্যাকেজ অনুযায়ী হবে
// import com.example.realtimecalltranslation.ui.CallScreenViewModel

@Composable
fun CallScreen(
    channel: String,
    token: String?,
    appId: String,
    localIsUsa: Boolean,
    onCallEnd: () -> Unit,
    mainRed: Color, // এগুলো সম্ভবত Theme থেকে আসবে বা লোকালভাবে ডিফাইন করা
    mainWhite: Color, // এগুলো সম্ভবত Theme থেকে আসবে বা লোকালভাবে ডিফাইন করা
    callScreenViewModel: CallScreenViewModel,
    user: User? = null
) {
    // --- ছবির ডিবাগিং লগ শুরু ---
    Log.d("PicDebugCallScreen", "CallScreen received User - Name: ${user?.name}, Phone: ${user?.phone}, ProfilePicUrl: ${user?.profilePicUrl}")
    // --- ছবির ডিবাগিং লগ শেষ ---

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var isMuted by rememberSaveable { mutableStateOf(false) }
    var isSpeakerOn by rememberSaveable { mutableStateOf(false) }
    var isOnHold by rememberSaveable { mutableStateOf(false) }
    var buttonScale by remember { mutableStateOf(1f) }

    LaunchedEffect(isMuted, isSpeakerOn, isOnHold) {
        buttonScale = 0.9f
        delay(100)
        buttonScale = 1f
    }

    LaunchedEffect(Unit) {
        // user?.name পাস করা হচ্ছে ViewModel এ
        callScreenViewModel.joinCall(channel, token, appId, user?.name)
    }

    DisposableEffect(Unit) {
        onDispose {
            callScreenViewModel.leaveCall()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainWhite)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "In Call: ${user?.name ?: channel}",
            style = MaterialTheme.typography.titleLarge,
            color = mainRed,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user?.phone ?: channel,
            style = MaterialTheme.typography.bodyLarge,
            color = mainRed.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(150.dp)
                .border(4.dp, mainRed.copy(alpha = pulseScale), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                user = user ?: User("0", "UnknownFallback", channel, null), // Fallback User
                size = 120.dp,
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
            )
        }
        Spacer(Modifier.height(24.dp))

        var seconds by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
        Text(
            text = String.format("%02d:%02d", seconds / 60, seconds % 60),
            style = MaterialTheme.typography.bodyLarge,
            color = mainRed,
            fontSize = 18.sp
        )
        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    callScreenViewModel.toggleMute(isMuted)
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(if (isMuted) mainRed else mainWhite, CircleShape)
                    .border(2.dp, mainRed, CircleShape)
                    .scale(buttonScale)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) mainWhite else mainRed,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    isSpeakerOn = !isSpeakerOn
                    callScreenViewModel.toggleSpeaker(isSpeakerOn)
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(if (isSpeakerOn) mainRed else mainWhite, CircleShape)
                    .border(2.dp, mainRed, CircleShape)
                    .scale(buttonScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.SpeakerPhone,
                    contentDescription = if (isSpeakerOn) "Turn off speaker" else "Turn on speaker",
                    tint = if (isSpeakerOn) mainWhite else mainRed,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    isOnHold = !isOnHold
                    callScreenViewModel.toggleHold(isOnHold)
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(if (isOnHold) mainRed else mainWhite, CircleShape)
                    .border(2.dp, mainRed, CircleShape)
                    .scale(buttonScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = if (isOnHold) "Resume" else "Hold",
                    tint = if (isOnHold) mainWhite else mainRed,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        IconButton(
            onClick = onCallEnd,
            modifier = Modifier
                .size(80.dp)
                .background(mainRed, CircleShape)
                .border(2.dp, mainWhite, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.CallEnd,
                contentDescription = "End Call",
                tint = mainWhite,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}