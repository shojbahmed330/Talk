package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.R

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_globe), // drawable resource
                contentDescription = "Globe",
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            UpgradePlanButton()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Real-Time",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color(0xFF202020)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect effortlessly with",
                fontSize = 18.sp,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6C47E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF222222)
                )
            }
        }
    }
}