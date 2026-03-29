package com.folius.dotnotes.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.folius.dotnotes.ui.components.DecryptedText
import android.content.Context
import android.content.ContextWrapper
import com.folius.dotnotes.data.Note

private fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretNotesScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onNoteClick: (Int) -> Unit
) {
    val isAuthenticated by viewModel.isSecretAuthenticated.collectAsState()
    var authTrigger by remember { mutableIntStateOf(0) }
    var showBiometricUnavailableDialog by remember { mutableStateOf(false) }
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val secretNotes by viewModel.secretNotes.collectAsState()
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    // Check if biometric is available
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val canAuthenticate = remember(biometricManager) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }
    
    val currentContext by rememberUpdatedState(context)
    val currentViewModel by rememberUpdatedState(viewModel)
    
    val triggerBiometricAuth = remember(canAuthenticate) {
        {
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val activity = currentContext.findActivity()
                if (activity != null) {
                    val executor = ContextCompat.getMainExecutor(currentContext)
                    val callback = object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            currentViewModel.setSecretAuthenticated(true)
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Toast.makeText(currentContext, errString, Toast.LENGTH_SHORT).show()
                        }
                        
                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            // System handles retries
                        }
                    }
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Secret Notes")
                        .setSubtitle("Authenticate to access your hidden notes")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                        .build()
                        
                    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
                }
            } else {
                 showBiometricUnavailableDialog = true
            }
        }
    }

    // Manual trigger via button
    LaunchedEffect(authTrigger) {
        if (authTrigger > 0 && !isAuthenticated) {
            triggerBiometricAuth()
        }
    }

    // Initial trigger on entry
    LaunchedEffect(Unit) {
        if (!isAuthenticated) {
            triggerBiometricAuth()
        }
    }

    if (showBiometricUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricUnavailableDialog = false },
            title = { Text("Biometrics Unavailable") },
            text = { Text("Biometric authentication (or device credential) is not set up or available on this device. You won't be able to access secret notes.") },
            confirmButton = {
                TextButton(onClick = { 
                    showBiometricUnavailableDialog = false
                    onBack()
                }) {
                    Text("Go Back")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricUnavailableDialog = false }) {
                    Text("Stay Here")
                }
            }
        )
    }
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isAuthenticated) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Authentication Required")
                        Button(onClick = { 
                            authTrigger++
                        }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Unlock")
                        }
                    }
                }
            } else {
                if (secretNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No secret notes found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(secretNotes, key = { it.id }) { note ->
                            SecretNoteItem(note = note, animationsEnabled = animationsEnabled, onClick = { onNoteClick(note.id) })
                        }
                    }
                }
            }

            // DotPill overlay at bottom
            com.folius.dotnotes.ui.components.DotPill(
                modifier = Modifier.align(Alignment.BottomCenter),
                placeholderText = if (isAuthenticated) "Secret Notes" else "Locked",
                isMenuExpanded = isPillMenuExpanded,
                onMenuExpandedChange = { isPillMenuExpanded = it },
                onSwipeRight = onBack,
                menuContent = {
                    val contrastColor = MaterialTheme.colorScheme.onSurface
                    Text("Secret Actions", style = MaterialTheme.typography.titleMedium, color = contrastColor, modifier = Modifier.padding(bottom = 16.dp))
                    
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isAuthenticated) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(80.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { 
                                        isPillMenuExpanded = false
                                        authTrigger++ 
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Lock, null, tint = contrastColor, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Unlock", style = MaterialTheme.typography.labelSmall, color = contrastColor)
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(80.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable { 
                                    isPillMenuExpanded = false
                                    onBack() 
                                }
                                .padding(8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = contrastColor, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Back", style = MaterialTheme.typography.labelSmall, color = contrastColor)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SecretNoteItem(note: Note, animationsEnabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DecryptedText(
                text = if (note.title.isBlank()) "Untitled Secret" else note.title,
                style = MaterialTheme.typography.titleLarge,
                speed = if (animationsEnabled) 40 else 0,
                maxIterations = if (animationsEnabled) 10 else 0,
                encryptedColor = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val previewText = if (note.isChecklist) {
                note.checklist.joinToString(", ") { it.text }
            } else {
                note.content
            }
            
            if (previewText.isNotBlank()) {
                DecryptedText(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    speed = if (animationsEnabled) 20 else 0,
                    maxIterations = if (animationsEnabled) 10 else 0,
                    encryptedColor = MaterialTheme.colorScheme.secondary,
                    characters = "01",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
