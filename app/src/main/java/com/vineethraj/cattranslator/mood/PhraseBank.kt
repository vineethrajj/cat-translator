package com.vineethraj.cattranslator.mood

import kotlin.random.Random

/** For-fun "translated" phrases per mood. A random phrase is picked each time for variety. */
object PhraseBank {

    private val phrasesByMood: Map<CatMood, List<String>> = mapOf(
        CatMood.CONTENT to listOf(
            "I'm happy right now. Don't stop petting me.",
            "This is the good life. Keep doing what you're doing.",
            "Everything is fine. I approve of this moment.",
        ),
        CatMood.WANTS_ATTENTION to listOf(
            "Look at me. Look at me right now.",
            "I require your attention immediately.",
            "Excuse me, human, I am right here.",
        ),
        CatMood.HUNGRY to listOf(
            "The bowl is empty and this is an emergency.",
            "Feed me. I have been very patient.",
            "I would like food. Now would be good.",
        ),
        CatMood.GREETING to listOf(
            "Oh, it's you! Hello!",
            "You're home! Wonderful. Acknowledge me.",
            "Greetings, human. I missed you, probably.",
        ),
        CatMood.PLAYFUL to listOf(
            "I am feeling chaotic. Let's play.",
            "Something is about to get knocked off a shelf.",
            "Chase me. Or I will chase you.",
        ),
        CatMood.ANNOYED to listOf(
            "Back off. I am not in the mood.",
            "One more step and there will be consequences.",
            "That's a warning growl. Heed it.",
        ),
        CatMood.DISTRESSED to listOf(
            "Something is wrong and I need you to fix it.",
            "This is a distress call. Please investigate.",
            "I am not okay right now.",
        ),
        CatMood.UNKNOWN to listOf(
            "That didn't sound like a cat to me - try again closer to the mic.",
            "I couldn't quite make that out. One more try?",
            "Unclear signal. Get a bit closer and try again.",
        ),
    )

    fun phraseFor(mood: CatMood): String {
        val phrases = phrasesByMood.getValue(mood)
        return phrases[Random.nextInt(phrases.size)]
    }
}
