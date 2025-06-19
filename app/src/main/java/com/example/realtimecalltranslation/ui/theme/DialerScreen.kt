package com.example.realtimecalltranslation.ui.theme

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.platform.LocalContext // No longer needed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.realtimecalltranslation.OutgoingCallActivity // No longer needed

@Composable
fun DialerScreen(
    onClose: () -> Unit,
    mainRed: Color, // This parameter is not used in the current body, but kept as per signature
    mainWhite: Color, // This parameter is not used in the current body, but kept as per signature
    onNavigateToCall: (String) -> Unit // Added new parameter
) {
    var phoneNumber by remember { mutableStateOf("") }
    // val context = LocalContext.current // No longer needed

    // Professional color palette
    val dialerBg = Brush.verticalGradient(
        colors = listOf(Color(0xFFf7fafc), Color(0xFFe3e5ea))
    )
    val numPadColor = Color(0xFFffffff)
    val numTextColor = Color(0xFF222e3a)
    val callBtnColor = Color(0xFF2AAA5B)
    val callBtnTextColor = Color.White
    val iconColor = Color(0xFF4B5563)

    // fun goToOutgoingCallActivity(number: String) { // Removed this function
    //     val intent = Intent(context, OutgoingCallActivity::class.java)
    //     intent.putExtra("CALLEE_NUMBER", number)
    //     context.startActivity(intent)
    // }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dialerBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        // Phone Number Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .background(
                    Color.White.copy(alpha = 0.95f),
                    shape = CircleShape
                )
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (phoneNumber.isEmpty()) "Enter Number" else phoneNumber,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = numTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = iconColor,
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
                    .padding(horizontal = 48.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Button(
                        onClick = { phoneNumber += key },
                        modifier = Modifier
                            .size(76.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = numPadColor,
                            contentColor = numTextColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            key,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = numTextColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Call Button
        Button(
            onClick = { if (phoneNumber.isNotEmpty()) onNavigateToCall(phoneNumber) }, // Changed to use onNavigateToCall
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 70.dp)
                .height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = callBtnColor)
        ) {
            Text("Call", fontSize = 22.sp, color = callBtnTextColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        TextButton(
            onClick = { onClose() }
        ) {
            Text("Close", color = Color(0xFF6B7280), fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
    }
}
