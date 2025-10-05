package com.librefocus.models

data class AppUsage(
    val packageName: String,
    val appName: String,
    val icon: Any?, // Drawable or ImageBitmap depending on load method
    val usageTimeMillis: Long
)