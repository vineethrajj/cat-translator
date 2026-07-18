package com.vineethraj.cattranslator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vineethraj.cattranslator.data.TranslationDirection
import com.vineethraj.cattranslator.mood.MoodTrends
import com.vineethraj.cattranslator.ui.components.TrendCard
import com.vineethraj.cattranslator.viewmodel.HistoryViewModel
import com.vineethraj.cattranslator.viewmodel.ProfilesViewModel

@Composable
fun HomeDashboard(
    onListen: () -> Unit,
    onSpeak: () -> Unit,
    onHistory: () -> Unit,
    onProfiles: () -> Unit,
    onAbout: () -> Unit,
    historyViewModel: HistoryViewModel = viewModel(),
    profilesViewModel: ProfilesViewModel = viewModel(),
) {
    val entries by historyViewModel.entries.collectAsState()
    val profiles by profilesViewModel.state.collectAsState()

    LaunchedEffect(Unit) { historyViewModel.refresh() }

    val trends = remember(entries) { MoodTrends.aggregate(entries, System.currentTimeMillis()) }
    val recent = remember(entries) { entries.take(3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        profiles.activeProfile?.let { cat ->
            Text(
                text = "Translating for ${cat.name} 🐾",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ActionCard(
            emoji = "🐱",
            title = "Listen to your cat",
            description = "Record a meow and find out what it might mean.",
            onClick = onListen,
        )
        ActionCard(
            emoji = "🗣️",
            title = "Speak to your cat",
            description = "Say something or pick a phrase, and play it back in cat.",
            onClick = onSpeak,
        )

        if (trends.isNotEmpty()) {
            TrendCard(counts = trends)
        }

        if (recent.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onHistory) { Text("See all") }
                }
                recent.forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onHistory)
                            .padding(vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(labelEmoji(entry.label), fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = if (entry.direction == TranslationDirection.CAT_TO_HUMAN) {
                                    labelDisplayName(entry.label)
                                } else {
                                    "You: ${labelDisplayName(entry.label)}"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallLinkCard(emoji = "📜", label = "History", modifier = Modifier.weight(1f), onClick = onHistory)
            SmallLinkCard(emoji = "🐾", label = "My Cats", modifier = Modifier.weight(1f), onClick = onProfiles)
            SmallLinkCard(emoji = "ℹ️", label = "About", modifier = Modifier.weight(1f), onClick = onAbout)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, fontSize = 40.sp)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SmallLinkCard(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
