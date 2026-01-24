package com.folius.dotnotes.ui

import android.os.Bundle
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.folius.dotnotes.ui.components.DecryptedText
import com.folius.dotnotes.ui.components.RevealDirection
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
    var isAuthenticated by remember { mutableStateOf(false) }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val secretNotes by viewModel.secretNotes.collectAsState()
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    // Check if biometric is available
    val biometricManager = BiometricManager.from(context)
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    
    LaunchedEffect(showBiometricPrompt) {
        if (!showBiometricPrompt || isAuthenticated) return@LaunchedEffect
        
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val activity = context.findActivity()
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(context)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated = true
                        showBiometricPrompt = false
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                        showBiometricPrompt = false
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                        showBiometricPrompt = false
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
             Toast.makeText(context, "Biometric authentication not installed/available", Toast.LENGTH_LONG).show()
             onBack()
        }
    }

    // Trigger on entry
    LaunchedEffect(Unit) {
        showBiometricPrompt = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secret Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!isAuthenticated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                        showBiometricPrompt = false
                        showBiometricPrompt = true
                    }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Unlock")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (secretNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No secret notes found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(secretNotes) { note ->
                            SecretNoteItem(note = note, animationsEnabled = animationsEnabled, onClick = { onNoteClick(note.id) })
                        }
                    }
                }
            }
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
                encryptedColor = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val previewText = if (note.isChecklist) {
                note.checklist.joinToString(", ") { it.text }
            } else {
                note.content
            }
            
            val truncatedPreview = if (previewText.length > 80) {
                previewText.take(80) + "..."
            } else {
                previewText
            }
            
            if (truncatedPreview.isNotBlank()) {
                DecryptedText(
                    text = truncatedPreview,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    speed = if (animationsEnabled) 20 else 0,
                    maxIterations = if (animationsEnabled) 10 else 0,
                    encryptedColor = MaterialTheme.colorScheme.secondary,
                    characters = "01"
                )
            }
        }
    }
}
