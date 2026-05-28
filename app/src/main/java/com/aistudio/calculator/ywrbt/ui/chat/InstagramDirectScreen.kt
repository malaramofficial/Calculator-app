package com.aistudio.calculator.ywrbt.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.aistudio.calculator.ywrbt.CalculatorViewModel
import com.aistudio.calculator.ywrbt.ui.theme.*
import com.aistudio.calculator.ywrbt.ui.profile.EditProfileScreen

// NOTE: These composables need to be accessible. 
// For now, assume they are defined in MainActivity.kt or need to be moved too.
// I will start by moving the structural composables first.

@Composable
fun InstagramDirectScreen(viewModel: CalculatorViewModel) {
    val activeRecipient by viewModel.activeRecipient.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()
    val firestoreStatus by viewModel.firestoreStatus.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }

    if (showEditProfile) {
        EditProfileScreen(viewModel = viewModel, onBack = { showEditProfile = false })
    } else if (currentUsername.isEmpty()) {
        // Assume ProfileSetupScreen is also in MainActivity for now, 
        // to be moved in next steps.
        // For compilation, I need to either import it or move it.
        // Let's keep moving step by step.
    } else if (activeRecipient != null) {
        // InstagramChatRoomView(viewModel)
    } else {
        var currentSubTab by remember { mutableStateOf("messages") } 

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlateBg)
        ) {
            InstagramTopHeader(viewModel, currentUsername, onEditProfile = { showEditProfile = true })

            // ... rest of the UI
        }
    }
}

@Composable
fun InstagramTopHeader(
    viewModel: CalculatorViewModel,
    currentUser: String,
    onEditProfile: () -> Unit
) {
    Surface(
        color = CardSlate,
        shadowElevation = 4.dp
    ) {
        // ... (Header code)
    }
}
