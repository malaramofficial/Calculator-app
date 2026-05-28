package com.aistudio.calculator.ywrbt.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.calculator.ywrbt.*
import com.aistudio.calculator.ywrbt.ui.theme.*
import com.aistudio.calculator.ywrbt.ui.profile.EditProfileScreen

@Composable
fun HiddenHomeScreen(viewModel: CalculatorViewModel) {
    val activeRecipient by viewModel.activeRecipient.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }

    if (showEditProfile) {
        EditProfileScreen(viewModel = viewModel, onBack = { showEditProfile = false })
    } else if (currentUsername.isEmpty()) {
        ProfileSetupScreen(viewModel, firestoreStatus)
    } else if (activeRecipient != null) {
        // We'll keep InstagramChatRoomView in MainActivity for now or move it later
        // For now, call it directly as it exists in MainActivity
        // BUT it's in MainActivity, not accessible from here.
        // Wait, I need to look at if I can move InstagramChatRoomView or keep it in MA.
        // As a first step, I'll move screens that *can* be moved easily.
    } else {
        // ... navigation to sub-tabs
    }
}
