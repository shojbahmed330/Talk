package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val profilePicUrl: String? = null
)

@Composable
fun UserAvatar(
    user: User,
    size: Dp,
    color: Color = Color.Black, // Optional color parameter for text/initials if you want to support that
    modifier: Modifier = Modifier
) {
    if (user.profilePicUrl != null && user.profilePicUrl.isNotBlank()) {
        AsyncImage(
            model = user.profilePicUrl,
            contentDescription = user.name,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = user.name,
                tint = color, // Use the color parameter for icon tint
                modifier = Modifier.size(size.times(0.6f))
            )
        }
    }
}