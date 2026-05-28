package com.aistudio.calculator.ywrbt.ui.chat

import com.aistudio.calculator.ywrbt.CalculatorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VanishModeManager(
    private val viewModel: CalculatorViewModel,
    private val scope: CoroutineScope
) {
    fun scheduleVanish(messageId: String, durationMs: Long) {
        scope.launch {
            delay(durationMs)
            // Implementation calls viewModel to delete the message
            // or perform actual Firebase deletion
        }
    }
}
