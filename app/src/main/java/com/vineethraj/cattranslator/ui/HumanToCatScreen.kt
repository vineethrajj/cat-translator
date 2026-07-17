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
import com.vineethraj.cattranslator.viewmodel.HumanToCatUiState
import com.vineethraj.cattranslator.viewmodel.HumanToCatViewModel

@Composable
fun HumanToCatScreen(viewModel: HumanToCatViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    fun startListeningWithPermissionCheck() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.startListening()
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
            is HumanToCatUiState.Idle -> {
                Text(
                    "Tap the mic and say something to your cat.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                MicButton(isActive = false, onClick = { startListeningWithPermissionCheck() })
            }

            is HumanToCatUiState.Listening -> {
                Text("Listening...", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                MicButton(isActive = true, onClick = {})
            }

            is HumanToCatUiState.Result -> {
                ResultCard(
                    title = state.intent.name.replace('_', ' '),
                    body = "You said: \"${state.recognizedText}\"",
                    subtitle = "Played a matching sound for your cat.",
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.replaySound() }) {
                    Text("Play again")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Speak again")
                }
            }

            is HumanToCatUiState.Error -> {
                Text("Error: ${state.message}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.reset() }) {
                    Text("Try again")
                }
            }
        }
    }
}
