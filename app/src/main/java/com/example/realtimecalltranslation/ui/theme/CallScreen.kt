package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Message(
    val fromUsa: Boolean,
    val original: String,
    val translated: String
)

@Composable
fun CallScreen(
    channel: String,
    token: String?,
    appId: String,
    localIsUsa: Boolean,
    onCallEnd: () -> Unit,
    messages: List<Message>,
    mainRed: Color,
    mainWhite: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("In Call: $channel", style = MaterialTheme.typography.titleLarge, color = mainRed)
        Spacer(Modifier.height(8.dp))
        // Conversation bubbles
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            messages.forEach { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (msg.fromUsa) Arrangement.End else Arrangement.Start
                ) {
                    Column(
                        horizontalAlignment =
                            if (msg.fromUsa) Alignment.End else Alignment.Start
                    ) {
                        val showText = if (localIsUsa) {
                            if (msg.fromUsa) msg.original else msg.translated
                        } else {
                            if (msg.fromUsa) msg.translated else msg.original
                        }
                        Surface(
                            color = if (msg.fromUsa) mainRed.copy(alpha = 0.11f) else mainWhite.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                showText,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 16.sp,
                                color = if (msg.fromUsa) mainRed else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onCallEnd() },
            colors = ButtonDefaults.buttonColors(
                containerColor = mainRed,
                contentColor = mainWhite
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Call")
        }
    }
}