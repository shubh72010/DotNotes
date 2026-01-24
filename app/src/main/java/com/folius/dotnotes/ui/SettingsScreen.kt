package com.folius.dotnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onSecretNotesClick: () -> Unit
) {
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentModelId by viewModel.modelId.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val animationsEnabledPref by viewModel.isAnimationsEnabled.collectAsState()

    var apiKey by remember { mutableStateOf(currentApiKey ?: "") }
    var modelId by remember { mutableStateOf(currentModelId) }
    var animationsEnabled by remember { mutableStateOf(animationsEnabledPref) }

    LaunchedEffect(currentApiKey, currentModelId, animationsEnabledPref) {
        apiKey = currentApiKey ?: ""
        modelId = currentModelId
        animationsEnabled = animationsEnabledPref
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("OpenRouter API Key") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-or-v1-...") }
            )

            OutlinedTextField(
                value = modelId,
                onValueChange = { modelId = it },
                label = { Text("AI Model ID") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("google/gemini-flash-1.5") }
            )

            // Secret Notes Entry
            OutlinedButton(
                onClick = onSecretNotesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Icon(Icons.Default.Lock, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Access Secret Notes")
            }

            // Theme Selection
            Text("App Theme", style = MaterialTheme.typography.titleMedium)
            
            var selectedTheme by remember { mutableStateOf(currentTheme) }
            LaunchedEffect(currentTheme) { selectedTheme = currentTheme }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("System", "Light", "Dark").forEach { themeOption ->
                    FilterChip(
                        selected = selectedTheme == themeOption,
                        onClick = { selectedTheme = themeOption },
                        label = { Text(themeOption) }
                    )
                }
            }

            // Animations Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Enable Text Animations", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = animationsEnabled,
                    onCheckedChange = { animationsEnabled = it }
                )
            }

            Button(
                onClick = {
                    viewModel.saveSettings(apiKey, modelId, selectedTheme, animationsEnabled)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            Text(
                text = "You can get your API key from openrouter.ai. Common models: google/gemini-flash-1.5, anthropic/claude-3-haiku, etc.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
