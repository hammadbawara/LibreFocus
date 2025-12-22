package com.librefocus.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccessibilityScreen(
    onEnableClick: () -> Unit,
    onNext: () -> Unit
) {
    val brandTeal = Color(0xFFB2DFDB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Scrollable Text Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Add padding at the bottom so content doesn't get hidden behind the button
                .padding(bottom = 70.dp)
                // Added extra vertical padding as requested
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Left Branding (Icon Removed)
            Text(
                text = buildAnnotatedString {
                    append("Libre")
                    withStyle(style = SpanStyle(color = brandTeal)) {
                        append("Focus")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 2. Heading
            Text(
                text = "Help Us Understand Your Usage",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Paragraphs
            ExplanationText(
                text = "To offer meaningful insights and smart usage alerts, we require Accessibility permission."
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExplanationText(
                text = "This allows the app to detect app activity safely and securely."
            )

            Spacer(modifier = Modifier.height(24.dp))

            ExplanationText(
                text = "We do not collect or transmit any personal data—everything remains on your device."
            )
        }

        // 4. Main Action Button (Fixed at Bottom)
        Button(
            onClick = onEnableClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = brandTeal,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(56.dp)
        ) {
            Text(
                text = "Grant Permission",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExplanationText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// --- PREVIEW FUNCTION ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AccessibilityScreenPreview() {
    MaterialTheme {
        Surface {
            AccessibilityScreen(
                onEnableClick = {},
                onNext = {}
            )
        }
    }
}