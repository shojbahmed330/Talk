package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad // Changed from Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Import cardBg if it's defined in Color.kt in this package, or ensure it's passed if not.
// Assuming it's available via wildcard import com.example.realtimecalltranslation.ui.theme.*
// If not, specific import: import com.example.realtimecalltranslation.ui.theme.cardBg

@Composable
fun CallHistoryScreen(
    callLogs: List<CallLog>,
    onProfile: (User) -> Unit,
    onCall: (User) -> Unit,
    onUserAvatar: (User) -> Unit,
    onFavourites: () -> Unit,
    onDialer: () -> Unit,
    onContacts: () -> Unit,
    selectedNav: Int,
    mainRed: Color,
    mainWhite: Color,
    accentRed: Color,
    lightRed: Color
    // cardBg is now directly used from theme import
) {
    var search by remember { mutableStateOf("") }
    var activeTab by rememberSaveable { mutableStateOf(CallType.INCOMING) }

    val filteredLogs = callLogs
        .filter { it.callType == activeTab || (it.isMissed && activeTab == CallType.MISSED) }
        .filter { search.isBlank() || it.user.name.contains(search, ignoreCase = true) }
        .take(50) // Take the first 50 from the already newest-first sorted and filtered list

    val recentUsers = callLogs
        .filter { it.callType == activeTab || (it.isMissed && activeTab == CallType.MISSED) }
        .asReversed()
        .distinctBy { it.user.id }
        .take(6)
        .map { it.user }

    Column(
        Modifier
            .fillMaxSize()
            .background(mainWhite)
    ) {
        // Top AppBar
        Surface(
            shadowElevation = 4.dp,
            color = mainRed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Call History",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainWhite
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            placeholder = { Text("Search by name", color = accentRed) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = mainRed,
                unfocusedBorderColor = accentRed,
                cursorColor = mainRed,
                focusedLabelColor = accentRed,
                unfocusedLabelColor = accentRed
            ),
            textStyle = androidx.compose.ui.text.TextStyle(color = mainRed)
        )

        Spacer(Modifier.height(10.dp))

        if (recentUsers.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                recentUsers.forEach { user ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatar(
                            user = user,
                            size = 48.dp,
                            modifier = Modifier
                                .border(2.dp, mainRed, CircleShape)
                                .clickable { onUserAvatar(user) }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            user.name,
                            fontSize = 12.sp,
                            color = accentRed,
                            modifier = Modifier.clickable { onUserAvatar(user) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterTab(
                title = "Incoming",
                selected = activeTab == CallType.INCOMING,
                onClick = { activeTab = CallType.INCOMING },
                mainRed = mainRed,
                mainWhite = mainWhite,
                accentRed = accentRed,
                lightRed = lightRed,
                modifier = Modifier.width(125.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterTab(
                title = "Outgoing",
                selected = activeTab == CallType.OUTGOING,
                onClick = { activeTab = CallType.OUTGOING },
                mainRed = mainRed,
                mainWhite = mainWhite,
                accentRed = accentRed,
                lightRed = lightRed,
                modifier = Modifier.width(125.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterTab(
                title = "Missed",
                selected = activeTab == CallType.MISSED,
                onClick = { activeTab = CallType.MISSED },
                mainRed = mainRed,
                mainWhite = mainWhite,
                accentRed = accentRed,
                lightRed = lightRed,
                modifier = Modifier.width(125.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = cardBg
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (search.isBlank()) "No ${activeTab.name.lowercase()} calls." else "No results found for \"$search\".",
                        fontSize = 18.sp,
                        color = accentRed
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(filteredLogs) { log ->
                        CallLogRow(
                            log = log,
                            onProfile = onProfile,
                            onCall = onCall,
                            mainRed = mainRed,
                            accentRed = accentRed,
                            lightRed = lightRed,
                            cardBg = mainWhite
                        )
                    }
                }
            }
        }

        Surface(
            shadowElevation = 8.dp,
            color = cardBg, // Changed background for the bottom navigation bar surface
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onContacts,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (selectedNav == 2) mainRed else mainWhite,
                            CircleShape
                        )
                        .border(
                            width = if (selectedNav == 2) 2.dp else 1.dp,
                            color = if (selectedNav == 2) mainWhite else lightRed,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Contacts",
                        tint = if (selectedNav == 2) mainWhite else accentRed,
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(
                    onClick = onDialer,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (selectedNav == 1) mainRed else mainWhite,
                            CircleShape
                        )
                        .border(
                            width = if (selectedNav == 1) 2.dp else 1.dp,
                            color = if (selectedNav == 1) mainWhite else lightRed,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Dialpad,
                        contentDescription = "Dialpad",
                        tint = if (selectedNav == 1) mainWhite else accentRed,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(
                    onClick = onFavourites,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (selectedNav == 0) mainRed else mainWhite,
                            CircleShape
                        )
                        .border(
                            width = if (selectedNav == 0) 2.dp else 1.dp,
                            color = if (selectedNav == 0) mainWhite else lightRed,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Favourites",
                        tint = if (selectedNav == 0) mainWhite else accentRed,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}