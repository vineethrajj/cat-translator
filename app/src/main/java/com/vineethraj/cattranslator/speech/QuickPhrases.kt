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
}
