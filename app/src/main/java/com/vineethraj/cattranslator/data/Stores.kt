package com.vineethraj.cattranslator.data

import android.content.Context
import java.io.File

/** Process-wide singletons so every ViewModel sees the same store instances. */
object Stores {

    @Volatile private var history: TranslationHistoryStore? = null
    @Volatile private var profiles: CatProfileStore? = null
    @Volatile private var favorites: FavoritesStore? = null

    fun history(context: Context): TranslationHistoryStore =
        history ?: synchronized(this) {
            history ?: TranslationHistoryStore(File(context.applicationContext.filesDir, "history.json"))
                .also { history = it }
        }

    fun profiles(context: Context): CatProfileStore =
        profiles ?: synchronized(this) {
            profiles ?: CatProfileStore(File(context.applicationContext.filesDir, "profiles.json"))
                .also { profiles = it }
        }

    fun favorites(context: Context): FavoritesStore =
        favorites ?: synchronized(this) {
            favorites ?: FavoritesStore(File(context.applicationContext.filesDir, "favorites.json"))
                .also { favorites = it }
        }
}
