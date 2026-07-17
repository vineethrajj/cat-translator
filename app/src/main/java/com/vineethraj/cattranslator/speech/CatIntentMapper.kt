package com.vineethraj.cattranslator.speech

enum class CatIntent {
    FOOD,
    PRAISE,
    SUMMON,
    SCOLD,
    DISMISS,
    PLAY,
    GREETING,
    AFFECTION,
    UNKNOWN,
}

/** Maps recognized speech text to a cat-relevant intent via keyword matching. */
object CatIntentMapper {

    private val keywordsByIntent: Map<CatIntent, List<String>> = mapOf(
        CatIntent.FOOD to listOf(
            "food", "dinner", "breakfast", "lunch", "hungry", "treat", "treats", "eat", "feed", "snack", "yummy",
        ),
        CatIntent.PRAISE to listOf(
            "good boy", "good girl", "good kitty", "well done", "clever", "smart", "nice job",
        ),
        CatIntent.SUMMON to listOf(
            "come here", "here kitty", "here, kitty", "come on", "over here", "come",
        ),
        CatIntent.SCOLD to listOf(
            "get down", "cut it out", "don't", "stop", "bad", "off", "no",
        ),
        CatIntent.DISMISS to listOf(
            "go on", "go away", "go there", "get going", "shoo", "scram", "leave",
        ),
        CatIntent.PLAY to listOf(
            "play", "toy", "chase", "fetch", "pounce", "laser", "string", "run",
        ),
        CatIntent.GREETING to listOf(
            "good morning", "hey there", "hello", "hi", "morning", "welcome", "miss you",
        ),
        CatIntent.AFFECTION to listOf(
            "love", "cuddle", "snuggle", "sweet", "cutie", "beautiful", "handsome", "adorable", "pet",
        ),
    )

    fun map(text: String): CatIntent {
        val normalized = text.lowercase()
        for ((intent, keywords) in keywordsByIntent) {
            if (keywords.any { matches(normalized, it) }) {
                return intent
            }
        }
        return CatIntent.UNKNOWN
    }

    private fun matches(normalizedText: String, keyword: String): Boolean {
        return if (keyword.contains(' ')) {
            normalizedText.contains(keyword)
        } else {
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(normalizedText)
        }
    }
}
