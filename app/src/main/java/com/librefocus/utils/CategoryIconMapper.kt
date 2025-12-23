package com.librefocus.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized mapping for category icons.
 * Provides icon resolution for system and custom categories.
 */
object CategoryIconMapper {
    
    /**
     * System category names (Android standard categories)
     */
    object SystemCategories {
        const val UNDEFINED = "Undefined"
        const val GAME = "Game"
        const val AUDIO = "Audio"
        const val VIDEO = "Video"
        const val IMAGE = "Image"
        const val SOCIAL = "Social"
        const val NEWS = "News"
        const val MAPS = "Maps"
        const val PRODUCTIVITY = "Productivity"
        const val ACCESSIBILITY = "Accessibility"
    }
    
    /**
     * Available icon options for category selection
     */
    data class IconOption(
        val name: String,
        val icon: ImageVector
    )
    
    /**
     * Get the icon for a specific category name
     */
    fun getIconForCategory(categoryName: String, isCustom: Boolean = false): ImageVector {
        return when (categoryName) {
            SystemCategories.UNDEFINED -> Icons.Outlined.Help
            SystemCategories.GAME -> Icons.Outlined.SportsEsports
            SystemCategories.AUDIO -> Icons.Outlined.Headphones
            SystemCategories.VIDEO -> Icons.Outlined.VideoLibrary
            SystemCategories.IMAGE -> Icons.Outlined.Image
            SystemCategories.SOCIAL -> Icons.Outlined.Group
            SystemCategories.NEWS -> Icons.Outlined.Article
            SystemCategories.MAPS -> Icons.Outlined.Map
            SystemCategories.PRODUCTIVITY -> Icons.Outlined.Work
            SystemCategories.ACCESSIBILITY -> Icons.Outlined.Accessibility
            else -> {
                // For custom categories, try to find a matching icon or return default
                availableIcons.find { it.name == categoryName }?.icon ?: Icons.Outlined.Category
            }
        }
    }
    
    /**
     * Get icon by name (for custom category icon selection)
     */
    fun getIconByName(iconName: String): ImageVector {
        return availableIcons.find { it.name == iconName }?.icon ?: Icons.Outlined.Category
    }
    
    /**
     * Get all available icons for selection
     */
    fun getAvailableIcons(): List<IconOption> = availableIcons
    
    /**
     * Get system category names
     */
    fun getSystemCategoryNames(): List<String> = listOf(
        SystemCategories.UNDEFINED,
        SystemCategories.GAME,
        SystemCategories.AUDIO,
        SystemCategories.VIDEO,
        SystemCategories.IMAGE,
        SystemCategories.SOCIAL,
        SystemCategories.NEWS,
        SystemCategories.MAPS,
        SystemCategories.PRODUCTIVITY,
        SystemCategories.ACCESSIBILITY
    )
    
    /**
     * Predefined list of available icons for category customization
     */
    private val availableIcons = listOf(
        // General
        IconOption("Category", Icons.Outlined.Category),
        IconOption("Folder", Icons.Outlined.Folder),
        IconOption("Label", Icons.Outlined.Label),
        IconOption("Star", Icons.Outlined.Star),
        
        // Entertainment & Media
        IconOption("Games", Icons.Outlined.SportsEsports),
        IconOption("Headphones", Icons.Outlined.Headphones),
        IconOption("Music", Icons.Outlined.MusicNote),
        IconOption("Video", Icons.Outlined.VideoLibrary),
        IconOption("Movie", Icons.Outlined.Movie),
        IconOption("Camera", Icons.Outlined.CameraAlt),
        IconOption("Image", Icons.Outlined.Image),
        IconOption("Photo", Icons.Outlined.Photo),
        
        // Communication & Social
        IconOption("Group", Icons.Outlined.Group),
        IconOption("People", Icons.Outlined.People),
        IconOption("Chat", Icons.Outlined.Chat),
        IconOption("Message", Icons.Outlined.Message),
        IconOption("Email", Icons.Outlined.Email),
        IconOption("Phone", Icons.Outlined.Phone),
        IconOption("Contacts", Icons.Outlined.Contacts),
        
        // Productivity
        IconOption("Work", Icons.Outlined.Work),
        IconOption("Business", Icons.Outlined.Business),
        IconOption("Edit", Icons.Outlined.Edit),
        IconOption("Note", Icons.Outlined.Note),
        IconOption("Calendar", Icons.Outlined.CalendarMonth),
        IconOption("Event", Icons.Outlined.Event),
        IconOption("Task", Icons.Outlined.Task),
        IconOption("Check", Icons.Outlined.CheckCircle),
        
        // Information & Reading
        IconOption("Article", Icons.Outlined.Article),
        IconOption("Book", Icons.Outlined.Book),
        IconOption("Library", Icons.Outlined.LibraryBooks),
        IconOption("Newspaper", Icons.Outlined.Newspaper),
        
        // Navigation & Travel
        IconOption("Map", Icons.Outlined.Map),
        IconOption("Location", Icons.Outlined.LocationOn),
        IconOption("Explore", Icons.Outlined.Explore),
        IconOption("Navigation", Icons.Outlined.Navigation),
        
        // Shopping & Finance
        IconOption("Shopping", Icons.Outlined.ShoppingCart),
        IconOption("Store", Icons.Outlined.Store),
        IconOption("Payment", Icons.Outlined.Payment),
        IconOption("Wallet", Icons.Outlined.AccountBalanceWallet),
        
        // Health & Fitness
        IconOption("Health", Icons.Outlined.Favorite),
        IconOption("Fitness", Icons.Outlined.FitnessCenter),
        IconOption("Sports", Icons.Outlined.Sports),
        IconOption("Accessibility", Icons.Outlined.Accessibility),
        
        // Tools & Utilities
        IconOption("Settings", Icons.Outlined.Settings),
        IconOption("Tool", Icons.Outlined.Build),
        IconOption("Security", Icons.Outlined.Security),
        IconOption("Lock", Icons.Outlined.Lock),
        IconOption("Cloud", Icons.Outlined.Cloud),
        IconOption("Download", Icons.Outlined.Download),
        
        // Food & Lifestyle
        IconOption("Restaurant", Icons.Outlined.Restaurant),
        IconOption("Food", Icons.Outlined.Fastfood),
        IconOption("Coffee", Icons.Outlined.LocalCafe),
        IconOption("Home", Icons.Outlined.Home),
        
        // Education
        IconOption("School", Icons.Outlined.School),
        IconOption("Science", Icons.Outlined.Science),
        
        // Other
        IconOption("Help", Icons.Outlined.Help),
        IconOption("Info", Icons.Outlined.Info),
        IconOption("Warning", Icons.Outlined.Warning)
    )
}
