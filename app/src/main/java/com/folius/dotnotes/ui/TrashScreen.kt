package com.folius.dotnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.folius.dotnotes.data.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Empty Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (deletedNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Trash is empty", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Deleted notes are kept for 30 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${deletedNotes.size} item${if (deletedNotes.size != 1) "s" else ""} in trash",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(deletedNotes, key = { it.id }) { note ->
                    TrashNoteItem(
                        note = note,
                        onRestore = { viewModel.restoreNote(note) },
                        onPermanentDelete = { viewModel.permanentlyDeleteNote(note) }
                    )
                }
            }
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Permanently delete all ${deletedNotes.size} items? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrashNoteItem(
    note: Note,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val deletedDate = remember(note.deletedTimestamp) {
        note.deletedTimestamp?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"
    }

    val daysRemaining = remember(note.deletedTimestamp) {
        note.deletedTimestamp?.let {
            val thirtyDays = 30L * 24 * 60 * 60 * 1000
            val remaining = (it + thirtyDays - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
            maxOf(0, remaining).toInt()
        } ?: 30
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val preview = if (note.isChecklist) {
                note.checklist.joinToString(", ") { it.text }.take(80)
            } else {
                note.content.take(80)
            }
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Deleted $deletedDate · $daysRemaining days left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPermanentDelete) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
}
