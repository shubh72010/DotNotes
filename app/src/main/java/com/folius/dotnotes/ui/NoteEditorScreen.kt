package com.folius.dotnotes.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.folius.dotnotes.ui.components.BlurText
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.folius.dotnotes.data.ChecklistItem
import com.folius.dotnotes.data.Note
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
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
    
    var isSummarizing by remember { mutableStateOf(false) }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var showAiChat by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }
    
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
                            isPinned != initialIsPinned
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Autosave
    LaunchedEffect(title, content, images, isChecklist, checklist, folderId, isSecret) {
        if (title.isBlank() && content.isBlank() && checklist.isEmpty() && images.isEmpty()) return@LaunchedEffect
        
        kotlinx.coroutines.delay(300) // Debounce 300ms for "instant" feel but efficiency
        
        val newId = viewModel.saveNote(
            id = currentNoteId,
            title = title,
            content = content,
            images = images,
            isChecklist = isChecklist,
            checklist = checklist,
            folderId = folderId,
            isSecret = isSecret,
            isPinned = isPinned
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
                isSecret = note.isSecret
                
                initialTitle = note.title
                initialContent = note.content
                initialImages = note.images
                initialIsChecklist = note.isChecklist
                initialChecklist = note.checklist
                initialFolderId = note.folderId
                initialIsSecret = note.isSecret
                initialIsPinned = note.isPinned
                isPinned = note.isPinned

                showTitleAnimation = animationsEnabled && note.title.isNotBlank()
                showContentAnimation = animationsEnabled && note.content.isNotBlank()
            }
        } else {
             // New note, default to currently selected folder in list screen if any
             folderId = viewModel.selectedFolderId.value
             initialFolderId = viewModel.selectedFolderId.value
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
            } catch (e: Exception) {
                // Ignore if not persistable
            }
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
            progress.collect { backEvent -> }
            showExitWarning = true
        } catch (e: Exception) {
            // Cancelled
        }
    }

    // Default BackHandler when no unsaved changes or warning shown (it will just go back)
    // Actually PredictiveBackHandler above handles it when changes exist.
    // If no changes, the default back behavior (onBack) should work.
    // But we need to ensure the warning dialog can actually exit.

    Scaffold(
        bottomBar = {
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

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = {
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
                                isPinned = isPinned
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
                                            val summary = viewModel.summarizeNote(content)
                                            aiResponse = summary ?: "Failed to generate summary."
                                            isSummarizing = false
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
                                        idToDelete?.let { viewModel.deleteNoteById(it) }
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

        val mainScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDrag > 300) { // Threshold for swipe
                                attemptBack()
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        }
                    )
                }
                .verticalScroll(mainScrollState)
                .padding(16.dp)
        ) {
            Text(
                text = if (noteId == null || noteId == -1) "New Note" else "Edit Note",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Header Animation State
            LaunchedEffect(title) {
                if (title.isBlank()) return@LaunchedEffect
                val elements = title.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
                val animationTime = (elements.size * 200L) + 1000L
                kotlinx.coroutines.delay(animationTime)
                showTitleAnimation = false
            }

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
            
            if (images.isNotEmpty()) {
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
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Remove", 
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isChecklist) {
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val state = rememberReorderableLazyListState(lazyListState) { from, to ->
                    checklist = checklist.toMutableList().apply { 
                         add(to.index, removeAt(from.index)) 
                    }
                }
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(checklist, key = { it.id }) { item ->
                        ReorderableItem(state, key = item.id) { isDragging ->
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
                                    
                                    // Drag Handle
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
                }
            } else {
                // Content Animation State
                LaunchedEffect(content) {
                    if (content.isBlank()) return@LaunchedEffect
                    val elements = content.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
                    val animationTime = (elements.size * 200L) + 1000L
                    kotlinx.coroutines.delay(animationTime)
                    showContentAnimation = false
                }

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (showContentAnimation && content.isNotBlank()) 0f else 1f),
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
