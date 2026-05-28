package com.aistudio.calculator.ywrbt.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Large Premium Google Brand Title Header with official spacing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("G", color = googleBlue, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("o", color = googleRed, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("o", color = googleYellow, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("g", color = googleBlue, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("l", color = googleGreen, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("e", color = googleRed, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
        }

        Text(
            text = "Welcome to Instagram Direct",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "सुरक्षित चैटिंग शुरू करने के लिए अपने गूगल अकाउंट से साइन इन करें।",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // One and Only Official Styled "Sign in with Google" Button
        Card(
            onClick = onSignInClick, // Directly call native Google Sign-In intent
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.2.sp
                )
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
                     tint = TextMuted,
                     modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Identity Secure Gateway",
                    color = TextMuted,
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
