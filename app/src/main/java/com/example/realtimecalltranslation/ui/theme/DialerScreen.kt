package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // ইতিমধ্যে আছে
import androidx.compose.material.icons.automirrored.filled.Backspace // নতুন ইম্পোর্ট যোগ
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialerScreen(
    onClose: () -> Unit,
    mainRed: Color,
    mainWhite: Color,
    onNavigateToCall: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    val dialerBgBrush = Brush.verticalGradient(
        colors = listOf(mainRed.copy(alpha = 0.9f), mainRed.copy(alpha = 0.7f))
    )
    val numPadContainerColor = mainWhite
    val numPadContentColor = mainRed
    val callButtonContainerColor = mainWhite
    val callButtonContentColor = mainRed
    val backspaceIconColor = mainRed
    val phoneNumberTextColor = mainRed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dialerBgBrush),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = mainWhite
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .background(
                    mainWhite.copy(alpha = 0.95f),
                    shape = CircleShape
                )
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (phoneNumber.isEmpty()) "Enter Number" else phoneNumber,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = phoneNumberTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(start = 38.dp)
                )
                IconButton(
                    onClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                    modifier = Modifier.size(38.dp).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace, // সংশোধিত
                        contentDescription = "Delete",
                        tint = backspaceIconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))

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
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "scale")

                    Button(
                        onClick = { phoneNumber += key },
                        modifier = Modifier
                            .scale(scale)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        pressed = true
                                        tryAwaitRelease()
                                        pressed = false
                                    }
                                )
                            }
                            .size(76.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = numPadContainerColor,
                            contentColor = numPadContentColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            key,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = numPadContentColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

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
                containerColor = callButtonContainerColor,
                contentColor = callButtonContentColor
            )
        ) {
            Text("Call", fontSize = 22.sp, color = callButtonContentColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(28.dp))
    }
}