package com.example.realtimecalltranslation.ui // Ensure correct package

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
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.realtimecalltranslation.ui.theme.CallLogRow
import com.example.realtimecalltranslation.ui.theme.User

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
    imageDataSource: Any?
) {
    Log.d("ProfileScreenInit", "Composing ProfileScreen for User: ${user.name}, Initial imageDataSource: $imageDataSource")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("ProfileScreenPicker", "Image URI selected by picker: ${uri?.toString()}")
        onProfilePicUriSelected(uri?.toString())
    }

    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable(user.name) { mutableStateOf(user.name) }
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
                        .size(110.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                ) {
                    key(imageDataSource) { // ***** KEY BLOCK ADDED *****
                        Box(modifier = Modifier.fillMaxSize()) { // Inner Box for content + overlay
                            if (imageDataSource is ByteArray || (imageDataSource is String && imageDataSource.isNotBlank())) {
                                val imageRequest = if (imageDataSource is ByteArray) {
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(imageDataSource)
                                        .size(Size(256, 256)) // Explicit size for byte array
                                        // No .crossfade(true) for ByteArray for this test
                                        .build()
                                } else { // Assumed to be String URI or other Coil-compatible model
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(imageDataSource)
                                        .crossfade(true) // Keep crossfade for URLs/other types
                                        .size(Size(256, 256)) // Explicit size
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest, // Use the conditionally built request
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(mainWhite, CircleShape)
                                        .border(4.dp, mainWhite, CircleShape),
                                    contentScale = ContentScale.Crop,
                                    onState = { state: AsyncImagePainter.State ->
                                        val modelTypeForLog = if (imageDataSource is ByteArray) {
                                            "ByteArray[size=${imageDataSource.size}]"
                                        } else {
                                            imageDataSource?.toString() ?: "null"
                                        }
                                        when (state) {
                                            is AsyncImagePainter.State.Loading -> Log.d("AsyncImageState", "Model: $modelTypeForLog -> State: Loading")
                                            is AsyncImagePainter.State.Success -> Log.d("AsyncImageState", "Model: $modelTypeForLog -> State: Success")
                                            is AsyncImagePainter.State.Error -> Log.e("ProfileScreenImgLoad", "Error (via onState) with Model: $modelTypeForLog", state.result.throwable)
                                            is AsyncImagePainter.State.Empty -> Log.d("AsyncImageState", "Model: $modelTypeForLog -> State: Empty")
                                        }
                                    }
                                )
                            } else {
                                // Fallback Person Icon
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(mainWhite)
                                        .border(4.dp, mainWhite, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Default Avatar",
                                        modifier = Modifier.size(70.dp),
                                        tint = mainRed
                                    )
                                }
                            }
                            // Edit Icon Overlay (inside key block)
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
                        } // ***** END INNER BOX *****
                    } // ***** END KEY BLOCK *****
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
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        items(callLogs) { log ->
                            CallLogRow(
                                log = log,
                                onProfile = {},
                                onCall = onCall,
                                mainRed = mainRed,
                                accentRed = mainRed,
                                lightRed = mainWhite,
                                cardBg = mainWhite
                            )
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
