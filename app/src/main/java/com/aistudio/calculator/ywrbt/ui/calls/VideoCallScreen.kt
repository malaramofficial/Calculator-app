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
fun VideoCallScreen(viewModel: CalculatorViewModel, callerName: String, onEndCall: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
    ) {
        Text("Video Call with $callerName", color = TextLight, modifier = Modifier.align(Alignment.Center))
        Button(
            onClick = onEndCall, 
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("End Call")
        }
    }
}
