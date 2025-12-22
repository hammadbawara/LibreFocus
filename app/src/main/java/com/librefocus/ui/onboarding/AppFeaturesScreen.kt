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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.vector.ImageVector
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

            // 3. Features List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing to fit more items
            ) {
                FeatureItem(
                    icon = Icons.Default.Smartphone,
                    text = "Track your app usage effortlessly"
                )
                FeatureItem(
                    icon = Icons.Default.BarChart,
                    text = "Visualize daily & weekly patterns"
                )
                FeatureItem(
                    icon = Icons.Default.QueryStats, // Magnifying glass with stats
                    text = "Discover deeper app insights"
                )
                FeatureItem(
                    icon = Icons.Default.Timer,
                    text = "Set usage limits & smart reminders"
                )
                FeatureItem(
                    icon = Icons.Default.SmartToy, // Robot icon
                    text = "Get guidance from your AI assistant"
                )
                FeatureItem(
                    icon = Icons.Default.EmojiEvents, // Trophy icon
                    text = "Stay motivated with achievements"
                )
                FeatureItem(
                    icon = Icons.Default.Lock,
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
fun FeatureItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
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