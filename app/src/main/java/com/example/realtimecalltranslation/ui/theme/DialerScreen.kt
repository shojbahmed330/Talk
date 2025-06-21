package com.example.realtimecalltranslation.ui.theme

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
// import androidx.compose.ui.platform.LocalContext // No longer needed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.realtimecalltranslation.OutgoingCallActivity // No longer needed

@Composable
fun DialerScreen(
    onClose: () -> Unit, // This will be repurposed for the back button
    mainRed: Color,
    mainWhite: Color,
    onNavigateToCall: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    // Using mainRed and mainWhite as requested
    val dialerBgBrush = Brush.verticalGradient(
        colors = listOf(mainRed.copy(alpha = 0.9f), mainRed.copy(alpha = 0.7f))
    )
    val numPadContainerColor = mainWhite
    val numPadContentColor = mainRed // Text color for numbers
    val callButtonContainerColor = mainWhite
    val callButtonContentColor = mainRed
    val backspaceIconColor = mainRed // For the backspace icon on white background
    val phoneNumberTextColor = mainRed // For the displayed phone number text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dialerBgBrush), // Using a red gradient background
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Back Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp) // Adjusted padding
        ) {
            IconButton(
                onClick = onClose, // Reusing the existing onClose lambda which should pop backstack
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = mainWhite // White icon on Red background
                )
            }
            // You could add a Title Text here if needed, aligned to Center.
            // For now, focusing on the back button as per primary request.
        }

        // Spacer(Modifier.height(40.dp)) // Original spacer, adjust as needed after adding back button

        // Phone Number Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .background(
                    mainWhite.copy(alpha = 0.95f), // White background for number display
                    shape = CircleShape
                )
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Ensure space for backspace
            ) {
                Text(
                    text = if (phoneNumber.isEmpty()) "Enter Number" else phoneNumber,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = phoneNumberTextColor, // Using mainRed for text
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(start = 38.dp) // Add padding to center text if backspace is present
                )
                IconButton(
                    onClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                    modifier = Modifier.size(38.dp).padding(end = 8.dp) // Padding for the icon button
                ) {
                    Icon(
                        Icons.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = backspaceIconColor, // Using mainRed for icon
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))

        // Dial Pad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )
        keys.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 6.dp), // Reduced vertical padding slightly
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "scale")

                    Button(
                        onClick = { phoneNumber += key },
                        modifier = Modifier
                            .scale(scale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { /* Called when the gesture starts */
                                        pressed = true
                                        tryAwaitRelease()
                                        pressed = false
                                    }
                                )
                            }
                            .size(76.dp), // Kept size
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = numPadContainerColor, // White buttons
                            contentColor = numPadContentColor // Red text
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            key,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = numPadContentColor // Red text
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Call Button
        var callButtonPressed by remember { mutableStateOf(false) }
        val callButtonScale by animateFloatAsState(if (callButtonPressed) 0.9f else 1f, label = "callButtonScale")

        Button(
            onClick = { if (phoneNumber.isNotEmpty()) onNavigateToCall(phoneNumber) },
            modifier = Modifier
                .scale(callButtonScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            callButtonPressed = true
                            tryAwaitRelease()
                            callButtonPressed = false
                        }
                    )
                }
                .fillMaxWidth()
                .padding(horizontal = 70.dp)
                .height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = callButtonContainerColor, // White call button
                contentColor = callButtonContentColor // Red text for call button
            )
        ) {
            Text("Call", fontSize = 22.sp, color = callButtonContentColor, fontWeight = FontWeight.Bold)
        }
        // Removed the "Close" TextButton as requested
        Spacer(Modifier.height(28.dp)) // Adjusted spacer after removing close button
    }
}
