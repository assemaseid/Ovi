package com.example.ovi.presentation.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.BluetoothViewModel
import com.example.ovi.presentation.viewmodel.OnboardingState
import com.example.ovi.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    onBack: () -> Unit = {},
    onNavigateToGuestJoin: () -> Unit = {},
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    var role by remember { mutableStateOf<String?>(null) }

    if (role == null) {
        RoleChooserContent(
            onBack = onBack,
            onOwner = { role = "owner" },
            onGuest = onNavigateToGuestJoin
        )
        return
    }
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val connectedAddress by viewModel.connectedAddress.collectAsState()
    val onboardingState by viewModel.onboardingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val detectedSsid = remember {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiManager.connectionInfo.ssid
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            ?: ""
    }

    var wifiSsid by remember { mutableStateOf(detectedSsid) }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(onboardingState) {
        when (val state = onboardingState) {
            is OnboardingState.Success -> {
                snackbarHostState.showSnackbar("Lock added successfully!")
                viewModel.resetOnboardingState()
            }
            is OnboardingState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetOnboardingState()
            }
            else -> {}
        }
    }

    if (onboardingState is OnboardingState.PinVerification) {
        OwnerPinVerificationDialog(
            onConfirm = { pin -> viewModel.verifyOwnerPin(pin) },
            onDismiss = { viewModel.resetOnboardingState() }
        )
    }

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startBleScan()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth permissions are required")
            }
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val allGranted = blePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) viewModel.startBleScan()
        else permissionLauncher.launch(blePermissions)
    }

    fun checkAndStartScan() {
        val btAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (btAdapter?.isEnabled != true) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        val allGranted = blePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) viewModel.startBleScan()
        else permissionLauncher.launch(blePermissions)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BgTop, BgBottom)
                    )
                )
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = "Pair Lock",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                if (isScanning) {
                    Surface(
                        shape = CircleShape,
                        color = AccentBlue.copy(alpha = 0.15f * pulseAlpha),
                        modifier = Modifier.size(140.dp)
                    ) {}
                    Surface(
                        shape = CircleShape,
                        color = AccentBlue.copy(alpha = 0.08f * pulseAlpha),
                        modifier = Modifier.size(110.dp)
                    ) {}
                }
                Surface(
                    shape = CircleShape,
                    color = if (isScanning) AccentBlue.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.BluetoothSearching
                        else Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
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
                text = if (isScanning) "Keep your lock close to the phone"
                else "Tap the button below to find nearby locks",
                fontSize = 13.sp,
                color = White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            OutlinedTextField(
                value = wifiSsid,
                onValueChange = { wifiSsid = it },
                label = { Text("WiFi name", color = White.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. MyWiFi", color = White.copy(alpha = 0.3f)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = White.copy(alpha = 0.3f),
                    cursorColor = AccentBlue,
                )
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = wifiPassword,
                onValueChange = { wifiPassword = it },
                label = { Text("WiFi password", color = White.copy(alpha = 0.7f)) },
                singleLine = true,
                visualTransformation = if (wifiPasswordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { wifiPasswordVisible = !wifiPasswordVisible }) {
                        Icon(
                            imageVector = if (wifiPasswordVisible) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = White.copy(alpha = 0.6f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = White.copy(alpha = 0.3f),
                    cursorColor = AccentBlue,
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isScanning) viewModel.stopBleScan()
                    else checkAndStartScan()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) Red.copy(alpha = 0.85f)
                    else AccentBlue
                )
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.BluetoothSearching
                    else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Stop Scanning" else "Start Scanning",
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
            }

            Spacer(Modifier.height(16.dp))

            val statusText = when (onboardingState) {
                is OnboardingState.Connecting -> "Connecting to lock..."
                is OnboardingState.ReadingInfo -> "Reading device info..."
                is OnboardingState.Registering -> "Registering with server..."
                is OnboardingState.Configuring -> "Configuring lock..."
                is OnboardingState.PinVerification -> "Verify PIN shown on lock display..."
                else -> null
            }
            if (statusText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(text = statusText, fontSize = 13.sp, color = White)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(12.dp))

            if (scannedDevices.isNotEmpty()) {
                Text(
                    text = "FOUND DEVICES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(scannedDevices) { device ->
                    val isConnected = connectedAddress == device.address

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (wifiSsid.isBlank() || wifiPassword.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Enter WiFi name and password first")
                                    }
                                    return@clickable
                                }
                                viewModel.pairAndConnect(device, wifiSsid, wifiPassword)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isConnected)
                                AccentBlue.copy(alpha = 0.12f)
                            else
                                Color.White.copy(alpha = 0.07f)
                        ),
                        border = if (isConnected) BorderStroke(1.dp, AccentBlue)
                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isConnected) AccentBlue
                                else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.padding(10.dp),
                                    tint = if (isConnected) Color.White
                                    else Color.White.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = White
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 12.sp,
                                    color = White.copy(alpha = 0.4f)
                                )
                            }

                            if (isConnected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (scannedDevices.isEmpty() && !isScanning) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No devices found yet",
                    fontSize = 14.sp,
                    color = White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun OwnerPinVerificationDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgTop,
        title = {
            Text("Verify Ownership", color = White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the 6-digit PIN shown on your lock's display to confirm you have physical access.",
                    color = White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN code", color = White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        cursorColor = AccentBlue,
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (pin.length == 6) onConfirm(pin) },
                enabled = pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Confirm", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
private fun RoleChooserContent(
    onBack: () -> Unit,
    onOwner: () -> Unit,
    onGuest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Text("Add Lock", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = White.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text("What is your role?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text(
                "Choose how you want to connect to this lock",
                fontSize = 14.sp,
                color = White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(
                onClick = onOwner,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = White)
                Spacer(Modifier.width(10.dp))
                Text("I'm the Owner", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGuest,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, White.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = White)
                Spacer(Modifier.width(10.dp))
                Text("I'm a Guest", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(80.dp))
        }
    }
}
