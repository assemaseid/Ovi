package com.example.ovi.presentation.ui.screens

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.BluetoothViewModel
import com.example.ovi.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(
    onBack: () -> Unit = {},
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        // Градиентный фон на весь экран
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BgTop, BgBottom)
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(48.dp))

            // Заголовок
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
                        tint = TextWhite
                    )
                }
                Text(
                    text = "Pair Lock",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            // Иконка с анимацией пульса
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

            // Статус текст
            Text(
                text = if (isScanning) "Searching for OVI Locks…" else "Ready to scan",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = if (isScanning) "Keep your lock close to the phone"
                else "Tap the button below to find nearby locks",
                fontSize = 13.sp,
                color = TextWhite.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Кнопка Scan / Stop
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
                    containerColor = if (isScanning) Color(0xFFEF5350).copy(alpha = 0.85f)
                    else AccentBlue
                )
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.BluetoothSearching
                    else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Stop Scanning" else "Start Scanning",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(28.dp))

            // Заголовок списка
            if (scannedDevices.isNotEmpty()) {
                Text(
                    text = "FOUND DEVICES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            }

            // Список найденных устройств
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // отступ под bottom bar
            ) {
                items(scannedDevices) { device ->
                    val isConnected = connectedAddress == device.address

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.pairAndConnect(device) },
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
                            // Иконка замка
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
                                    color = TextWhite
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 12.sp,
                                    color = TextWhite.copy(alpha = 0.4f)
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

            // Пустое состояние — нет устройств и не сканирует
            if (scannedDevices.isEmpty() && !isScanning) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No devices found yet",
                    fontSize = 14.sp,
                    color = TextWhite.copy(alpha = 0.3f)
                )
            }
        }
    }
}