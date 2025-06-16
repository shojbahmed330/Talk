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
import com.example.realtimecalltranslation.ui.theme.User

@Composable
fun UserAvatar(
    user: User,
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
        // Default avatar from drawable resource
        Image(
            painter = painterResource(id = R.drawable.ic_avatar),
            contentDescription = user.name,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}