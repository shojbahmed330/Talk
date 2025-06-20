package com.example.realtimecalltranslation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter // Added import
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.realtimecalltranslation.ui.theme.CallLog
// CallLogRow will be replaced by custom layout
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.formatTimeAgo // Added import
import com.example.realtimecalltranslation.ui.theme.CallType // Added import
// Removed duplicate: import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider // Added import
import androidx.compose.material3.MaterialTheme // Added import for MaterialTheme.colorScheme

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds < 0) return ""
    if (seconds == 0L) return "0s"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (secs > 0 || (hours == 0L && minutes == 0L)) append("${secs}s")
    }.trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    callLogs: List<CallLog>,
    onBack: () -> Unit,
    onCall: (User) -> Unit,
    mainRed: Color,
    mainWhite: Color,
    onNameUpdate: (newName: String) -> Unit,
    onProfilePicUriSelected: (uriString: String?) -> Unit,
    imageDataSource: Any? = null // Default to null if not provided
) {
    Log.d("ProfileScreenInit", "Composing ProfileScreen for User: ${user.name}, Initial imageDataSource: $imageDataSource")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("ProfileScreenPicker", "Image URI selected by picker: ${uri?.toString()}")
        onProfilePicUriSelected(uri?.toString())
    }

    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable { mutableStateOf(user.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(mainRed, mainWhite)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    mainRed,
                                    mainRed.copy(alpha = 0.8f),
                                    mainWhite
                                )
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = mainRed
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = mainWhite
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(40.dp)) // for symmetry
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Centered Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp) // This is the outer clickable Box size
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    key(imageDataSource, user.profilePicUrl) { // Updated key
                        // This inner Box is for the image/placeholder itself
                        Box(
                            modifier = Modifier
                                .fillMaxSize() // Fill the 110.dp Box
                                .clip(CircleShape)
                                .background(mainWhite, CircleShape) // Background for the circle
                                .border(4.dp, mainWhite, CircleShape), // Border for the circle
                            contentAlignment = Alignment.Center
                        ) {
                            val modelToLoad: Any? = imageDataSource ?: user.profilePicUrl

                            if (modelToLoad is ByteArray || (modelToLoad is String && modelToLoad.isNotBlank())) {
                                // Valid model for Coil: ByteArray or non-blank String (URL/URI)
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(modelToLoad)
                                        .crossfade(true)
                                        // .size(Size(256, 256)) // Optional
                                        .build(),
                                    contentDescription = user.name ?: "User Avatar",
                                    modifier = Modifier.fillMaxSize(), // Fill the parent Box (which is already clipped)
                                    contentScale = ContentScale.Crop,
                                    onError = { error ->
                                        Log.e("ProfileScreenImgLoad", "Error loading image: $modelToLoad", error.result.throwable)
                                    }
                                )
                            } else {
                                // Fallback to placeholder Icon if no valid image data
                                Image(
                                    imageVector = Icons.Filled.Person, // Default placeholder
                                    contentDescription = user.name ?: "User Avatar Placeholder",
                                    modifier = Modifier.size(60.dp), // Adjust size of the icon within the circle
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(mainRed.copy(alpha = 0.7f)) // Tint for the placeholder
                                )
                            }
                        }
                    }
                    // Edit Icon Overlay (should be outside the key block, but inside the clickable Box)
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Change Picture",
                        tint = mainWhite.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .background(mainRed.copy(alpha = 0.7f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name & Phone
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Edit Name", color = mainWhite.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onNameUpdate(editedName)
                                isEditingName = false
                                keyboardController?.hide()
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = mainWhite,
                                unfocusedTextColor = mainWhite,
                                cursorColor = mainWhite,
                                focusedBorderColor = mainWhite,
                                unfocusedBorderColor = mainWhite.copy(alpha = 0.7f),
                                focusedLabelColor = mainWhite,
                                unfocusedLabelColor = mainWhite.copy(alpha = 0.7f)
                            )
                        )
                        IconButton(onClick = {
                            onNameUpdate(editedName)
                            isEditingName = false
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = "Save Name", tint = mainWhite)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isEditingName = true }
                    ) {
                        Text(
                            text = editedName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = mainWhite
                        )
                        IconButton(
                            onClick = { isEditingName = true },
                            modifier = Modifier.size(32.dp).padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit Name",
                                tint = mainWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                user.phone?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            color = mainWhite.copy(alpha = 0.85f),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { onCall(user) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mainWhite,
                        contentColor = mainRed
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(46.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                ) {
                    Text("Call", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Call History",
                fontWeight = FontWeight.Medium,
                fontSize = 19.sp,
                color = mainRed,
                modifier = Modifier.padding(start = 24.dp, bottom = 6.dp, top = 8.dp)
            )

            if (callLogs.isNotEmpty()) {
                Surface(
                    color = mainWhite.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    modifier = Modifier.fillMaxSize().padding(top = 6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp) // Horizontal padding handled by items now
                    ) {
                        val displayedLogs = if (callLogs.size > 50) callLogs.take(50) else callLogs
                        items(displayedLogs) { log ->
                            val callTypeString = when (log.callType) {
                                CallType.INCOMING -> "Incoming"
                                CallType.OUTGOING -> "Outgoing"
                                CallType.MISSED -> "Missed"
                            }
                            val durationString = formatDuration(log.duration)
                            val timeAgoString = formatTimeAgo(log.timestamp)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 16.dp) // Item specific horizontal padding
                                    .background(mainWhite.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    // UserAvatar(user = log.user, size = 40.dp)
                                    // Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${log.user.name} (${log.user.phone})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = mainRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = callTypeString, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(text = log.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) // Original message
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Duration: $durationString", fontSize = 12.sp, color = Color.Gray)
                                    Text(text = timeAgoString, fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(text = log.formattedDateTime, fontSize = 12.sp, color = Color.Gray)
                            }
                            Divider(color = mainRed.copy(alpha = 0.2f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No call history found.", color = mainWhite, fontSize = 16.sp)
                }
            }
        }
    }
}