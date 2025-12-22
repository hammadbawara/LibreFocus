package com.librefocus.ui.onboarding

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librefocus.R

@Composable
fun AppFeaturesScreen(
    onNext: () -> Unit
) {
    val brandTeal = Color(0xFFB2DFDB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Top Icon Area (roughly 35% of screen height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp), // control height here only
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxHeight(), // takes full box height
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 2. App Name (Centered under icon)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Libre")
                        withStyle(style = SpanStyle(color = brandTeal)) {
                            append("Focus")
                        }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Features List with Emojis
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItem(
                    emoji = "📱",
                    text = "Track your app usage effortlessly"
                )
                FeatureItem(
                    emoji = "📊",
                    text = "Visualize daily & weekly patterns"
                )
                FeatureItem(
                    emoji = "🔍",
                    text = "Discover deeper app insights"
                )
                FeatureItem(
                    emoji = "⏱️",
                    text = "Set usage limits & smart reminders"
                )
                FeatureItem(
                    emoji = "🤖",
                    text = "Get guidance from your AI assistant"
                )
                FeatureItem(
                    emoji = "🏆",
                    text = "Stay motivated with achievements"
                )
                FeatureItem(
                    emoji = "🔒",
                    text = "Privacy-first, your data stays secure"
                )
            }
        }

        // 4. Bottom Right Button with ">"
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = brandTeal,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(64.dp) // Square button
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun FeatureItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Emoji rendered as Text to preserve original colors
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 16.sp
        )
    }
}

// --- PREVIEW FUNCTION ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppFeaturesScreenPreview() {
    MaterialTheme {
        Surface {
            AppFeaturesScreen(
                onNext = {}
            )
        }
    }
}