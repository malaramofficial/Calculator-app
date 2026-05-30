package com.aistudio.calculator.ywrbt.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.calculator.ywrbt.ui.theme.*

@Composable
fun SignInScreen(
    onSignInClick: () -> Unit,
    onManualSignIn: (email: String, name: String) -> Unit
) {
    // Official Google Color Palette
    val googleBlue = Color(0xFF4285F4)
    val googleRed = Color(0xFFEA4335)
    val googleYellow = Color(0xFFFBBC05)
    val googleGreen = Color(0xFF34A853)

    var isPressed by remember { mutableStateOf(false) }
    val googleButtonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "google_button_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF020617)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Dynamic Glowing Emblem Card Overlay
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF3B82F6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Large Premium Google Brand Title Header with official spacing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text("G", color = googleBlue, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("o", color = googleRed, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("o", color = googleYellow, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("g", color = googleBlue, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("l", color = googleGreen, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("e", color = googleRed, fontSize = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
        }

        Text(
            text = "Welcome to Instagram Direct",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "सुरक्षित चैटिंग शुरू करने के लिए अपने गूगल अकाउंट से साइन इन करें।",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(44.dp))

        // One and Only Official Styled "Sign in with Google" Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(googleButtonScale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onSignInClick() }
                    )
                }
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Official G Circle Layout
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = googleBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Text(
                    text = "Sign in with Google",
                    color = Color(0xFF1F1F1F),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "या बिना अकाउंट के प्रवेश करें 💕",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        var showOfflineInput by remember { mutableStateOf(false) }
        if (!showOfflineInput) {
            OutlinedButton(
                onClick = { showOfflineInput = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5277)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5277).copy(alpha = 0.6f))
            ) {
                Text("GUEST MODE (बिना गूगल अकाउंट चैट रूम)", fontSize = 12.sp)
            }
        } else {
            var guestName by remember { mutableStateOf("") }
            var guestEmail by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color(0xFFFF5277).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    label = { Text("Display Name / नाम", color = Color.White.copy(alpha = 0.6f)) },
                    placeholder = { Text("e.g. Rahul / Priya") },
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF5277),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFF5277)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = guestEmail,
                    onValueChange = { guestEmail = it },
                    label = { Text("Mock Email / ईमेल", color = Color.White.copy(alpha = 0.6f)) },
                    placeholder = { Text("e.g. rahul@love.com") },
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF5277),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color(0xFFFF5277)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val finalEmail = guestEmail.trim().ifEmpty { "guest_${kotlin.random.Random.nextInt(1000, 9999)}@love.com" }
                        val finalName = guestName.trim().ifEmpty { "Guest Lover" }
                        onManualSignIn(finalEmail, finalName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5277)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("चैट रूम में जाएं (Enter Chat)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))

        // Security Shield Badge & Disclaimers at Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                     imageVector = Icons.Default.Lock,
                     contentDescription = null,
                     tint = AccentBlue,
                     modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Identity Secure Gateway",
                    color = TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "This application strictly adheres to Google API Services User Data Policy.",
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
