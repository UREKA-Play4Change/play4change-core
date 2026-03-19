package com.ureka.play4change.core.model

data class Badge(
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val iconType: BadgeIconType,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null
)

enum class BadgeIconType {
    FIRST_STEP,
    FLAME,
    CALENDAR,
    STAR,
    CAMERA,
    COMPASS
}
