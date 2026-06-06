package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.data.dto.grant.GuestDto
import com.example.ovi.presentation.viewmodel.GuestManagementViewModel
import com.example.ovi.ui.theme.*

@Composable
fun GuestManagementScreen(
    deviceId: String,
    lockName: String,
    onBack: () -> Unit,
    viewModel: GuestManagementViewModel = hiltViewModel()
) {
    val guests by viewModel.guests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var revokeTarget by remember { mutableStateOf<GuestDto?>(null) }

    LaunchedEffect(deviceId) { viewModel.load(deviceId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Guests", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                    Text(lockName, fontSize = 13.sp, color = White.copy(alpha = 0.5f))
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            error?.let {
                Text(
                    text = it,
                    color = Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (guests.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text("No guests yet", color = TextHint, fontSize = 15.sp, textAlign = TextAlign.Center)
                                Text(
                                    "Guests can request access via the app",
                                    color = TextHint,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(guests, key = { it.grantUuid }) { guest ->
                    GuestItem(
                        guest = guest,
                        onRevoke = { revokeTarget = guest }
                    )
                }
            }
        }
    }

    revokeTarget?.let { guest ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            containerColor = androidx.compose.ui.graphics.Color(0xFF1E5A8A),
            title = { Text("Revoke Access", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove access for ${guest.userName ?: guest.userEmail ?: "this user"}?",
                    color = White.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeGuest(deviceId, guest.grantUuid)
                    revokeTarget = null
                }) {
                    Text("Revoke", color = Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) {
                    Text("Cancel", color = TextHint)
                }
            }
        )
    }
}

@Composable
private fun GuestItem(guest: GuestDto, onRevoke: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = White.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guest.userName ?: "Guest",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = White
                )
                Text(
                    text = guest.userEmail ?: "",
                    fontSize = 12.sp,
                    color = TextHint
                )
                val permLabel = when {
                    "lock" in guest.permissions -> "Unlock & Lock"
                    "unlock" in guest.permissions -> "Unlock only"
                    else -> "Read only"
                }
                Text(text = permLabel, fontSize = 11.sp, color = AccentBlue.copy(alpha = 0.8f))
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Default.PersonRemove, contentDescription = "Revoke", tint = Red.copy(alpha = 0.8f))
            }
        }
    }
}
