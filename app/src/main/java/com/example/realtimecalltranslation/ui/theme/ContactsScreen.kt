package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.Context
import android.net.Uri // Added import
import android.provider.ContactsContract
import android.util.Log // Added import
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

// Removed: import com.example.realtimecalltranslation.ui.theme.*
// Assuming ContactListItem, mainRed, accentRed etc. are available through other means
// (e.g. ContactListItem in same package, Colors defined in Theme.kt or Color.kt and accessible)

data class Contact(val name: String, val phone: String)

// Removed data class ContactDetails - will use the one from CallModels.kt

fun fetchContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use { cur ->
        val nameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cur.moveToNext()) {
            val name = cur.getString(nameIndex) ?: "Unknown"
            val number = cur.getString(numberIndex) ?: "--"
            contacts.add(Contact(name, number))
        }
    }
    return contacts
}

// Removed fun getContactDetailsByNumber - will use the one from CallModels.kt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ContactsScreen(
    onBack: () -> Unit = {},
    onCallContact: (String) -> Unit = {},
    mainRed: Color,
    accentRed: Color,
    mainWhite: Color,
    mainGreen: Color,
    lightGreen: Color,
    lightRed: Color
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = Manifest.permission.READ_CONTACTS)
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var expandedContact by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(permissionState.status) {
        if (permissionState.status is PermissionStatus.Granted) {
            contacts = fetchContacts(context)
        }
    }

    val filteredContacts = if (searchText.isBlank()) contacts
    else contacts.filter { it.name.contains(searchText, ignoreCase = true) || it.phone.contains(searchText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainWhite)
    ) {
        // TopBar with Back + Search
        Surface(
            color = mainRed,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentRed,
                            contentColor = mainWhite
                        ),
                        shape = CircleShape
                    ) {
                        Text("Back")
                    }
                    Text(
                        text = "Contacts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = mainWhite,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search contacts", color = accentRed) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentRed,
                        unfocusedBorderColor = mainRed,
                        cursorColor = accentRed,
                        focusedTextColor = mainRed,
                        unfocusedTextColor = mainRed
                        // backgroundColor = mainWhite, // Removed as M3 OutlinedTextFieldDefaults.colors doesn't have direct backgroundColor
                    )
                )
            }
        }

        when {
            permissionState.status !is PermissionStatus.Granted -> {
                Spacer(modifier = Modifier.height(60.dp))
                Button(
                    onClick = { permissionState.launchPermissionRequest() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mainRed,
                        contentColor = mainWhite
                    )
                ) {
                    Text("Allow Contacts Permission")
                }
            }
            contacts.isEmpty() -> {
                Spacer(modifier = Modifier.height(60.dp))
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = mainRed
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(mainWhite)
                ) {
                    items(filteredContacts) { contact ->
                        ContactListItem(
                            contact = contact,
                            isExpanded = expandedContact == contact.phone,
                            onClick = {
                                expandedContact = if (expandedContact == contact.phone) null else contact.phone
                            },
                            onCall = { onCallContact(contact.phone) },
                            mainWhite = mainWhite,
                            mainGreen = mainGreen,
                            lightGreen = lightGreen,
                            accentRed = accentRed,
                            mainRed = mainRed
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}