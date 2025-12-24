package com.librefocus.models

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val category: String
)

enum class AppCategory(val displayName: String) {
    ALL("All"),
    SOCIAL("Social"),
    PRODUCTIVITY("Productivity"),
    ENTERTAINMENT("Entertainment"),
    GAMES("Games"),
    COMMUNICATION("Communication"),
    UTILITIES("Utilities"),
    SHOPPING("Shopping"),
    NEWS("News & Magazines"),
    LIFESTYLE("Lifestyle"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): AppCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
