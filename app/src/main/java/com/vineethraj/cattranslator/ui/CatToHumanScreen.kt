package com.vineethraj.cattranslator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.vineethraj.cattranslator.ui.components.ErrorCard
import com.vineethraj.cattranslator.ui.components.MicButton
import com.vineethraj.cattranslator.ui.components.ResultCard
import com.vineethraj.cattranslator.viewmodel.CatToHumanUiState
import com.vineethraj.cattranslator.viewmodel.CatToHumanViewModel

@Composable
fun CatToHumanScreen(viewModel: CatToHumanViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.recordAndTranslate()
    }

    fun startRecordingWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            permissionDenied = false
            viewModel.recordAndTranslate()
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
            is CatToHumanUiState.Idle -> {
                Text(
                    "Listen to your cat",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Hold your phone near your cat and tap the mic to translate their meow.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
                MicButton(isActive = false, onClick = { startRecordingWithPermissionCheck() })

                if (permissionDenied) {
                    Spacer(modifier = Modifier.height(24.dp))
                    ErrorCard(
                        "Microphone permission is required to hear your cat. If the prompt " +
                            "didn't reappear, enable it in Settings > Apps > Cat Translator > Permissions.",
                    )
                }
            }

            is CatToHumanUiState.Recording -> {
                Text(
                    "Listening...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Stay close for the next few seconds.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
                MicButton(isActive = true, onClick = {})
            }

            is CatToHumanUiState.Processing -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Translating...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is CatToHumanUiState.Result -> {
                ResultCard(
                    emoji = state.mood.emoji(),
                    title = state.catName?.let { "$it sounds: ${state.mood.displayName()}" }
                        ?: state.mood.displayName(),
                    body = "“${state.phrase}”",
                    subtitle = state.matchedLabel?.let { "Heard: $it · ${state.confidencePercent}% confidence" }
                        ?: "Couldn't confidently identify a cat sound - try moving closer.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Translate again")
                }
            }

            is CatToHumanUiState.Error -> {
                ErrorCard(state.message)
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text("Try again", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
