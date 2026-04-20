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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.domain.model.EventType
import com.example.ovi.domain.model.LockEvent
import com.example.ovi.domain.model.UnlockMethod
import com.example.ovi.presentation.viewmodel.EventViewModel
import com.example.ovi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventLogScreen(
    lockId: String,
    lockName: String,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()

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
                        text = "Event Log",
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${events.size} events",
                        fontSize = 12.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextWhite.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No events yet",
                            color = TextWhite.copy(alpha = 0.5f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(events) { event ->
                        EventCard(event = event)
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: LockEvent) {
    val (icon, label, iconColor) = when (event.type) {
        EventType.UNLOCK -> Triple(
            Icons.Default.LockOpen,
            "Unlocked",
            Color(0xFF81C784)
        )
        EventType.LOCK -> Triple(
            Icons.Default.Lock,
            "Locked",
            Color(0xFF90CAF9)
        )
        EventType.PIN_ROTATION -> Triple(
            Icons.Default.Pin,
            "PIN Rotated",
            Color(0xFFFFB74D)
        )
        EventType.LOW_BATTERY -> Triple(
            Icons.Default.BatteryAlert,
            "Low Battery",
            Color(0xFFFF7043)
        )
        EventType.TAMPER_DETECTED -> Triple(
            Icons.Default.Warning,
            "Tamper Detected",
            Color(0xFFEF5350)
        )
    }

    val methodLabel = when (event.method) {
        UnlockMethod.BLUETOOTH -> "Bluetooth"
        UnlockMethod.PIN -> "PIN code"
        UnlockMethod.REMOTE -> "Remote"
        UnlockMethod.MANUAL -> "Manual"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                    if (!event.success) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF5350).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Failed",
                                fontSize = 10.sp,
                                color = Color(0xFFEF5350),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
//                Text(
//                    text = "via $methodLabel",
//                    fontSize = 12.sp,
//                    color = TextWhite.copy(alpha = 0.5f)
//                )
            }

            Text(
                text = formatEventTime(event.timestamp),
                fontSize = 12.sp,
                color = TextWhite.copy(alpha = 0.45f)
            )
        }
    }
}

private fun formatEventTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
