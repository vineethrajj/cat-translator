package com.vineethraj.cattranslator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onReplayOnboarding: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AboutCard(
            title = "How translation works",
            body = "A real machine-learning sound classifier (Google's YAMNet, running entirely " +
                "on your phone) identifies whether it heard a meow, purr, hiss, growl, or " +
                "caterwaul. We then combine that with the sound's pitch, length, and rhythm to " +
                "guess a mood and pick a fun phrase.",
        )
        AboutCard(
            title = "An honest note on the science",
            body = "Research shows cat sounds do vary with context, but nobody - including us - " +
                "has scientifically decoded cat language. The moods and phrases here are " +
                "best-effort entertainment, not veterinary advice. If your cat seems unwell, " +
                "please see a vet.",
        )
        AboutCard(
            title = "Your privacy",
            body = "Recordings are processed on your phone and are not uploaded anywhere. " +
                "History, cat profiles, and photos stay in this app's private storage on your " +
                "device. The only feature that may use the network is your phone's own speech " +
                "recognition service when you use the mic on the Speak screen.",
        )
        AboutCard(
            title = "Free means free",
            body = "Every translation, every mood, every sound - free. No ads, no subscriptions, " +
                "no locked features.",
        )
        TextButton(
            onClick = onReplayOnboarding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Replay the intro")
        }
    }
}

@Composable
private fun AboutCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
