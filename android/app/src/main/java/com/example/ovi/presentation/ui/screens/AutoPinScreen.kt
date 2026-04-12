package com.example.ovi.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.AutoPinViewModel
import com.example.ovi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPinScreen(
    lockId: String,
    lockName: String,
    onBack: () -> Unit,
    viewModel: AutoPinViewModel = hiltViewModel()
) {
    val fetchedPin by viewModel.currentPin.collectAsState()
    val isLoadingPin by viewModel.isLoading.collectAsState()
    val pinError by viewModel.error.collectAsState()

    var currentPin by remember { mutableStateOf("••••••") }
    var pinVisible by remember { mutableStateOf(false) }

    LaunchedEffect(fetchedPin) {
        fetchedPin?.let {
            currentPin = it
            pinVisible = true
        }
    }

    var selectedHours by remember { mutableStateOf(6) }
    var showOnDisplay by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    val nextRotation = remember(selectedHours) {
        System.currentTimeMillis() + selectedHours * 60 * 60 * 1000L
    }

    val rotationOptions = listOf(1, 2, 6, 12, 24)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto PIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = lockName,
                        fontSize = 13.sp,
                        color = TextWhite.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
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
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = TextWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CURRENT PIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite.copy(alpha = 0.5f),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (pinVisible) currentPin else "••••••",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                letterSpacing = 8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pinVisible = !pinVisible },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(alpha = 0.25f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(
                                    imageVector = if (pinVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pinVisible) "Hide" else "Show",
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.refreshPin(lockId) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBlue
                                ),
                                enabled = !isLoadingPin
                            ) {
                                if (isLoadingPin) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh", fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }

                        pinError?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = Color(0xFFEF9A9A),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = TextWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Next rotation: ${formatNextRotation(nextRotation)}",
                                fontSize = 13.sp,
                                color = TextWhite.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = TextWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ROTATION SETTINGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite.copy(alpha = 0.5f),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Text(
                            text = "Rotation period",
                            fontSize = 14.sp,
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rotationOptions.forEach { hours ->
                                val isSelected = selectedHours == hours
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) AccentBlue
                                    else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f),
                                    onClick = { selectedHours = hours }
                                ) {
                                    Text(
                                        text = if (hours < 24) "${hours}h" else "24h",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold
                                        else FontWeight.Normal,
                                        color = if (isSelected) Color.White
                                        else TextWhite.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(16.dp))

//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(
//                                    text = "Show PIN on lock display",
//                                    fontSize = 15.sp,
//                                    color = TextWhite,
//                                    fontWeight = FontWeight.Medium
//                                )
//                                Text(
//                                    text = "Display new PIN for 10 sec after rotation",
//                                    fontSize = 12.sp,
//                                    color = TextWhite.copy(alpha = 0.45f),
//                                    modifier = Modifier.padding(top = 2.dp)
//                                )
//                            }
//                            Switch(
//                                checked = showOnDisplay,
//                                onCheckedChange = { showOnDisplay = it },
//                                colors = SwitchDefaults.colors(
//                                    checkedThumbColor = Color.White,
//                                    checkedTrackColor = AccentBlue,
//                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
//                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
//                                )
//                            )
//                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(visible = saveSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF81C784).copy(alpha = 0.15f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settings saved successfully",
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        isSaving = true
                        saveSuccess = false
                        isSaving = false
                        saveSuccess = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Settings",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

private fun formatNextRotation(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    return when {
        diff < 60 * 60 * 1000 -> "in ${diff / (60 * 1000)} min"
        diff < 24 * 60 * 60 * 1000 -> "in ${diff / (60 * 60 * 1000)}h"
        else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}