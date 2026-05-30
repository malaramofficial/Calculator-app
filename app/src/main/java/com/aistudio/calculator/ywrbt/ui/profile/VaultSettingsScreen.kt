package com.aistudio.calculator.ywrbt.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.calculator.ywrbt.CalculatorViewModel
import com.aistudio.calculator.ywrbt.ui.theme.*

@Composable
fun VaultSettingsScreen(
    viewModel: CalculatorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.appTheme.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()
    val currentRingtone by viewModel.messageRingtone.collectAsState()
    val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()
    val backgroundSyncEnabled by viewModel.backgroundSyncEnabled.collectAsState()
    val googleEmail by viewModel.googleAccountEmail.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextLight
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = SettingsLoc.t("settings_title", currentLanguage),
                    color = TextLight,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Scrollable Settings Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: APPEARANCE / COLOR THEME
            Text(
                text = SettingsLoc.t("theme_section", currentLanguage).uppercase(),
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = SettingsLoc.t("theme_desc", currentLanguage),
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val themesList = listOf(
                        Triple("theme_dark_slate", SettingsLoc.t("theme_slate", currentLanguage), Color(0xFF38BDF8)),
                        Triple("theme_midnight_cosmic", SettingsLoc.t("theme_cosmic", currentLanguage), Color(0xFFC084FC)),
                        Triple("theme_forest_emerald", SettingsLoc.t("theme_forest", currentLanguage), Color(0xFF34D399)),
                        Triple("theme_royal_amethyst", SettingsLoc.t("theme_amethyst", currentLanguage), Color(0xFFE879F9)),
                        Triple("theme_love_velvet", SettingsLoc.t("theme_love_velvet", currentLanguage), Color(0xFFFF5277))
                    )

                    for (themeItem in themesList) {
                        val id = themeItem.first
                        val title = themeItem.second
                        val colorHex = themeItem.third
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setAppTheme(id)
                                    Toast.makeText(context, SettingsLoc.t("toast_theme", currentLanguage), Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorHex)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    text = title,
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = currentTheme == id,
                                onClick = {
                                    viewModel.setAppTheme(id)
                                    Toast.makeText(context, SettingsLoc.t("toast_theme", currentLanguage), Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Section 2: LANGUAGE SELECTION
            Text(
                text = SettingsLoc.t("lang_section", currentLanguage).uppercase(),
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = SettingsLoc.t("lang_desc", currentLanguage),
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val langList = listOf(
                        "hi" to SettingsLoc.t("lang_hi", currentLanguage),
                        "en" to SettingsLoc.t("lang_en", currentLanguage),
                        "hinglish" to SettingsLoc.t("lang_hinglish", currentLanguage)
                    )

                    for (langPair in langList) {
                        val code = langPair.first
                        val label = langPair.second
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setAppLanguage(code)
                                    Toast.makeText(context, SettingsLoc.t("toast_lang", currentLanguage), Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Lang icon",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    text = label,
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = currentLanguage == code,
                                onClick = {
                                    viewModel.setAppLanguage(code)
                                    Toast.makeText(context, SettingsLoc.t("toast_lang", currentLanguage), Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: RINGTONES / MESSAGE ALERTS
            Text(
                text = SettingsLoc.t("sound_section", currentLanguage).uppercase(),
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = SettingsLoc.t("sound_desc", currentLanguage),
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val tones = listOf(
                        "ringtone_default" to SettingsLoc.t("sound_bell", currentLanguage),
                        "ringtone_bubble" to SettingsLoc.t("sound_bubble", currentLanguage),
                        "ringtone_digital" to SettingsLoc.t("sound_digital", currentLanguage),
                        "ringtone_silent" to SettingsLoc.t("sound_silent", currentLanguage)
                    )

                    for (tonePair in tones) {
                        val code = tonePair.first
                        val label = tonePair.second
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.setMessageRingtone(code)
                                    Toast.makeText(context, SettingsLoc.t("toast_sound", currentLanguage), Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (code == "ringtone_silent") Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "volume status",
                                tint = if (code == "ringtone_silent") AccentRose else AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                    text = label,
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = currentRingtone == code,
                                onClick = {
                                    viewModel.setMessageRingtone(code)
                                    Toast.makeText(context, SettingsLoc.t("toast_sound", currentLanguage), Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Section 4: TRIGGERS AND GENERAL BEHAVIORS
            Text(
                text = "Privacy & Notifications".uppercase(),
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Vibrate toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SettingsLoc.t("vibrate_title", currentLanguage),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SettingsLoc.t("vibrate_desc", currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = { viewModel.setVibrateEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ButtonSlate
                            )
                        )
                    }

                    HorizontalDivider(color = DarkSlateBg, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Seen status read receipts toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SettingsLoc.t("seen_title", currentLanguage),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SettingsLoc.t("seen_desc", currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = readReceiptsEnabled,
                            onCheckedChange = { viewModel.setReadReceiptsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ButtonSlate
                            )
                        )
                    }

                    HorizontalDivider(color = DarkSlateBg, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Background Chat Sync Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SettingsLoc.t("bg_sync_title", currentLanguage),
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = SettingsLoc.t("bg_sync_desc", currentLanguage),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = backgroundSyncEnabled,
                            onCheckedChange = { viewModel.setBackgroundSyncEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ButtonSlate
                            )
                        )
                    }
                }
            }

            // Section 5: Play Store Compliance & Data Safety
            Text(
                text = "Play Store Compliance & Data Safety".uppercase(),
                color = AccentBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Privacy Policy Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "info",
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLanguage == "hi") "गोपनीयता नीति (Privacy Policy)" else "Privacy Policy & Disclosures",
                                color = TextLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentLanguage == "hi") "डेटा सुरक्षा, उपयोग और अनुपालन के विवरण देखें" else "Check legal data usage compliance statements",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "go",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = DarkSlateBg, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    // Account Deletion Request
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteConfirm = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "delete",
                            tint = AccentRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLanguage == "hi") "खाता और डेटा स्थायी रूप से हटाएं" else "Delete Account & Clean Store Data",
                                color = AccentRose,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentLanguage == "hi") "सभी संदेश इतिहास और प्रोफ़ाइल तुरंत साफ़ करें" else "Instantly delete Firestore documents and chat logs",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Credits/Account Details panel
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlateBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Verified",
                        tint = AccentBlue,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = googleEmail ?: "No Connected Account",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SettingsLoc.t("dev_credit", currentLanguage),
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    // Dialogs for Policy & Account Deletion
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacy Policy & Safety Guide",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Information We Collect",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We collect your Google profile details (Display Name and Email Address) solely for the purpose of creating a chat ID handle on Firebase Firestore.",
                        color = TextLight,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "2. Chat & Communication",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Messages, images and data you send inside chats are saved directly to Firestore Database to enable secure network relaying. Chat logs can be fully wiped out at any time.",
                        color = TextLight,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "3. Complete Data Deletion Control",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "According to Google Play developer policy, you can completely request and delete your auth handle and user entry by tapping 'Delete Account & Clean Store Data'.",
                        color = TextLight,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "4. Third-Party Sharing",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "We never sell, rent or share any user details or logs with marketing teams, analytic hubs, or advertisers.",
                        color = TextLight,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("I Understand", color = DarkSlateBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            title = {
                Text(
                    text = if (currentLanguage == "hi") "क्या आप निश्चित हैं?" else "Are you absolutely sure?",
                    color = AccentRose,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == "hi")
                        "डेटा हटाना अपरिवर्तनीय है। यह प्रक्रिया आपके गूगल प्रोफ़ाइल आईडी और सभी फायरबेस स्टोर दस्तावेज़ों को स्थायी रूप से हटा देगी!"
                        else "This operation will completely erase your profile node from Firestore and clear all active instances. There is no recovery option.",
                    color = TextLight,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteUserAccountAndData { success ->
                            isDeleting = false
                            showDeleteConfirm = false
                            Toast.makeText(
                                context,
                                if (success) "Account and Google link fully deleted!" else "Signed out of secure tunnel!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (currentLanguage == "hi") "हाँ, स्थायी रूप से हटाएं" else "Yes, Erase Everything", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeleting
                ) {
                    Text(SettingsLoc.t("close", currentLanguage), color = TextLight)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
