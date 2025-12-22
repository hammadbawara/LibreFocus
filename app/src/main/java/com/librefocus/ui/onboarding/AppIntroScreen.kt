package com.librefocus.ui.onboarding

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
fun AppIntroScreen(
    onNext: () -> Unit
) {
    // Define the custom color provided
    val brandTeal = Color(0xFFB2DFDB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Middle Content Group
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.End // Pushes the slogan to the right
        ) {
            // Row for Logo (Left) and Name (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))

                // "LibreFocus" with "Focus" colored
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

            // Slogan
            Text(
                text = "Reclaim your focus",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bottom Button
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = brandTeal, // Background color #B2DFDB
                contentColor = Color.Black  // Text color Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(56.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- PREVIEW FUNCTION ---
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppIntroScreenPreview() {
    MaterialTheme {
        Surface {
            AppIntroScreen(
                onNext = {}
            )
        }
    }
}