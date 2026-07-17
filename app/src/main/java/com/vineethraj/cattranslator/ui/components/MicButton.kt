package com.vineethraj.cattranslator.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vineethraj.cattranslator.R

@Composable
fun MicButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(96.dp),
        containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = if (isActive) "Listening" else "Start recording",
            modifier = Modifier.size(40.dp),
        )
    }
}
