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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vineethraj.cattranslator.ui.components.MicButton
import com.vineethraj.cattranslator.ui.components.ResultCard
import com.vineethraj.cattranslator.viewmodel.CatToHumanUiState
import com.vineethraj.cattranslator.viewmodel.CatToHumanViewModel

@Composable
fun CatToHumanScreen(viewModel: CatToHumanViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.recordAndTranslate()
    }

    fun startRecordingWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.recordAndTranslate()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            is CatToHumanUiState.Idle -> {
                Text(
                    "Hold your phone near your cat and tap the mic to translate their meow.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                MicButton(isActive = false, onClick = { startRecordingWithPermissionCheck() })
            }

            is CatToHumanUiState.Recording -> {
                Text("Listening for 3 seconds...", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                MicButton(isActive = true, onClick = {})
            }

            is CatToHumanUiState.Processing -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Translating...", style = MaterialTheme.typography.bodyLarge)
            }

            is CatToHumanUiState.Result -> {
                ResultCard(
                    title = state.mood.name.replace('_', ' '),
                    body = "\"${state.phrase}\"",
                    subtitle = state.matchedLabel?.let { "Heard: $it (${state.confidencePercent}% confidence)" }
                        ?: "Couldn't confidently identify a cat sound.",
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Translate again")
                }
            }

            is CatToHumanUiState.Error -> {
                Text("Error: ${state.message}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Try again")
                }
            }
        }
    }
}
