package com.aistudio.calculator.ywrbt.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistudio.calculator.ywrbt.CalculatorViewModel
import com.aistudio.calculator.ywrbt.ui.theme.*

@Composable
fun VoiceCallScreen(viewModel: CalculatorViewModel, callerName: String, onEndCall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Calling $callerName", color = TextLight, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEndCall, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("End Call")
        }
    }
}
