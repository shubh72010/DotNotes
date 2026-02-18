package com.folius.dotnotes.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.folius.dotnotes.ui.components.BlurText
import com.folius.dotnotes.utils.MarkdownRenderer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.folius.dotnotes.data.ChecklistItem
import com.folius.dotnotes.data.Note
import kotlinx.coroutines.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Note color presets
val NoteColors = listOf(
    null to "None",
    0xFFEF5350.toInt() to "Red",
    0xFFFF7043.toInt() to "Orange",
    0xFFFFCA28.toInt() to "Yellow",
    0xFF66BB6A.toInt() to "Green",
    0xFF26A69A.toInt() to "Teal",
    0xFF42A5F5.toInt() to "Blue",
    0xFFAB47BC.toInt() to "Purple",
    0xFFEC407A.toInt() to "Pink"
)

// Helper for resolving [[Wiki Links]] to Note IDs
private suspend fun resolveWikiLinks(content: String, viewModel: NoteViewModel): List<Int> {
    val regex = Regex("\\[\\[(.*?)\\]\\]")
    return regex.findAll(content)
        .map { it.groupValues[1] }
        .toSet()
        .mapNotNull { title -> viewModel.getNoteByTitle(title)?.id }
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onNavigateToNote: ((Int) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isChecklist by remember { mutableStateOf(false) }
    var checklist by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var folderId by remember { mutableStateOf<Int?>(null) }
    var isSecret by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var currentNoteId by remember { mutableStateOf(noteId) }
    var linkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var noteColor by remember { mutableStateOf<Int?>(null) }
    var isMap by remember { mutableStateOf(false) }
    var mapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    
    var isSummarizing by remember { mutableStateOf(false) }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var showAiChat by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    var showTitleAnimation by remember { mutableStateOf(animationsEnabled && title.isNotBlank()) }
    var showContentAnimation by remember { mutableStateOf(animationsEnabled && content.isNotBlank()) }
    
    var initialTitle by remember { mutableStateOf("") }
    var initialContent by remember { mutableStateOf("") }
    var initialImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialIsChecklist by remember { mutableStateOf(false) }
    var initialChecklist by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var initialFolderId by remember { mutableStateOf<Int?>(null) }
    var initialIsSecret by remember { mutableStateOf(false) }
    var initialIsPinned by remember { mutableStateOf(false) }
    var initialIsMap by remember { mutableStateOf(false) }
    var initialMapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    var initialTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialNoteColor by remember { mutableStateOf<Int?>(null) }
    var initialLinkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    var showExitWarning by remember { mutableStateOf(false) }
    var showFolderDropdown by remember { mutableStateOf(false) }
    
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current
    
    val hasUnsavedChanges = title != initialTitle || 
                            content != initialContent || 
                            images != initialImages || 
                            isChecklist != initialIsChecklist || 
                            checklist != initialChecklist ||
                            folderId != initialFolderId ||
                            isSecret != initialIsSecret ||
                            isPinned != initialIsPinned ||
                            tags != initialTags ||
                            noteColor != initialNoteColor ||
                            isMap != initialIsMap ||
                            linkedNoteIds != initialLinkedNoteIds
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // â”€â”€â”€ Word Count â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val wordCount = remember(content, checklist, isChecklist) {
        val text = if (isChecklist) {
            checklist.joinToString(" ") { it.text }
        } else {
            content
        }
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(content, checklist, isChecklist) {
        if (isChecklist) {
            checklist.sumOf { it.text.length }
        } else {
            content.length
        }
    }

    // â”€â”€â”€ Autosave (increased debounce to 1000ms for perf) â”€â”€â”€
    LaunchedEffect(title, content, images, isChecklist, checklist, folderId, isSecret, isPinned, tags, noteColor, isMap, mapItems) {
        if (title.isBlank() && content.isBlank() && checklist.isEmpty() && images.isEmpty() && !isMap) return@LaunchedEffect
        
        delay(1000) // Debounce 1s for efficiency
        
        linkedNoteIds = resolveWikiLinks(content, viewModel)
        
        val newId = viewModel.saveNote(
            id = currentNoteId,
            title = title,
            content = content,
            images = images,
            isChecklist = isChecklist,
            checklist = checklist,
            folderId = folderId,
            isSecret = isSecret,
            isPinned = isPinned,
            linkedNoteIds = linkedNoteIds,
            tags = tags,
            color = noteColor,
            isMap = isMap,
            mapItems = mapItems
        )
        if (currentNoteId == null || currentNoteId == -1) {
            currentNoteId = newId
        }
    }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != -1) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                content = note.content
                images = note.images
                isChecklist = note.isChecklist
                checklist = note.checklist
                folderId = note.folderId
                isPinned = note.isPinned
                isMap = note.isMap
                mapItems = note.mapItems
                linkedNoteIds = note.linkedNoteIds
                tags = note.tags
                noteColor = note.color
                
                initialTitle = note.title
                initialContent = note.content
                initialImages = note.images
                initialIsChecklist = note.isChecklist
                initialChecklist = note.checklist
                initialFolderId = note.folderId
                initialIsSecret = note.isSecret
                initialIsPinned = note.isPinned
                initialIsMap = note.isMap
                initialMapItems = note.mapItems
                initialTags = note.tags
                initialNoteColor = note.color
                initialLinkedNoteIds = note.linkedNoteIds

                if (animationsEnabled) {
                    showTitleAnimation = note.title.isNotBlank()
                    showContentAnimation = note.content.isNotBlank()
                }
            }
        } else {
             folderId = viewModel.selectedFolderId.value
             initialFolderId = viewModel.selectedFolderId.value
        }
    }

    // Stability: Animation auto-dismiss logic hoited out of code branches
    LaunchedEffect(showTitleAnimation) {
        if (!showTitleAnimation || title.isBlank()) return@LaunchedEffect
        val elements = title.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
        delay((elements.size * 200L) + 1000L)
        showTitleAnimation = false
    }

    LaunchedEffect(showContentAnimation) {
        if (!showContentAnimation || content.isBlank()) return@LaunchedEffect
        val elements = content.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
        delay((elements.size * 20L) + 1000L)
        showContentAnimation = false
    }


    if (isMap && currentNoteId != null && currentNoteId != -1) {
        NoteMapScreen(
            noteId = currentNoteId!!,
            viewModel = viewModel,
            mapItems = mapItems,
            onMapItemsChange = { mapItems = it },
            onNavigateToNote = { id -> onNavigateToNote?.invoke(id) },
            onBack = onBack
        )
        return
    }

    // Load Backlinks
    var backlinks by remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(currentNoteId) {
        if (currentNoteId != null && currentNoteId != -1) {
            backlinks = viewModel.getBacklinks(currentNoteId!!)
        }
    }


    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val newImages = uris.map { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
            uri.toString()
        }
        images = images + newImages
    }

    val attemptBack = {
        if (hasUnsavedChanges) {
            showExitWarning = true
        } else {
            onBack()
        }
    }

    PredictiveBackHandler(enabled = hasUnsavedChanges) { progress ->
        try {
            progress.collect { /* No-op */ }
            showExitWarning = true
        } catch (e: Exception) {
            android.util.Log.e("NoteEditor", "Predictive back failed", e)
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                // Word count bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$wordCount words Â· $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (tags.isNotEmpty()) {
                            Text(
                                tags.joinToString(", ") { "#$it" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }

                BottomAppBar(
                    actions = {
                        IconButton(onClick = attemptBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        if (showTitleAnimation || showContentAnimation) {
                            IconButton(onClick = {
                                showTitleAnimation = false
                                showContentAnimation = false
                            }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip Animation")
                            }
                        }

                        // Markdown preview toggle
                        if (!isChecklist) {
                            IconButton(onClick = { showMarkdownPreview = !showMarkdownPreview }) {
                                Icon(
                                    if (showMarkdownPreview) Icons.Default.Edit else Icons.Default.RemoveRedEye,
                                    contentDescription = if (showMarkdownPreview) "Edit" else "Preview",
                                    tint = if (showMarkdownPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {
                            scope.launch {
                                linkedNoteIds = resolveWikiLinks(content, viewModel)

                                 viewModel.saveNote(
                                     id = currentNoteId,
                                     title = title,
                                     content = content,
                                     images = images,
                                     isChecklist = isChecklist,
                                     checklist = checklist,
                                     folderId = folderId,
                                     isSecret = isSecret,
                                     isPinned = isPinned,
                                     linkedNoteIds = linkedNoteIds,
                                     tags = tags,
                                     color = noteColor,
                                     isMap = isMap,
                                     mapItems = mapItems
                                 )
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Save")
                        }

                        var showMenu by remember { mutableStateOf(false) }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // Move to Folder
                                DropdownMenuItem(
                                    text = { Text("Move to Folder") },
                                    onClick = {
                                        showMenu = false
                                        showFolderDropdown = true 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (isSecret) "Remove from Secret" else "Make Secret") },
                                    onClick = {
                                        showMenu = false
                                        isSecret = !isSecret
                                        Toast.makeText(context, if (isSecret) "Added to Secret Notes" else "Removed from Secret Notes", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(if (isSecret) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null) }
                                )

                                // Share Note
                                DropdownMenuItem(
                                    text = { Text("Share Note") },
                                    onClick = {
                                        showMenu = false
                                        val shareIntent = Intent().apply {
                                            action = if (images.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
                                            val shareText = buildString {
                                                if (title.isNotBlank()) append("$title\n\n")
                                                if (isChecklist) {
                                                    append(checklist.joinToString("\n") { "${if (it.isChecked) "[x]" else "[ ]"} ${it.text}" })
                                                } else {
                                                    append(content)
                                                }
                                            }
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            if (images.isNotEmpty()) {
                                                val imageUris = images.map { Uri.parse(it) }
                                                if (images.size > 1) {
                                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                                                } else {
                                                    putExtra(Intent.EXTRA_STREAM, imageUris.first())
                                                }
                                                type = "image/*"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            } else {
                                                type = "text/plain"
                                            }
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Note via"))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                )

                                // Add Image
                                DropdownMenuItem(
                                    text = { Text("Add Image") },
                                    onClick = {
                                        showMenu = false
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                )

                                // Toggle Checklist
                                DropdownMenuItem(
                                    text = { Text(if (isChecklist) "Hide Checklist" else "Show Checklist") },
                                    onClick = {
                                        showMenu = false
                                        val newIsChecklist = !isChecklist
                                        if (newIsChecklist && checklist.isEmpty() && content.isNotBlank()) {
                                            checklist = content.lines().filter { it.isNotBlank() }.map { 
                                                ChecklistItem(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    text = it
                                                ) 
                                            }
                                            content = ""
                                        } else if (!newIsChecklist && content.isBlank() && checklist.isNotEmpty()) {
                                            content = checklist.joinToString("\n") { it.text }
                                            checklist = emptyList()
                                        }
                                        isChecklist = newIsChecklist
                                        showMarkdownPreview = false
                                    },
                                    leadingIcon = {
                                        Icon(if (isChecklist) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, contentDescription = null)
                                    }
                                )

                                // Pin/Unpin
                                DropdownMenuItem(
                                    text = { Text(if (isPinned) "Unpin Note" else "Pin Note") },
                                    onClick = {
                                        showMenu = false
                                        isPinned = !isPinned
                                        Toast.makeText(context, if (isPinned) "Note Pinned" else "Note Unpinned", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                )

                                // Tags
                                DropdownMenuItem(
                                    text = { Text("Tags") },
                                    onClick = {
                                        showMenu = false
                                        showTagInput = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                                )

                                // Color
                                DropdownMenuItem(
                                    text = { Text("Note Color") },
                                    onClick = {
                                        showMenu = false
                                        showColorPicker = true
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Palette, 
                                            contentDescription = null,
                                            tint = noteColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    }
                                )

                                // Duplicate
                                if (existingNote != null || (currentNoteId != null && currentNoteId != -1)) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicate Note") },
                                        onClick = {
                                            showMenu = false
                                            scope.launch {
                                                val noteToClone = viewModel.getNoteById(currentNoteId ?: -1)
                                                if (noteToClone != null) {
                                                    viewModel.duplicateNote(noteToClone)
                                                    Toast.makeText(context, "Note duplicated", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                                    )
                                }

                                // Convert to Map
                                if (!isMap) {
                                    DropdownMenuItem(
                                        text = { Text("Convert to Map") },
                                        onClick = {
                                            showMenu = false
                                            scope.launch {
                                                 val newId = viewModel.saveNote(
                                                     id = currentNoteId,
                                                     title = title,
                                                     content = content,
                                                     images = images,
                                                     isChecklist = isChecklist,
                                                     checklist = checklist,
                                                     folderId = folderId,
                                                     isSecret = isSecret,
                                                     isPinned = isPinned,
                                                     linkedNoteIds = linkedNoteIds,
                                                     tags = tags,
                                                     color = noteColor,
                                                     isMap = true,
                                                     mapItems = mapItems
                                                 )
                                                 if (currentNoteId == null || currentNoteId == -1) {
                                                     currentNoteId = newId
                                                 }
                                                 isMap = true
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) }
                                    )
                                }

                                // Summarize
                                if (content.isNotBlank() && !isChecklist) {
                                    DropdownMenuItem(
                                        text = { Text("Summarize") },
                                        onClick = {
                                            showMenu = false
                                            showAiChat = true
                                            scope.launch {
                                                isSummarizing = true
                                                aiResponse = "Generating summary..."
                                                viewModel.summarizeNote(content) { summary ->
                                                    aiResponse = summary ?: "Failed to generate summary."
                                                    isSummarizing = false
                                                }
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                                    )
                                }

                                // Delete
                                if (existingNote != null || (currentNoteId != null && currentNoteId != -1)) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            val idToDelete = currentNoteId ?: existingNote?.id
                                            idToDelete?.let { viewModel.trashNoteById(it) }
                                            onBack()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        // Folder Selection Dialog
        if (showFolderDropdown) {
             AlertDialog(
                 onDismissRequest = { showFolderDropdown = false },
                 title = { Text("Select Folder") },
                 text = {
                     LazyColumn {
                         item {
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clickable {
                                         folderId = null
                                         showFolderDropdown = false
                                     }
                                     .padding(16.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                 Spacer(modifier = Modifier.width(16.dp))
                                 Text("None (All Notes)", style = MaterialTheme.typography.bodyLarge)
                                 if (folderId == null) {
                                     Spacer(modifier = Modifier.weight(1f))
                                     Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                 }
                             }
                         }
                         items(folders) { folder ->
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .clickable {
                                         folderId = folder.id
                                         showFolderDropdown = false
                                     }
                                     .padding(16.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                 Spacer(modifier = Modifier.width(16.dp))
                                 Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                                 if (folderId == folder.id) {
                                     Spacer(modifier = Modifier.weight(1f))
                                     Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                 }
                             }
                         }
                     }
                 },
                 confirmButton = {
                     TextButton(onClick = { showFolderDropdown = false }) {
                         Text("Cancel")
                     }
                 }
             )
        }

        // Tag Input Dialog
        if (showTagInput) {
            AlertDialog(
                onDismissRequest = { showTagInput = false },
                title = { Text("Tags") },
                text = {
                    Column {
                        // Existing tags
                        if (tags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = { tags = tags - tag },
                                        label = { Text("#$tag") },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it.replace(" ", "_") },
                            placeholder = { Text("Add a tag...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (newTagText.isNotBlank() && newTagText !in tags) {
                                            tags = tags + newTagText
                                            newTagText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTagInput = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Color Picker Dialog
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Note Color") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NoteColors.forEach { (colorVal, label) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colorVal == null) MaterialTheme.colorScheme.surfaceVariant
                                        else Color(colorVal)
                                    )
                                    .clickable {
                                        noteColor = colorVal
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (noteColor == colorVal) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (colorVal == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (colorVal == null) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "No Color",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Exit warning dialog
        if (showExitWarning) {
            AlertDialog(
                onDismissRequest = { showExitWarning = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Save before leaving?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            viewModel.saveNote(
                                id = currentNoteId,
                                title = title,
                                content = content,
                                images = images,
                                isChecklist = isChecklist,
                                checklist = checklist,
                                folderId = folderId,
                                isSecret = isSecret,
                                isPinned = isPinned,
                                linkedNoteIds = linkedNoteIds,
                                tags = tags,
                                color = noteColor,
                                isMap = isMap,
                                mapItems = mapItems
                            )
                            showExitWarning = false
                            onBack()
                        }
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

        // Hoisted for LazyColumn use
        val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

        // Hoisted for checklist reordering; only active when isChecklist is true
        val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
            checklist = checklist.toMutableList().apply { 
                 add(to.index, removeAt(from.index)) 
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDrag > 300) { attemptBack() }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
                    )
                }
                .padding(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (noteId == null || noteId == -1) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    noteColor?.let { c ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            // Title
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                        modifier = Modifier.fillMaxWidth().alpha(if (showTitleAnimation && title.isNotBlank()) 0f else 1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                    
                    if (showTitleAnimation && title.isNotBlank()) {
                         BlurText(
                            text = title, 
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                         )
                    }
                }
            }

            // Tags
            if (tags.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { showTagInput = true },
                                label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Images
            if (images.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images) { imageUri ->
                            Box {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable { onImageClick(imageUri) },
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { images = images - imageUri },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Content / Checklist / Preview
            if (isChecklist) {
                items(checklist, key = { it.id }) { item ->
                    ReorderableItem(reorderState, key = item.id) { isDragging ->
                        val elevation = if (isDragging) 8.dp else 0.dp
                        Surface(
                            shadowElevation = elevation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { isChecked ->
                                        checklist = checklist.map { 
                                            if (it.id == item.id) it.copy(isChecked = isChecked) else it 
                                        }
                                    }
                                )
                                TextField(
                                    value = item.text,
                                    onValueChange = { newText ->
                                        checklist = checklist.map { 
                                            if (it.id == item.id) it.copy(text = newText) else it 
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    textStyle = if (item.isChecked) {
                                        MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    }
                                )
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle()
                                ) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    checklist = checklist.filter { it.id != item.id }
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Item")
                                }
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = {
                        checklist = checklist + ChecklistItem(text = "")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item")
                    }
                }
            } else if (showMarkdownPreview) {
                item {
                    val rendered = remember(content) { MarkdownRenderer.render(content) }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)
                    ) {
                        ClickableText(
                            text = rendered,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { offset ->
                                rendered.getStringAnnotations("wiki_link", offset, offset).firstOrNull()?.let { annotation ->
                                    scope.launch {
                                        val linkedNote = viewModel.getNoteByTitle(annotation.item)
                                        if (linkedNote != null) onNavigateToNote?.invoke(linkedNote.id)
                                        else Toast.makeText(context, "Note \"${annotation.item}\" not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = { Text("Add yo content here -Folius") },
                            modifier = Modifier.fillMaxWidth().alpha(if (showContentAnimation && content.isNotBlank()) 0f else 1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                         if (showContentAnimation && content.isNotBlank()) {
                              BlurText(
                                 text = content, 
                                 style = MaterialTheme.typography.bodyLarge,
                                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                 delay = 20
                              )
                         }
                    }
                }
            }

            // Backlinks
            if (backlinks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Linked to this note:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backlinks.forEach { note ->
                            AssistChip(
                                onClick = { onNavigateToNote?.invoke(note.id) },
                                label = { Text(if (note.title.isNotBlank()) note.title else "Untitled") },
                                leadingIcon = { 
                                    Icon(
                                        Icons.Default.Link, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp)
                                    ) 
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }


        // â”€â”€â”€ AI Summary Sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (showAiChat) {
            ModalBottomSheet(
                onDismissRequest = { showAiChat = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "AI Summary",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isSummarizing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = aiResponse,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (!isSummarizing && aiResponse.isNotEmpty() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAiChat = false }) {
                                Text("Dismiss")
                            }
                            Button(
                                onClick = {
                                    content += "\n\n--- AI Summary ---\n$aiResponse"
                                    showAiChat = false
                                }
                            ) {
                                Text("Add to Note")
                            }
                        }
                    } else if (aiResponse.startsWith("Error")) {
                        Button(
                            onClick = { showAiChat = false },
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}


