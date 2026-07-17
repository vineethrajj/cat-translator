package com.vineethraj.cattranslator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vineethraj.cattranslator.speech.CatIntent
import com.vineethraj.cattranslator.speech.QuickPhrases
import com.vineethraj.cattranslator.ui.components.ErrorCard
import com.vineethraj.cattranslator.ui.components.MicButton
import com.vineethraj.cattranslator.ui.components.ResultCard
import com.vineethraj.cattranslator.viewmodel.HumanToCatUiState
import com.vineethraj.cattranslator.viewmodel.HumanToCatViewModel

private fun CatIntent.emoji(): String = when (this) {
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

private fun CatIntent.displayName(): String = name.lowercase()
    .split('_')
    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

@Composable
fun HumanToCatScreen(viewModel: HumanToCatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.startListening()
    }

    fun startListeningWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            permissionDenied = false
            viewModel.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            is HumanToCatUiState.Idle -> {
                Text(
                    "Speak to your cat",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Tap the mic and say something, or pick a quick phrase below.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(28.dp))
                MicButton(isActive = false, onClick = { startListeningWithPermissionCheck() })

                if (permissionDenied) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ErrorCard(
                        "Microphone permission is required to speak to your cat. If the prompt " +
                            "didn't reappear, enable it in Settings > Apps > Cat Translator > Permissions.",
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "QUICK PHRASES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                QuickPhraseGrid(onSelect = viewModel::selectQuickPhrase)
            }

            is HumanToCatUiState.Listening -> {
                Text(
                    "Listening...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Say something to your cat now.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(28.dp))
                MicButton(isActive = true, onClick = {})
            }

            is HumanToCatUiState.Result -> {
                ResultCard(
                    emoji = state.intent.emoji(),
                    title = state.intent.displayName(),
                    body = "You said: “${state.recognizedText}”",
                    subtitle = "Played a matching sound for your cat.",
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.replaySound() }) {
                    Text("Play again")
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text("Speak again", fontWeight = FontWeight.SemiBold)
                }
            }

            is HumanToCatUiState.Error -> {
                ErrorCard(state.message)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Or try a quick phrase instead:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                QuickPhraseGrid(onSelect = viewModel::selectQuickPhrase)
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text("Try the mic again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun QuickPhraseGrid(onSelect: (String, CatIntent) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickPhrases.all.forEach { phrase ->
            OutlinedButton(onClick = { onSelect(phrase.label, phrase.intent) }) {
                Text("${phrase.intent.emoji()}  ${phrase.label}")
            }
        }
    }
}
