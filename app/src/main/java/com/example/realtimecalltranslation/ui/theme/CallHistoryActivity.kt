package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class CallHistoryActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permission granted. Reopen to refresh call logs.", Toast.LENGTH_SHORT).show()
            recreate() // Reopen activity to refresh content
        } else {
            Toast.makeText(this, "Permission denied! Can't show real call logs.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }

        // Set Compose UI
        setContent {
            CallHistoryScreenContent()
        }
    }
}

@Composable
fun CallHistoryScreenContent() {
    val context = LocalContext.current

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    val callLogs = if (hasPermission) getRealCallLogs(context) else listOf(
        // Fallback demo call logs
        CallLog(User("1", "Demo User", "017XXXXXXXX", null), "Demo incoming", CallType.INCOMING, false, "5 min ago"),
        CallLog(User("2", "Missed Caller", "018XXXXXXXX", null), "Missed call", CallType.MISSED, true, "10 min ago"),
        CallLog(User("3", "Outgoing Guy", "019XXXXXXXX", null), "Outgoing call", CallType.OUTGOING, false, "15 min ago")
    )

    // Use activeUsers if needed in CallHistoryScreen, otherwise remove
    val activeUsers = callLogs.map { it.user }.distinctBy { it.id }

    CallHistoryScreen(
        callLogs = callLogs,
        onProfileClick = {}, // Placeholder for profile click
        onCallClick = {},   // Placeholder for call click
        onUserAvatarClick = {}, // Placeholder for avatar click
        onFavouritesClick = {}, // Placeholder for favourites click
        onDialerClick = {}, // Placeholder for dialer click
        onContactsClick = {}, // Placeholder for contacts click
        selectedNav = 0,
        mainRed = Color.Red,
        mainWhite = Color.White,
        accentRed = Color(0xFFFF4444),
        lightRed = Color(0xFFFF9999)
    )
}

// Data classes and functions
data class CallLog(val user: User, val title: String, val type: CallType, val isMissed: Boolean, val time: String)
data class User(val id: String, val name: String, val phone: String, val avatar: String?)
enum class CallType { INCOMING, MISSED, OUTGOING }

fun getRealCallLogs(context: android.content.Context): List<CallLog> {
    // Placeholder for real call log retrieval
    return emptyList() // Replace with actual ContentResolver logic
}

// Updated CallHistoryScreen Composable definition
@Composable
fun CallHistoryScreen(
    callLogs: List<CallLog>,
    onProfileClick: () -> Unit,
    onCallClick: () -> Unit,
    onUserAvatarClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    onDialerClick: () -> Unit,
    onContactsClick: () -> Unit,
    selectedNav: Int,
    mainRed: Color,
    mainWhite: Color,
    accentRed: Color,
    lightRed: Color
) {
    // Implement your UI logic here
    // Example: Display call logs
    androidx.compose.material3.Text(
        text = "Call Logs: ${callLogs.size}",
        color = mainRed
    )
    // Add more UI elements as needed
}