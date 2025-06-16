package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.R

@Composable
fun LoginScreen(
    onLogin: () -> Unit = {},
    onFacebook: () -> Unit = {},
    onGoogle: () -> Unit = {},
    onSignUp: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_login_illustration),
                contentDescription = "Login Illustration",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text("RealTimeCallTranslation", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(
                "Connect across languages instantly!",
                fontSize = 16.sp,
                color = Color(0xFF444444)
            )
            Spacer(modifier = Modifier.height(16.dp))
            UpgradePlanButton()
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { /* Forgot password */ }) {
                    Text("Forgot your password?", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6C47E)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Log in", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                Text("  or  ", fontSize = 14.sp)
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onFacebook,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B5998)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Facebook", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onGoogle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB4437)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Google", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text("New user? ")
                TextButton(onClick = onSignUp) {
                    Text("Sign up")
                }
            }
        }
    }
}