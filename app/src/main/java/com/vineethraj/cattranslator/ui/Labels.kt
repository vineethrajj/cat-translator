package com.vineethraj.cattranslator.ui

import com.vineethraj.cattranslator.mood.CatMood
import com.vineethraj.cattranslator.speech.CatIntent

fun CatMood.emoji(): String = when (this) {
    CatMood.CONTENT -> "😻"
    CatMood.WANTS_ATTENTION -> "👀"
    CatMood.HUNGRY -> "🍽️"
    CatMood.GREETING -> "👋"
    CatMood.PLAYFUL -> "🧶"
    CatMood.ANNOYED -> "😿"
    CatMood.DISTRESSED -> "🚨"
    CatMood.UNKNOWN -> "❓"
}

fun CatIntent.emoji(): String = when (this) {
    CatIntent.FOOD -> "🍽️"
    CatIntent.PRAISE -> "👍"
    CatIntent.SUMMON -> "📣"
    CatIntent.SCOLD -> "⚠️"
    CatIntent.DISMISS -> "🚪"
    CatIntent.PLAY -> "🧶"
    CatIntent.GREETING -> "👋"
    CatIntent.AFFECTION -> "❤️"
    CatIntent.UNKNOWN -> "❓"
}

private fun enumDisplayName(name: String): String = name.lowercase()
    .split('_')
    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

fun CatMood.displayName(): String = enumDisplayName(name)

fun CatIntent.displayName(): String = enumDisplayName(name)

/** Emoji for a raw history label that may be either a mood or an intent enum name. */
fun labelEmoji(label: String): String =
    runCatching { CatMood.valueOf(label).emoji() }.getOrNull()
        ?: runCatching { CatIntent.valueOf(label).emoji() }.getOrElse { "❓" }

/** Display name for a raw history label that may be either a mood or an intent enum name. */
fun labelDisplayName(label: String): String = enumDisplayName(label)
