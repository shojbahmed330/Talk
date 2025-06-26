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
// import coil.compose.AsyncImage // Unused import
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
    // val imageUrl = user.profilePicUrl // No longer attempting to load profilePicUrl
    Log.d("UserAvatar", "Displaying placeholder for user: ${user.name}. ProfilePicUrl is intentionally ignored.")

    // Always display the placeholder image as profile pictures are removed from the feature set.
    Image(
        painter = painterResource(id = R.drawable.ic_avatar), // Ensure ic_avatar drawable exists
        contentDescription = user.name ?: "User Avatar Placeholder",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}
