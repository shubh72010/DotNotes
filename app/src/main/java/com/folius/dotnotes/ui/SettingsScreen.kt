package com.folius.dotnotes.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onSecretNotesClick: () -> Unit,
    onTrashClick: () -> Unit = {}
) {
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentModelId by viewModel.modelId.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val animationsEnabledPref by viewModel.isAnimationsEnabled.collectAsState()
    val currentStorageUri by viewModel.storageUri.collectAsState()
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    
    val context = LocalContext.current

    var apiKey by remember { mutableStateOf(currentApiKey ?: "") }
    var modelId by remember { mutableStateOf(currentModelId) }
    var animationsEnabled by remember { mutableStateOf(animationsEnabledPref) }
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    // Sync from VM
    var hasInitialized by remember { mutableStateOf(false) }
    if (!hasInitialized) {
        apiKey = currentApiKey ?: ""
        modelId = currentModelId
        animationsEnabled = animationsEnabledPref
        selectedTheme = currentTheme
        hasInitialized = true
    }

    var showExitWarning by remember { mutableStateOf(false) }
    val hasUnsavedChanges = apiKey != (currentApiKey ?: "") || 
                            modelId != currentModelId || 
                            animationsEnabled != animationsEnabledPref || 
                            selectedTheme != currentTheme

    val attemptBack = {
        if (hasUnsavedChanges) {
            showExitWarning = true
        } else {
            onBack()
        }
    }

    androidx.activity.compose.PredictiveBackHandler(enabled = hasUnsavedChanges) { progress ->
        try {
            progress.collect { /* No-op */ }
            showExitWarning = true
        } catch (e: Exception) {
            android.util.Log.e("Settings", "Predictive back failed", e)
        }
    }

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.setStorageUri(it.toString())
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importNotes(uris) { count ->
                Toast.makeText(context, "Imported $count note${if (count != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = attemptBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Secret Notes ────────────────────────────────
            OutlinedButton(
                onClick = onSecretNotesClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Icon(Icons.Default.Lock, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Access Secret Notes")
            }

            // ─── Trash ──────────────────────────────────────
            OutlinedButton(
                onClick = onTrashClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Icon(Icons.Default.Delete, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Trash (${deletedNotes.size})")
            }

            HorizontalDivider()

            // ─── Import Notes ────────────────────────────────
            Text("Import Notes", style = MaterialTheme.typography.titleMedium)
            
            OutlinedButton(
                onClick = {
                    importPicker.launch(arrayOf(
                        "text/plain",
                        "text/markdown",
                        "application/json",
                        "application/octet-stream"
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import .txt / .md / .json Files")
            }
            
            HorizontalDivider()

            // ─── Theme & Appearance ──────────────────────────
            Text("App Theme", style = MaterialTheme.typography.titleMedium)
            
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val themeOptions = listOf(
                    ThemeData("System", Color(0xFF6750A4), Color(0xFFF3EDF7)),
                    ThemeData("Material You", Color(0xFF4285F4), Color(0xFFFBBC05)),
                    ThemeData("Light", Color(0xFF6650A4), Color.White),
                    ThemeData("Dark", Color(0xFFD0BCFF), Color(0xFF1C1B1F))
                )
                
                themeOptions.forEach { data ->
                    ThemeSquircleCard(
                        name = data.name,
                        primaryColor = data.primary,
                        backgroundColor = data.background,
                        isSelected = selectedTheme == data.name,
                        onClick = { 
                            selectedTheme = data.name 
                            viewModel.saveSettings(apiKey, modelId, data.name, animationsEnabled)
                        }
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
                    onCheckedChange = { 
                        animationsEnabled = it 
                        viewModel.saveSettings(apiKey, modelId, selectedTheme, it)
                    }
                )
            }

            HorizontalDivider()

            // ─── AI Settings ─────────────────────────────────
            Text("AI Settings", style = MaterialTheme.typography.titleMedium)

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

            HorizontalDivider()

            // ─── Storage & Backup ────────────────────────────
            Text("Storage & Backup", style = MaterialTheme.typography.titleMedium)
            
            OutlinedButton(
                onClick = { storagePicker.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentStorageUri != null) "Change Backup Directory" else "Set Backup Directory")
            }
            
            if (currentStorageUri != null) {
                Button(
                    onClick = {
                        viewModel.backupNotes(
                            onSuccess = {
                                Toast.makeText(context, "Backup successful!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Notes Now")
                }
            }

            HorizontalDivider()

            Button(
                onClick = {
                    viewModel.saveSettings(apiKey, modelId, selectedTheme, animationsEnabled)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showExitWarning) {
            AlertDialog(
                onDismissRequest = { showExitWarning = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes in your settings. Save before leaving?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveSettings(apiKey, modelId, selectedTheme, animationsEnabled)
                        showExitWarning = false
                        onBack()
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExitWarning = false
                        onBack()
                    }) { Text("Discard") }
                }
            )
        }
    }
}

@Composable
fun ThemeSquircleCard(
    name: String,
    primaryColor: Color,
    backgroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(backgroundColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            if (name == "Material You") {
                // Multi-color dynamic preview
                Column(modifier = Modifier.size(32.dp)) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 8.dp)).background(Color(0xFF4285F4)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 8.dp)).background(Color(0xFFEA4335)))
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(bottomStart = 8.dp)).background(Color(0xFFFBBC05)))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(bottomEnd = 8.dp)).background(Color(0xFF34A853)))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (backgroundColor.red + backgroundColor.green + backgroundColor.blue > 1.5) primaryColor else Color.White,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
        )
    }
}

data class ThemeData(val name: String, val primary: Color, val background: Color)
