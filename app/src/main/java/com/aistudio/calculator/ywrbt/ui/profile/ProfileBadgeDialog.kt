package com.aistudio.calculator.ywrbt.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.aistudio.calculator.ywrbt.CalculatorViewModel
import com.aistudio.calculator.ywrbt.ChatProfile
import com.aistudio.calculator.ywrbt.ui.theme.*

@Composable
fun ProfileBadgeDialog(
    profile: ChatProfile,
    viewModel: CalculatorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.appLanguage.collectAsState()
    
    val avatarColorHex = try {
        Color(android.graphics.Color.parseColor(profile.avatarColorHex))
    } catch (e: Exception) {
        AccentBlue
    }

    val isOnline = isOnlineUserCheckShared(profile.username, profile.lastActive)

    val isOwner = profile.googleEmail.trim().equals("malaramofficial@gmail.com", ignoreCase = true) ||
            profile.username.trim().equals("malaram_official", ignoreCase = true) ||
            profile.username.trim().equals("malaramofficial", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .border(1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SettingsLoc.t("badge_profile_details", currentLanguage),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Badge",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Avatar Large
                Box(contentAlignment = Alignment.BottomEnd) {
                    val gradientBorder = Brush.sweepGradient(
                        colors = listOf(AccentBlue, AccentRose, Color(0xFF10B981), AccentBlue)
                    )
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .let { modifier ->
                                if (isOwner) {
                                    modifier.background(Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))))
                                } else if (profile.avatarUrl != null) {
                                    modifier.background(Color.Transparent)
                                } else {
                                    modifier.background(Brush.linearGradient(listOf(avatarColorHex, avatarColorHex.copy(alpha = 0.6f))))
                                }
                            }
                            .border(3.dp, if (isOnline) gradientBorder else Brush.linearGradient(listOf(CardSlate, CardSlate)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOwner) {
                            Text(
                                text = "👑",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (profile.avatarUrl != null) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "Profile Pic Large",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = profile.username.take(2).uppercase(),
                                color = TextLight,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Online indicator badge large
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(3.dp, CardSlate, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name with validation status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = profile.fullName,
                        color = if (isOwner) Color(0xFFFBBF24) else TextLight,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isOwner) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFBBF24))
                        ) {
                            Text(
                                text = "मालिक 👑",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (profile.username.trim().lowercase() in listOf("instagram_support", "sneha_kapoor", "rahul_dev", "elon_musk")) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified status",
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "@${profile.username}",
                    color = AccentBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Personal details / Localized fields
                // Status Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSlateBg.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Circle else Icons.Default.TripOrigin,
                        contentDescription = "Presence",
                        tint = if (isOnline) Color(0xFF10B981) else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = SettingsLoc.t("badge_status", currentLanguage),
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOnline) SettingsLoc.t("badge_online", currentLanguage) else SettingsLoc.t("badge_offline", currentLanguage),
                            color = if (isOnline) Color(0xFF10B981) else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bio Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSlateBg.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = SettingsLoc.t("badge_bio", currentLanguage),
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (profile.bio.isEmpty()) "चैटिंग के लिए बायो सेट नहीं किया गया है।" else profile.bio,
                        color = TextLight,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                // Show Google verification context if they have an email registered
                if (profile.googleEmail.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSlateBg.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email verified",
                            tint = Color(0xFFFFB0B0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = SettingsLoc.t("account_info", currentLanguage),
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile.googleEmail,
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Direct Chat navigation button inside Badge
                Button(
                    onClick = {
                        viewModel.setActiveRecipient(profile)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Chat",
                        tint = DarkSlateBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SettingsLoc.t("badge_start_chat", currentLanguage),
                        color = DarkSlateBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
