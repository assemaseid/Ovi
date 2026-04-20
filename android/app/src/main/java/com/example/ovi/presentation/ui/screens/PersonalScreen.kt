package com.example.ovi.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ovi.presentation.viewmodel.AuthViewModel
import com.example.ovi.ui.theme.*

@Composable
fun PersonalScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val email = currentUser?.email ?: viewModel.getUserEmail()
    val name = currentUser?.name

    val initials = if (!name.isNullOrBlank()) {
        name.trim().split("\\s+".toRegex())
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    } else {
        email.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Profile",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 32.dp)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!name.isNullOrBlank()) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = email,
                fontSize = 14.sp,
                color = TextWhite.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    if (!name.isNullOrBlank()) {
                        ProfileRow(
                            icon = Icons.Default.Person,
                            label = "Name",
                            value = name
                        )
                    }

                    ProfileRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = email
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log out",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(133.dp))
        }
    }
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextWhite.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextHint
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
        }
    }
}