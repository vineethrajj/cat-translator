package com.vineethraj.cattranslator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.vineethraj.cattranslator.ui.AboutScreen
import com.vineethraj.cattranslator.ui.CatToHumanScreen
import com.vineethraj.cattranslator.ui.HistoryScreen
import com.vineethraj.cattranslator.ui.HomeDashboard
import com.vineethraj.cattranslator.ui.HumanToCatScreen
import com.vineethraj.cattranslator.ui.OnboardingScreen
import com.vineethraj.cattranslator.ui.ProfilesScreen
import com.vineethraj.cattranslator.ui.theme.CatTranslatorTheme

private const val PREFS_NAME = "settings"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

private enum class Screen(val title: String) {
    HOME("Cat Translator"),
    LISTEN("Listen to your cat"),
    SPEAK("Speak to your cat"),
    HISTORY("History"),
    PROFILES("My Cats"),
    ABOUT("About"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatTranslatorTheme {
                CatTranslatorApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatTranslatorApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var onboardingDone by remember { mutableStateOf(prefs.getBoolean(KEY_ONBOARDING_DONE, false)) }
    var screen by remember { mutableStateOf(Screen.HOME) }

    if (!onboardingDone) {
        OnboardingScreen(
            onFinished = {
                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                onboardingDone = true
            },
        )
        return
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(screen.title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    if (screen != Screen.HOME) {
                        IconButton(onClick = { screen = Screen.HOME }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Back to home",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (screen) {
                Screen.HOME -> HomeDashboard(
                    onListen = { screen = Screen.LISTEN },
                    onSpeak = { screen = Screen.SPEAK },
                    onHistory = { screen = Screen.HISTORY },
                    onProfiles = { screen = Screen.PROFILES },
                    onAbout = { screen = Screen.ABOUT },
                )
                Screen.LISTEN -> CatToHumanScreen()
                Screen.SPEAK -> HumanToCatScreen()
                Screen.HISTORY -> HistoryScreen()
                Screen.PROFILES -> ProfilesScreen()
                Screen.ABOUT -> AboutScreen(
                    onReplayOnboarding = {
                        onboardingDone = false
                    },
                )
            }
        }
    }
}
