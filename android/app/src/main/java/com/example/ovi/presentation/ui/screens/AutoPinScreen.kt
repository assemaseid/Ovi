package com.example.ovi.presentation.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.AutoPinViewModel
import com.example.ovi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val INTERVAL_OPTIONS = listOf(1, 2, 4, 6, 12, 24, 48, 72, 168)

private fun intervalLabel(hours: Int): String = when {
    hours < 24 -> "${hours}h"
    hours % 168 == 0 -> "${hours / 168}w"
    hours % 24 == 0 -> "${hours / 24}d"
    else -> "${hours}h"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPinScreen(
    lockId: String,
    lockName: String,
    isOwner: Boolean,
    onBack: () -> Unit,
    viewModel: AutoPinViewModel = hiltViewModel()
) {
    val currentPin by viewModel.currentPin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pinError by viewModel.error.collectAsState()
    val schedule by viewModel.schedule.collectAsState()

    var pinVisible by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    // Schedule editor state
    var scheduleEnabled by remember { mutableStateOf(false) }
    var intervalHours by remember { mutableStateOf(24) }
    var nextRotationCal by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }) }
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    var scheduleDirty by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Server stores/compares all datetimes in UTC — always format in UTC when sending,
    // and parse in UTC when receiving so that display converts correctly to local time.
    val utcFmt = remember {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
    }
    val displayFmt = remember { SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()) }

    LaunchedEffect(lockId) { viewModel.loadSchedule(lockId) }

    // Sync editor state when schedule loads
    LaunchedEffect(schedule) {
        schedule?.let { s ->
            scheduleEnabled = s.enabled
            intervalHours = s.rotation_interval_hours
            s.next_rotation_at?.let { iso ->
                runCatching {
                    // Parse as UTC so Calendar reflects correct local time
                    val date = utcFmt.parse(iso.substringBefore('+').substringBefore('Z').trimEnd())
                        ?: return@runCatching
                    nextRotationCal = Calendar.getInstance().apply { time = date }
                }
            }
        }
        scheduleDirty = false
    }

    LaunchedEffect(viewModel.saveSuccess) {
        viewModel.saveSuccess.collect { scheduleDirty = false }
    }

    LaunchedEffect(currentPin) { pinVisible = false }

    fun requestBiometric() {
        biometricError = null
        val activity = context as? FragmentActivity ?: run { pinVisible = true; return }
        val canAuth = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) { pinVisible = true; return }

        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { pinVisible = true }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                        biometricError = errString.toString()
                }
                override fun onAuthenticationFailed() { biometricError = "Authentication failed" }
            }
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm identity")
                .setSubtitle("Authenticate to view your PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        )
    }

    fun pickDateTime() {
        val cal = nextRotationCal
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                nextRotationCal = Calendar.getInstance().apply { set(year, month, day, hour, minute, 0) }
                scheduleDirty = true
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto PIN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
                    Text(lockName, fontSize = 13.sp, color = White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // ── Current PIN card ──────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            Text("CURRENT PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Black.copy(alpha = 0.2f))
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = White)
                            } else {
                                Text(
                                    text = if (pinVisible && currentPin != null) currentPin!! else "••••••",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    letterSpacing = 8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { if (pinVisible) pinVisible = false else requestBiometric() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                enabled = !isLoading && currentPin != null,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
                            ) {
                                Icon(if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (pinVisible) "Hide" else "Show", fontSize = 13.sp)
                            }

                            if (isOwner) {
                                Button(
                                    onClick = { pinVisible = false; viewModel.refreshPin(lockId) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh", fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }

                        (biometricError ?: pinError)?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(err, fontSize = 12.sp, color = Color(0xFFEF9A9A), modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val nextMillis = remember(intervalHours) { viewModel.getNextRotationMillis(intervalHours) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(White.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = White.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Next TOTP slot: ${formatNextRotation(nextMillis)}",
                                fontSize = 13.sp,
                                color = White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                if (isOwner) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Auto Rotation Settings card ──────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                Text("ROTATION SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
                            }

                            // Enable toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(White.copy(alpha = 0.07f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Auto Rotation", fontSize = 14.sp, color = White.copy(alpha = 0.9f), fontWeight = FontWeight.Medium)
                                    Text("Server rotates PIN on schedule", fontSize = 11.sp, color = White.copy(alpha = 0.4f))
                                }
                                Switch(
                                    checked = scheduleEnabled,
                                    onCheckedChange = { scheduleEnabled = it; scheduleDirty = true },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = White,
                                        checkedTrackColor = AccentBlue,
                                        uncheckedThumbColor = White.copy(alpha = 0.5f),
                                        uncheckedTrackColor = White.copy(alpha = 0.15f)
                                    )
                                )
                            }

                            if (scheduleEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Interval picker
                                Text("Rotation interval", fontSize = 13.sp, color = White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = intervalMenuExpanded,
                                    onExpandedChange = { intervalMenuExpanded = !intervalMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = intervalLabel(intervalHours),
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalMenuExpanded) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedTextColor = White,
                                            focusedTextColor = White,
                                            unfocusedBorderColor = White.copy(alpha = 0.2f),
                                            focusedBorderColor = AccentBlue,
                                            unfocusedContainerColor = White.copy(alpha = 0.07f),
                                            focusedContainerColor = White.copy(alpha = 0.07f),
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = intervalMenuExpanded,
                                        onDismissRequest = { intervalMenuExpanded = false }
                                    ) {
                                        INTERVAL_OPTIONS.forEach { h ->
                                            DropdownMenuItem(
                                                text = { Text(intervalLabel(h)) },
                                                onClick = { intervalHours = h; intervalMenuExpanded = false; scheduleDirty = true }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Next rotation date-time picker
                                Text("Next rotation at", fontSize = 13.sp, color = White.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(White.copy(alpha = 0.07f))
                                        .clickable { pickDateTime() }
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = displayFmt.format(nextRotationCal.time),
                                        fontSize = 14.sp,
                                        color = White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(Icons.Default.EditCalendar, contentDescription = "Pick date", tint = AccentBlue, modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save button
                            Button(
                                onClick = {
                                    viewModel.saveSchedule(
                                        lockId,
                                        scheduleEnabled,
                                        intervalHours,
                                        utcFmt.format(nextRotationCal.time)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (scheduleDirty) AccentBlue else White.copy(alpha = 0.15f)
                                ),
                                enabled = !isLoading && scheduleDirty
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = White)
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Schedule", fontSize = 14.sp, color = White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

private fun formatNextRotation(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    return when {
        diff < 60 * 60 * 1000 -> "in ${diff / (60 * 1000)} min"
        diff < 24 * 60 * 60 * 1000 -> "in ${diff / (60 * 60 * 1000)}h"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
