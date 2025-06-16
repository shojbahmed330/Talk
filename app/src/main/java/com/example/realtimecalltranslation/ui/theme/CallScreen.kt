package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Data class for messages, might be moved later if needed elsewhere
data class Message(
    val fromUsa: Boolean,
    val original: String,
    val translated: String
)

@Composable
fun CallScreen(
    channel: String, // Phone number
    token: String?,
    appId: String,
    localIsUsa: Boolean,
    onCallEnd: () -> Unit,
    messages: List<Message>,
    mainRed: Color,
    mainWhite: Color,
    // New parameters
    contactName: String,
    contactProfilePicUrl: String?,
    onToggleLoudspeaker: (Boolean) -> Unit,
    onToggleMute: (Boolean) -> Unit
) {
    var isLoudspeakerOn by remember { mutableStateOf(false) }
    var isMuteOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainWhite)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Area: Contact Info
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(mainRed.copy(alpha = 0.1f)) // Fallback background using theme color
                .border(2.dp, mainRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!contactProfilePicUrl.isNullOrBlank()) {
                AsyncImage(
                    model = contactProfilePicUrl,
                    contentDescription = "$contactName profile picture",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Default Avatar",
                    modifier = Modifier.size(80.dp),
                    tint = mainRed
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = contactName,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = mainRed // Using mainRed for emphasis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = channel, // Phone number
            fontSize = 18.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Middle Area: Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp) // Added padding for messages list
        ) {
            items(messages) { msg -> // Corrected usage of items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), // Padding for each message row
                    horizontalArrangement = if (msg.fromUsa == localIsUsa) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.fromUsa == localIsUsa) mainRed.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp), // Consistent rounded corners
                        modifier = Modifier.padding(horizontal = 4.dp) // Padding around surface
                    ) {
                        Text(
                            text = if (msg.fromUsa == localIsUsa) msg.original else msg.translated,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            fontSize = 16.sp,
                            color = Color.Black // Consistent text color
                        )
                    }
                }
            }
        }

        // Bottom Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp), // Increased vertical padding
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isLoudspeakerOn = !isLoudspeakerOn
                    onToggleLoudspeaker(isLoudspeakerOn)
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(mainRed.copy(alpha = 0.1f)) // Using theme color variant
            ) {
                Icon(
                    imageVector = if (isLoudspeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = "Toggle Loudspeaker",
                    tint = mainRed, // Using mainRed
                    modifier = Modifier.size(30.dp)
                )
            }

            Button(
                onClick = onCallEnd,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = mainRed),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call",
                    tint = mainWhite, // Explicitly mainWhite
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = {
                    isMuteOn = !isMuteOn
                    onToggleMute(isMuteOn)
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(mainRed.copy(alpha = 0.1f)) // Using theme color variant
            ) {
                Icon(
                    imageVector = if (isMuteOn) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Toggle Mute",
                    tint = mainRed, // Using mainRed
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}