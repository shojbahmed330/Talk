package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.ui.theme.CallLog
import com.example.realtimecalltranslation.ui.theme.CallType

@Composable
fun CallLogRow(
    log: CallLog,
    onProfile: (User) -> Unit,
    onCall: (User) -> Unit,
    mainRed: Color,
    accentRed: Color,
    lightRed: Color,
    cardBg: Color
) {
    val (icon, iconTint) = when {
        log.isMissed -> Icons.AutoMirrored.Filled.CallMissed to mainRed
        log.callType == CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to accentRed
        else -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF4CAF50)
    }

    val backgroundColor = when {
        log.isMissed -> lightRed
        log.callType == CallType.OUTGOING -> cardBg
        else -> cardBg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(backgroundColor, shape = MaterialTheme.shapes.medium)
            .clickable { onProfile(log.user) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar + Name + Message
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                user = log.user,
                size = 40.dp,
                modifier = Modifier.clickable { onProfile(log.user) }
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = log.user.name,
                    color = mainRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = log.message,
                    color = accentRed,
                    fontSize = 13.sp
                )
                if (log.formattedDateTime.isNotBlank()) {
                    Text(
                        text = log.formattedDateTime,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        IconButton(
            onClick = { onCall(log.user) }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Call",
                tint = iconTint
            )
        }
    }
}