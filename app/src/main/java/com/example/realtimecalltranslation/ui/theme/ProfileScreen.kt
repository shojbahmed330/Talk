package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallLogRow

@Composable
fun ProfileScreen(
    user: User,
    callLogs: List<CallLog>,
    onBack: () -> Unit,
    onCall: (User) -> Unit,
    mainRed: Color,
    mainWhite: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(mainRed, mainWhite)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    mainRed,
                                    mainRed.copy(alpha = 0.8f),
                                    mainWhite
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = mainRed
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = mainWhite
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp)) // for symmetry
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (user.profilePicUrl != null && user.profilePicUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = user.profilePicUrl),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(mainWhite, CircleShape)
                            .border(4.dp, mainWhite, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(mainWhite)
                            .border(4.dp, mainWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.first().toString(),
                            fontSize = 48.sp,
                            color = mainRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name & Phone
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = mainWhite
                )
                user.phone?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            color = mainWhite.copy(alpha = 0.85f),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { onCall(user) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mainWhite,
                        contentColor = mainRed
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(46.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                ) {
                    Text("Call", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Call History Title
            Text(
                text = "Call History",
                fontWeight = FontWeight.Medium,
                fontSize = 19.sp,
                color = mainRed,
                modifier = Modifier
                    .padding(start = 24.dp, bottom = 6.dp, top = 8.dp)
            )

            // Call log list
            if (callLogs.isNotEmpty()) {
                Surface(
                    color = mainWhite.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp)
                ) {
                    Column(
                        Modifier
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        callLogs.forEach { log ->
                            CallLogRow(
                                log = log,
                                onProfile = {},
                                onCall = onCall,
                                mainRed = mainRed,
                                accentRed = mainRed,
                                lightRed = mainWhite,
                                cardBg = mainWhite
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No call history found.",
                        color = mainWhite,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}