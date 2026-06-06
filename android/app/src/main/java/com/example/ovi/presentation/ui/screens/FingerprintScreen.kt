package com.example.ovi.presentation.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.data.dto.device.FingerprintDto
import com.example.ovi.presentation.viewmodel.EnrollState
import com.example.ovi.presentation.viewmodel.FingerprintViewModel
import com.example.ovi.ui.theme.*

@Composable
fun FingerprintScreen(
    deviceId: String,
    lockName: String,
    onBack: () -> Unit,
    viewModel: FingerprintViewModel = hiltViewModel()
) {
    val fingerprints by viewModel.fingerprints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val enrollState by viewModel.enrollState.collectAsState()

    var showEnrollDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FingerprintDto?>(null) }
    var deleteTarget by remember { mutableStateOf<FingerprintDto?>(null) }

    LaunchedEffect(deviceId) { viewModel.load(deviceId) }

    LaunchedEffect(enrollState) {
        if (enrollState is EnrollState.Success) {
            showEnrollDialog = true
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Text(
                    text = "Fingerprints",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (fingerprints.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No fingerprints enrolled",
                                    color = TextHint,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Tap + to add one",
                                    color = TextHint,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(fingerprints, key = { it.fingerId }) { fp ->
                    FingerprintItem(
                        fp = fp,
                        onRename = { renameTarget = fp },
                        onDelete = { deleteTarget = fp }
                    )
                }
            }

            Button(
                onClick = { showEnrollDialog = true; viewModel.startEnroll(deviceId) },
                enabled = enrollState is EnrollState.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 88.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(Modifier.width(8.dp))
                Text("Add Fingerprint", color = White, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showEnrollDialog) {
        EnrollDialog(
            state = enrollState,
            onDismiss = {
                showEnrollDialog = false
                viewModel.cancelEnroll()
            },
            onNameSave = { name ->
                val fid = (enrollState as? EnrollState.Success)?.fingerId ?: return@EnrollDialog
                viewModel.rename(deviceId, fid, name)
                showEnrollDialog = false
                viewModel.cancelEnroll()
            }
        )
    }

    renameTarget?.let { fp ->
        RenameDialog(
            current = fp.name,
            onDismiss = { renameTarget = null },
            onSave = { name ->
                viewModel.rename(deviceId, fp.fingerId, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { fp ->
        DeleteConfirmDialog(
            name = fp.name.ifEmpty { "Fingerprint #${fp.fingerId}" },
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(deviceId, fp.fingerId)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun FingerprintItem(
    fp: FingerprintDto,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
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
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fp.name.ifEmpty { "Fingerprint #${fp.fingerId}" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = White
                )
                Text(
                    text = "Slot ${fp.fingerId}",
                    fontSize = 12.sp,
                    color = TextHint
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = White.copy(alpha = 0.7f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun EnrollDialog(
    state: EnrollState,
    onDismiss: () -> Unit,
    onNameSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (state is EnrollState.Idle || state is EnrollState.Success || state is EnrollState.Failure) onDismiss() },
        containerColor = Color(0xFF1E5A8A),
        title = {
            Text(
                text = when (state) {
                    is EnrollState.Success -> "Fingerprint Enrolled!"
                    is EnrollState.Failure -> "Enrollment Failed"
                    else -> "Enrolling Fingerprint"
                },
                color = White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    is EnrollState.InProgress -> EnrollProgressContent(state)
                    is EnrollState.Success -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Fingerprint saved successfully!", color = White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name (optional)", color = TextHint) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onNameSave(name) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = White,
                                unfocusedBorderColor = BorderUnfocused,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is EnrollState.Failure -> {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.reason, color = White, textAlign = TextAlign.Center)
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (state) {
                is EnrollState.Success -> TextButton(onClick = { onNameSave(name) }) {
                    Text("Save", color = White, fontWeight = FontWeight.SemiBold)
                }
                is EnrollState.Failure -> TextButton(onClick = onDismiss) {
                    Text("Close", color = White)
                }
                is EnrollState.InProgress -> TextButton(onClick = onDismiss) {
                    Text("Cancel", color = White.copy(alpha = 0.7f))
                }
                else -> {}
            }
        }
    )
}

@Composable
private fun EnrollProgressContent(state: EnrollState.InProgress) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        val steps = listOf("Place finger", "Lift finger", "Place again")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEachIndexed { idx, _ ->
                val done = state.step > idx + 1
                val active = state.step == idx + 1
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done   -> Green
                                active -> White
                                else   -> White.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.message,
            color = White,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        if (state.step == 0) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = White,
                trackColor = White.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E5A8A),
        title = { Text("Rename Fingerprint", color = White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = TextHint) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = White,
                    unfocusedBorderColor = BorderUnfocused,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text("Save", color = White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextHint) }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E5A8A),
        title = { Text("Delete Fingerprint", color = White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Delete \"$name\"? This cannot be undone.",
                color = White.copy(alpha = 0.9f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Red, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextHint) }
        }
    )
}
