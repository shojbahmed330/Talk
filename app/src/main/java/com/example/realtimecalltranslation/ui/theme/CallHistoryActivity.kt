package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat // Added import
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.realtimecalltranslation.ui.theme.User
import com.example.realtimecalltranslation.ui.theme.CallType
import com.example.realtimecalltranslation.ui.theme.CallLog

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

    val now = System.currentTimeMillis()
    val callLogs = if (hasPermission) getRealCallLogs(context) else listOf(
        CallLog(
            user = User("1", "Demo User", "017XXXXXXXX", null),
            message = "Incoming Call", // Simplified
            callType = CallType.INCOMING,
            isMissed = false,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 300000L).toString(),
            timestamp = now - 300000L,
            duration = 60L
        ),
        CallLog(
            user = User("2", "Missed Caller", "018XXXXXXXX", null),
            message = "Missed Call", // Simplified
            callType = CallType.MISSED,
            isMissed = true,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 600000L).toString(),
            timestamp = now - 600000L,
            duration = 0L
        ),
        CallLog(
            user = User("3", "Outgoing Guy", "019XXXXXXXX", null),
            message = "Outgoing Call", // Simplified
            callType = CallType.OUTGOING,
            isMissed = false,
            formattedDateTime = DateFormat.format("dd MMM, h:mm a", now - 900000L).toString(),
            timestamp = now - 900000L,
            duration = 120L
        )
    )

    // Use activeUsers if needed in CallHistoryScreen, otherwise remove
    val activeUsers = callLogs.map { it.user }.distinctBy { it.id }

    CallHistoryScreen(
        callLogs = callLogs,
        onProfile = {}, // Placeholder for profile click
        onCall = {},   // Placeholder for call click
        onUserAvatar = {}, // Placeholder for avatar click
        onFavourites = {}, // Placeholder for favourites click
        onDialer = {}, // Placeholder for dialer click
        onContacts = {}, // Placeholder for contacts click
        selectedNav = 0,
        mainRed = Color.Red, // Consider using theme colors
        mainWhite = Color.White, // Consider using theme colors
        accentRed = Color(0xFFFF4444), // Consider using theme colors
        lightRed = Color(0xFFFF9999)  // Consider using theme colors
    )
}

// Data classes and functions