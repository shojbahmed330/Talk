package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.realtimecalltranslation.ui.getRealCallLogs

class CallHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permission if not granted
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG),
                1001
            )
        }

        // Set Compose UI
        setContent {
            CallHistoryScreenContent()
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted. Reopen to refresh call logs.", Toast.LENGTH_SHORT).show()
        } else if (requestCode == 1001) {
            Toast.makeText(this, "Permission denied! Can't show real call logs.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun CallHistoryScreenContent() {
    val context = LocalContext.current

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    val callLogs = if (hasPermission) getRealCallLogs(context) else listOf(
        // Fallback demo call logs
        CallLog(User("1", "Demo User", "017XXXXXXXX", null), "Demo incoming", CallType.INCOMING, false, "5 min ago"),
        CallLog(User("2", "Missed Caller", "018XXXXXXXX", null), "Missed call", CallType.MISSED, true, "10 min ago"),
        CallLog(User("3", "Outgoing Guy", "019XXXXXXXX", null), "Outgoing call", CallType.OUTGOING, false, "15 min ago")
    )

    val activeUsers = callLogs.map { it.user }.distinctBy { it.id }

    CallHistoryScreen(
        callLogs = callLogs,
        onProfile = { /* handle profile click */ },
        onCall = { /* handle call */ },
        onAddNew = { /* handle add new */ },
        onUserAvatar = { /* handle avatar click */ },
        onFavourites = { /* handle favourites */ },
        onDialer = { /* handle dialer */ },
        onContacts = { /* handle contacts */ },
        selectedNav = 0,
        mainRed = mainRed,
        mainWhite = mainWhite,
        accentRed = accentRed,
        lightRed = lightRed
    )
}
