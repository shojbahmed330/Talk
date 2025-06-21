package com.example.realtimecalltranslation.ui.theme

import android.util.Log
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
    modifier: Modifier = Modifier
) {
    val imageUrl = user.profilePicUrl
    Log.d("PicDebugUserAvatar", "UserAvatar: User: ${user.name}, ProfilePicUrl from user object: ${user.profilePicUrl}, imageUrl variable: $imageUrl")

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
            painter = painterResource(id = R.drawable.ic_avatar),
            contentDescription = user.name ?: "User Avatar Placeholder",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}
