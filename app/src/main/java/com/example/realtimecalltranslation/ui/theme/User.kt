package com.example.realtimecalltranslation.ui.theme

import android.net.Uri // Added import for Uri
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
import androidx.compose.material3.MaterialTheme // Added import for MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

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
    val model = user.profilePicUrl?.let { Uri.parse(it) }

    if (model != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = user.name ?: "User Avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_avatar), // Fallback to placeholder on error
            placeholder = painterResource(id = R.drawable.ic_avatar) // Fallback placeholder while loading
        )
    } else {
        // Fallback to placeholder Icon if no profilePicUrl
        Image(
            imageVector = Icons.Filled.Person, // Default placeholder
            contentDescription = user.name ?: "User Avatar Placeholder",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) // Tint for the placeholder
        )
    }
}
