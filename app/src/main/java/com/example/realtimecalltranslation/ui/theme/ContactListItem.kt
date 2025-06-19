package com.example.realtimecalltranslation.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContactListItem(
    contact: Contact,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onCall: () -> Unit,
    mainWhite: Color,
    mainGreen: Color,
    lightGreen: Color,
    accentRed: Color,
    mainRed: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) lightGreen else mainWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Contact Avatar",
                tint = if (isExpanded) mainGreen else accentRed,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(mainWhite)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = if (isExpanded) mainGreen else mainRed
                )
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.phone,
                        fontSize = 15.sp,
                        color = mainGreen,
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Button(
                            onClick = onCall,
                            colors = ButtonDefaults.buttonColors(containerColor = mainGreen),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Call", color = mainWhite, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}