package com.example.realtimecalltranslation.ui.theme

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call // Added import for Call icon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

// Colors are passed from MainNavigation; no local color definitions!

data class FavouriteContact(
    val id: String,
    val name: String,
    val phone: String
)

class FavouritesRepository(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("favourites_prefs", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()
    private val favouritesKey = "favourite_contacts_list"

    private var _favourites = mutableStateListOf<FavouriteContact>()
    val favourites: List<FavouriteContact> get() = _favourites

    init {
        loadFavourites()
    }

    private fun loadFavourites() {
        val json = sharedPreferences.getString(favouritesKey, null)
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<FavouriteContact>>() {}.type
            val loadedFavourites: List<FavouriteContact> = gson.fromJson(json, type)
            _favourites.addAll(loadedFavourites)
        }
    }

    private fun saveFavourites() {
        val json = gson.toJson(_favourites)
        sharedPreferences.edit().putString(favouritesKey, json).apply()
    }

    fun add(contact: FavouriteContact) {
        if (_favourites.none { it.id == contact.id }) {
            _favourites.add(contact)
            saveFavourites()
        }
    }

    fun remove(contact: FavouriteContact) {
        if (_favourites.removeAll { it.id == contact.id }) {
            saveFavourites()
        }
    }

    fun clear() {
        if (_favourites.isNotEmpty()) {
            _favourites.clear()
            saveFavourites()
        }
    }
}

fun loadRealContacts(context: Context): List<FavouriteContact> {
    val list = mutableListOf<FavouriteContact>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        != PackageManager.PERMISSION_GRANTED
    ) return emptyList()
    val cr = context.contentResolver
    val cursor = cr.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC"
    )
    cursor?.use {
        val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
        val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val unique = mutableSetOf<String>()
        while (it.moveToNext()) {
            val id = it.getString(idIdx) ?: continue
            if (unique.contains(id)) continue
            unique.add(id)
            list.add(
                FavouriteContact(
                    id = id,
                    name = it.getString(nameIdx) ?: "",
                    phone = it.getString(phoneIdx) ?: ""
                )
            )
        }
    }
    return list
}

private val demoContacts = listOf(
    FavouriteContact("1", "Demo Baba", "01711XXXXXX"),
    FavouriteContact("2", "Demo Ma", "01811XXXXXX"),
    FavouriteContact("3", "Demo Friend", "01911XXXXXX")
)

@Composable
fun FavouritesScreen(
    onBack: () -> Unit = {},
    favouritesRepository: FavouritesRepository, // Added parameter
    onCall: (String) -> Unit, // Added callback for calling
    mainRed: Color,
    mainWhite: Color,
    accentRed: Color,
    lightRed: Color
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var contactList by remember { mutableStateOf<List<FavouriteContact>>(emptyList()) }
    // Removed isReal state as FavouritesRepository now handles persistence

    LaunchedEffect(Unit) { // Load contacts for the "Add Favourite" dialog
        contactList = if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadRealContacts(context)
        } else {
            demoContacts // Fallback to demo contacts if permission not granted
        }
    }

    // The FavouritesRepository instance is now passed, so no need to remember it here.
    // The list of favourites is directly observed from the repository.
    val favouritesList by remember { derivedStateOf { favouritesRepository.favourites } }

    Column(
        Modifier
            .fillMaxSize()
            .background(mainWhite)
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(mainRed)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Favourites",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = mainWhite
            )
            Button(
                onClick = { showAddDialog = true },
                enabled = contactList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentRed,
                    contentColor = mainWhite,
                    disabledContainerColor = lightRed,
                    disabledContentColor = accentRed
                )
            ) {
                Text("Add")
            }
        }

        // Back Button
        Button(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                .align(Alignment.Start),
            colors = ButtonDefaults.buttonColors(
                containerColor = mainRed,
                contentColor = mainWhite
            ),
            shape = CircleShape
        ) {
            Text("Back")
        }

        if (favouritesList.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    "No favourites added yet.", // Simplified text
                    color = accentRed,
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 12.dp)
            ) {
                items(favouritesList.size) { idx ->
                    val contact = favouritesList[idx]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(lightRed, shape = CircleShape)
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = mainRed,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = accentRed
                            )
                            Text(
                                contact.phone,
                                fontSize = 15.sp,
                                color = mainRed
                            )
                        }
                        // Call Button
                        IconButton(onClick = { onCall(contact.phone) }) { // Use onCall callback
                            Icon(
                                imageVector = Icons.Filled.Call, // Assuming Icons.Filled.Call is available
                                contentDescription = "Call ${contact.name}",
                                tint = mainRed // Or another appropriate color
                            )
                        }
                        Spacer(Modifier.width(8.dp)) // Add some space between buttons
                        // Delete Button
                        IconButton(onClick = { favouritesRepository.remove(contact) }) { // Use favouritesRepository
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remove ${contact.name}",
                                tint = accentRed
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddFavouriteDialog(
                contactList = contactList,
                favouritesList = favouritesList, // This is fine for checking if already added
                onAdd = { contact ->
                    favouritesRepository.add(contact) // Corrected: use favouritesRepository
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false },
                mainRed = mainRed,
                mainWhite = mainWhite,
                accentRed = accentRed,
                lightRed = lightRed
            )
        }
    }
}

@Composable
fun AddFavouriteDialog(
    contactList: List<FavouriteContact>,
    favouritesList: List<FavouriteContact>,
    onAdd: (FavouriteContact) -> Unit,
    onDismiss: () -> Unit,
    mainRed: Color,
    mainWhite: Color,
    accentRed: Color,
    lightRed: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = mainWhite,
        title = {
            Text(
                "Add Favourite Contact",
                color = mainRed,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (contactList.isEmpty()) {
                Text("No contacts found.", color = accentRed)
            } else {
                Column(Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(contactList.size) { idx ->
                            val contact = contactList[idx]
                            val alreadyAdded = favouritesList.any { it.id == contact.id }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !alreadyAdded
                                    ) { onAdd(contact) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp)
                                    .background(
                                        if (alreadyAdded) mainWhite else lightRed,
                                        shape = CircleShape
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = mainRed,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(contact.name, fontSize = 16.sp, color = mainRed)
                                    Text(contact.phone, fontSize = 13.sp, color = accentRed)
                                }
                                if (alreadyAdded) {
                                    Text("Added", color = accentRed, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = mainRed)
            ) { Text("Close") }
        }
    )
}