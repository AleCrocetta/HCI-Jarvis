package com.example.calendarapp.ui.home

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendarapp.ui.theme.DarkBlue
import com.example.calendarapp.ui.theme.LightGrayBg
import com.example.calendarapp.ui.theme.TextGray
import com.example.calendarapp.ui.theme.White

@Composable
fun JarvisOrb(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")
    
    // Animate scale/pulse
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val baseRadius = size.minDimension / 3.2f
            
            // Outer glowing ambient ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.35f * scale),
                        Color(0xFF2979FF).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.8f
                )
            )
            
            // Cyber-ring outline
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.6f),
                radius = baseRadius * 1.15f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            
            // Pulsing core orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF2979FF)
                    ),
                    center = center,
                    radius = baseRadius * 0.75f * scale
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisBottomBar(
    modifier: Modifier = Modifier,
    onSendClick: (String) -> Unit = {},
    onMicClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var textState by remember { mutableStateOf("") }
    
    Surface(
        color = White,
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, spotColor = DarkBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.example.calendarapp.R.drawable.jarvis_bottom_icon),
                contentDescription = "Jarvis Logo",
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Search / Chat Input Bar
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { 
                    Text(
                        "Ask Jarvis...", 
                        color = TextGray, 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .width(400.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBlue.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = LightGrayBg,
                    unfocusedContainerColor = LightGrayBg
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Microphone / Send Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2979FF),
                                Color(0xFF00E5FF)
                            )
                        )
                    )
                    .clickable {
                        if (textState.isNotBlank()) {
                            onSendClick(textState)
                            textState = ""
                        } else {
                            onMicClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (textState.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = if (textState.isNotBlank()) "Send Text" else "Voice Input",
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
