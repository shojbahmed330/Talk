package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    mainRed: Color,
    mainWhite: Color,
    accentRed: Color,
    lightRed: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) mainRed else lightRed,
            contentColor = if (selected) mainWhite else accentRed
        ),
        shape = CircleShape
    ) {
        Text(
            text = title,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}