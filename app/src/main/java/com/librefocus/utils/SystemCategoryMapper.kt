package com.librefocus.utils

import android.content.Context
import android.content.pm.ApplicationInfo

object SystemCategoryMapper {
    
    fun getSystemCategories(): List<SystemCategory> = listOf(
        SystemCategory(ApplicationInfo.CATEGORY_UNDEFINED, "Undefined"),
        SystemCategory(ApplicationInfo.CATEGORY_GAME, "Game"),
        SystemCategory(ApplicationInfo.CATEGORY_AUDIO, "Audio"),
        SystemCategory(ApplicationInfo.CATEGORY_VIDEO, "Video"),
        SystemCategory(ApplicationInfo.CATEGORY_IMAGE, "Image"),
        SystemCategory(ApplicationInfo.CATEGORY_SOCIAL, "Social"),
        SystemCategory(ApplicationInfo.CATEGORY_NEWS, "News"),
        SystemCategory(ApplicationInfo.CATEGORY_MAPS, "Maps"),
        SystemCategory(ApplicationInfo.CATEGORY_PRODUCTIVITY, "Productivity"),
        SystemCategory(ApplicationInfo.CATEGORY_ACCESSIBILITY, "Accessibility")
    )
    
    fun getLocalizedCategoryName(context: Context, systemCategoryId: Int): String {
        val categoryTitle = ApplicationInfo.getCategoryTitle(context, systemCategoryId)
        return categoryTitle?.toString() ?: getSystemCategories()
            .find { it.id == systemCategoryId }?.fallbackName ?: "Uncategorized"
    }
    
    fun isSystemCategory(systemCategoryId: Int?): Boolean {
        if (systemCategoryId == null) return false
        return getSystemCategories().any { it.id == systemCategoryId }
    }
}

data class SystemCategory(
    val id: Int,
    val fallbackName: String
)
