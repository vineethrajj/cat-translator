package com.vineethraj.cattranslator.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vineethraj.cattranslator.viewmodel.CatToHumanUiState
import com.vineethraj.cattranslator.viewmodel.CatToHumanViewModel
import com.vineethraj.cattranslator.viewmodel.Detection
import com.vineethraj.cattranslator.ui.components.ErrorCard
import com.vineethraj.cattranslator.ui.components.MicButton
import com.vineethraj.cattranslator.ui.components.ResultCard

@Composable
fun CatToHumanScreen(viewModel: CatToHumanViewModel = viewModel()) {
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
    ) {
        when (val state = uiState) {
            is CatToHumanUiState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Listen to your cat",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tap start, then just let your cat be a cat - the app keeps listening " +
                            "and reacts on its own whenever it hears a meow, purr, or hiss.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    MicButton(isActive = false, onClick = { startListeningWithPermissionCheck() })

                    if (permissionDenied) {
                        Spacer(modifier = Modifier.height(24.dp))
                        ErrorCard(
                            "Microphone permission is required to hear your cat. If the prompt " +
                                "didn't reappear, enable it in Settings > Apps > Cat Translator > Permissions.",
                        )
                    }
                }
            }

            is CatToHumanUiState.Listening -> {
                ListeningHeader(onStop = { viewModel.stopListening() })
                Spacer(modifier = Modifier.height(24.dp))
                DetectionsArea(
                    detections = state.detections,
                    emptyMessage = "Listening for your cat - human voices and background " +
                        "noise are ignored.",
                )
            }

            is CatToHumanUiState.Stopped -> {
                Text(
                    if (state.detections.isEmpty()) {
                        "Session ended"
                    } else {
                        "Session ended - ${state.detections.size} sound${if (state.detections.size == 1) "" else "s"} translated"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(20.dp))
                DetectionsArea(
                    detections = state.detections,
                    emptyMessage = "No cat sounds were detected this time. Try moving closer " +
                        "or reducing background noise.",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.startListening() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Listen again")
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { viewModel.reset() }) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
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

@Composable
private fun ListeningHeader(onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
            Text(
                text = "Listening…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        OutlinedButton(
            onClick = onStop,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Stop")
        }
    }
}

@Composable
private fun DetectionsArea(detections: List<Detection>, emptyMessage: String) {
    if (detections.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    val latest = detections.first()
    ResultCard(
        emoji = latest.mood.emoji(),
        title = latest.catName?.let { "$it sounds: ${latest.mood.displayName()}" }
            ?: latest.mood.displayName(),
        body = "“${latest.phrase}”",
        subtitle = latest.matchedLabel?.let { "Heard: $it · ${latest.confidencePercent}% confidence" },
    )

    val earlier = detections.drop(1)
    if (earlier.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "EARLIER THIS SESSION",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            earlier.forEach { detection -> DetectionRow(detection) }
        }
    }
}

@Composable
private fun DetectionRow(detection: Detection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(detection.mood.emoji(), fontSize = 18.sp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = detection.mood.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detection.phrase,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}
