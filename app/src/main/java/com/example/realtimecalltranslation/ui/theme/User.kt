package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.realtimecalltranslation.R

// Canonical User data class
data class User(
    val id: String,
    val name: String,
    val phone: String,
    val profilePicUrl: String? = null // Made nullable to support users without pics
)

@Composable
fun UserAvatar(
    user: User, // This will now refer to the User class defined above
    size: Dp,
    modifier: Modifier = Modifier
) {
    val imageUrl = user.profilePicUrl
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = user.name,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.ic_avatar), // Assuming R.drawable.ic_avatar exists
            contentDescription = user.name,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}