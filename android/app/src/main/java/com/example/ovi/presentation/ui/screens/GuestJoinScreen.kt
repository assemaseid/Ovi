package com.example.ovi.presentation.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.GuestJoinState
import com.example.ovi.presentation.viewmodel.GuestJoinViewModel
import com.example.ovi.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GuestJoinScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: GuestJoinViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) {
        when (state) {
            is GuestJoinState.Success -> {
                onSuccess()
                viewModel.resetState()
            }
            is GuestJoinState.Error -> {
                snackbarHostState.showSnackbar((state as GuestJoinState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) viewModel.startBleScan()
        else scope.launch { snackbarHostState.showSnackbar("Bluetooth permissions required") }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (blePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) viewModel.startBleScan()
        else permissionLauncher.launch(blePermissions)
    }

    fun checkAndStartScan() {
        val btAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (btAdapter?.isEnabled != true) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        if (blePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) viewModel.startBleScan()
        else permissionLauncher.launch(blePermissions)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
                .zIndex(1f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Column {
                    Text("Join as Guest", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                    Text("Find the lock via Bluetooth", fontSize = 13.sp, color = White.copy(alpha = 0.5f))
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                if (isScanning) {
                    Surface(shape = CircleShape, color = AccentBlue.copy(alpha = 0.15f * pulseAlpha), modifier = Modifier.size(120.dp)) {}
                    Surface(shape = CircleShape, color = AccentBlue.copy(alpha = 0.08f * pulseAlpha), modifier = Modifier.size(90.dp)) {}
                }
                Surface(
                    shape = CircleShape,
                    color = if (isScanning) AccentBlue.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = if (isScanning) AccentBlue else Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isScanning) "Searching for OVI Locks…" else "Ready to scan",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = White
            )
            Text(
                text = if (isScanning) "Keep your lock close to the phone" else "Tap the button below to find nearby locks",
                fontSize = 13.sp,
                color = White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                textAlign = TextAlign.Center
            )

            val isRequesting = state is GuestJoinState.RequestingAccess

            Button(
                onClick = {
                    if (isScanning) viewModel.stopBleScan()
                    else checkAndStartScan()
                },
                enabled = !isRequesting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Red.copy(alpha = 0.85f) else AccentBlue
                )
            ) {
                if (isRequesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting…", color = White, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isScanning) "Stop Scanning" else "Start Scanning", fontWeight = FontWeight.SemiBold, color = White)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (scannedDevices.isNotEmpty()) {
                Text(
                    text = "FOUND LOCKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(scannedDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isRequesting) {
                            viewModel.requestAccess(device)
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(42.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(10.dp), tint = Color.White.copy(alpha = 0.5f))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                @Suppress("MissingPermission")
                                Text(device.name ?: "Unknown Device", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = White)
                                Text(device.address, fontSize = 12.sp, color = White.copy(alpha = 0.4f))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = White.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            if (scannedDevices.isEmpty() && !isScanning) {
                Spacer(Modifier.height(16.dp))
                Text("No devices found yet", fontSize = 14.sp, color = White.copy(alpha = 0.3f))
            }
        }
    }

    if (state is GuestJoinState.WaitingForPin) {
        PinEntryDialog(
            onDismiss = { viewModel.resetState() },
            onSubmit = { pin -> viewModel.submitPin(pin) },
            isJoining = false
        )
    }

    if (state is GuestJoinState.Joining) {
        PinEntryDialog(
            onDismiss = {},
            onSubmit = {},
            isJoining = true
        )
    }
}

@Composable
private fun PinEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isJoining: Boolean
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        containerColor = Color(0xFF1E5A8A),
        title = { Text("Enter Access PIN", color = White, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "The lock owner received a PIN via notification. Ask them to share it with you.",
                    color = White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                if (isJoining) {
                    CircularProgressIndicator(color = White)
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) pin = it },
                        label = { Text("6-digit PIN", color = White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (pin.length == 6) onSubmit(pin) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = White,
                            unfocusedBorderColor = White.copy(alpha = 0.4f),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!isJoining) {
                TextButton(
                    onClick = { onSubmit(pin) },
                    enabled = pin.length == 6
                ) {
                    Text("Join", color = if (pin.length == 6) White else White.copy(alpha = 0.4f), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (!isJoining) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = White.copy(alpha = 0.7f)) }
            }
        }
    )
}
