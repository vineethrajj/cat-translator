package com.vineethraj.cattranslator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(val emoji: String, val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        emoji = "🐱",
        title = "Welcome to Cat Translator",
        body = "Translate your cat's meows into moods and phrases, and talk back with " +
            "cat-like sounds. Every feature is free - no ads, no subscriptions, no locked translations.",
    ),
    OnboardingPage(
        emoji = "🎙️",
        title = "Getting good translations",
        body = "Record in a quiet room, hold the phone within a metre of your cat, and let " +
            "them finish their meow. The clearer the sound, the better the guess.",
    ),
    OnboardingPage(
        emoji = "🤝",
        title = "An honest note",
        body = "A real on-device sound classifier identifies meows, purrs, and hisses - and " +
            "your audio never leaves your phone. But nobody has scientifically decoded cat " +
            "language, so the mood we show is a fun best-effort guess, not veterinary advice. " +
            "If your cat seems unwell, trust your vet, not an app.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLast = pageIndex == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = page.emoji, fontSize = 72.sp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 20.dp),
        ) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == pageIndex) 10.dp else 8.dp)
                        .background(
                            if (i == pageIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                )
            }
        }

        Button(
            onClick = { if (isLast) onFinished() else pageIndex++ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLast) "Let's start" else "Next")
        }
        Box(modifier = Modifier.height(52.dp), contentAlignment = Alignment.Center) {
            if (!isLast) {
                TextButton(onClick = onFinished) {
                    Text("Skip")
                }
            }
        }
    }
}
