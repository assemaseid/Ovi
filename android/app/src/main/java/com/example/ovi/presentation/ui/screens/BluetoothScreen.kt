package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ovi.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.BluetoothViewModel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import com.example.ovi.ui.theme.Green40
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val connectedAddress by viewModel.connectedAddress.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                snackbarHostState.showSnackbar("Bluetooth permissions are required to scan for devices")
            }
        }
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val allGranted = blePermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.startBleScan()
        } else {
            permissionLauncher.launch(blePermissions)
        }
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
        if (allGranted) {
            viewModel.startBleScan()
        } else {
            permissionLauncher.launch(blePermissions)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween (1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))

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
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(48.dp))


                    Text(
                        text = "Pair Lock",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Surface(
                            shape = CircleShape,
                            color = Green40.copy(alpha = 0.1f * alpha),
                            modifier = Modifier.size(120.dp)
                        ) {}
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isScanning) Green40.copy(alpha = 0.15f) else Color.Gray.copy(
                            alpha = 0.1f
                        ),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = if (isScanning) Green40 else Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = if (isScanning) "Searching for OVI Locks" else "Bluetooth Scanner",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (isScanning) "Keep your lock close to the phone" else "Tap scan to find nearby devices",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { if (isScanning) viewModel.stopBleScan() else checkAndStartScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color.Red.copy(alpha = 0.8f) else Green40
                    )
                ) {
                    Text(
                        text = if (isScanning) "STOP SCANNING" else "START SCANNING",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "FOUND DEVICES",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(scannedDevices) { device ->
                        val isConnected = connectedAddress == device.address

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.pairAndConnect(device) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isConnected) Green40.copy(0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f
                                )
                            ),
                            border = if (isConnected) BorderStroke(1.dp, Green40) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isConnected) Green40 else Color.Gray.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = if (isConnected) Color.White else Color.Gray
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: "Unknown Device",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                if (isConnected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Green40
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
