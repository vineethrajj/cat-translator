package com.vineethraj.cattranslator.speech

/** A canned phrase for the "You -> Cat" screen that bypasses speech recognition entirely. */
data class QuickPhrase(val label: String, val intent: CatIntent)

object QuickPhrases {
    val all: List<QuickPhrase> = listOf(
        QuickPhrase("Come here!", CatIntent.SUMMON),
        QuickPhrase("Dinner time!", CatIntent.FOOD),
        QuickPhrase("Good kitty!", CatIntent.PRAISE),
        QuickPhrase("I love you", CatIntent.AFFECTION),
        QuickPhrase("Let's play!", CatIntent.PLAY),
        QuickPhrase("Hello!", CatIntent.GREETING),
        QuickPhrase("No!", CatIntent.SCOLD),
        QuickPhrase("Go on, shoo", CatIntent.DISMISS),
    )

    /** Pinned phrases first (in the order they were pinned), then the rest in default order. */
    fun ordered(pinnedLabels: List<String>): List<QuickPhrase> {
        val byLabel = all.associateBy { it.label }
        val pinned = pinnedLabels.mapNotNull { byLabel[it] }
        return pinned + (all - pinned.toSet())
    }
}
