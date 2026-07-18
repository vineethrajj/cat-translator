package com.vineethraj.cattranslator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Surface
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

@Composable
fun HumanToCatScreen(viewModel: HumanToCatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Long-press a phrase to pin it first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                QuickPhraseGrid(
                    favorites = favorites,
                    onSelect = viewModel::selectQuickPhrase,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
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
                QuickPhraseGrid(
                    favorites = favorites,
                    onSelect = viewModel::selectQuickPhrase,
                    onToggleFavorite = viewModel::toggleFavorite,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text("Try the mic again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
private fun QuickPhraseGrid(
    favorites: List<String>,
    onSelect: (String, CatIntent) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickPhrases.ordered(favorites).forEach { phrase ->
            val isPinned = phrase.label in favorites
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isPinned) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
                modifier = Modifier.combinedClickable(
                    onClick = { onSelect(phrase.label, phrase.intent) },
                    onLongClick = { onToggleFavorite(phrase.label) },
                ),
            ) {
                Text(
                    text = buildString {
                        if (isPinned) append("⭐ ")
                        append(phrase.intent.emoji())
                        append("  ")
                        append(phrase.label)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}
