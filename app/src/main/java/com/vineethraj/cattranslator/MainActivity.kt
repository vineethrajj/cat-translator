package com.vineethraj.cattranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vineethraj.cattranslator.ui.CatToHumanScreen
import com.vineethraj.cattranslator.ui.HumanToCatScreen
import com.vineethraj.cattranslator.ui.theme.CatTranslatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatTranslatorTheme {
                CatTranslatorApp()
            }
        }
    }
}

@Composable
private fun CatTranslatorApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cat -> You", "You -> Cat")

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> CatToHumanScreen()
                else -> HumanToCatScreen()
            }
        }
    }
}
