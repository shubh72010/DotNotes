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
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.folius.dotnotes.data.ChecklistItem
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.ui.components.*
import kotlinx.coroutines.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Int?,
    initialTab: String = "NOTES",
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onNavigateToNote: ((Int) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue("")) }
    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isChecklist by remember { mutableStateOf(initialTab == "CHECKLISTS") }
    val checklist = remember { mutableStateListOf<ChecklistItem>() }
    var isSecret by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var currentNoteId by remember { mutableStateOf(noteId) }
    var linkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var noteColor by remember { mutableStateOf<Int?>(null) }
    var isMap by remember { mutableStateOf(initialTab == "MAPS") }
    var mapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    
    var isSummarizing by remember { mutableStateOf(false) }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var showAiChat by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf("") }
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var showTagInput by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var isTitleExpanded by remember { mutableStateOf(false) }
    
    val animationsEnabled by viewModel.isAnimationsEnabled.collectAsState()
    
    var showTitleAnimation by remember { mutableStateOf(animationsEnabled && title.isNotBlank()) }
    var showContentAnimation by remember { mutableStateOf(animationsEnabled && content.text.isNotBlank()) }
    
    var initialTitle by remember { mutableStateOf("") }
    var initialContent by remember { mutableStateOf("") }
    var initialImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialIsChecklist by remember { mutableStateOf(false) }
    var initialChecklist by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var initialIsSecret by remember { mutableStateOf(false) }
    var initialIsPinned by remember { mutableStateOf(false) }
    var initialIsMap by remember { mutableStateOf(false) }
    var initialMapItems by remember { mutableStateOf<List<com.folius.dotnotes.data.MapItem>>(emptyList()) }
    var initialTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var initialNoteColor by remember { mutableStateOf<Int?>(null) }
    var initialLinkedNoteIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    var isPillVisible by remember { mutableStateOf(true) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }
    val mapNotes by viewModel.mapNotes.collectAsState()
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    
    val hasShownHints by viewModel.hasShownGestureHints.collectAsState()
    var showHint by remember { mutableStateOf(false) }

    // Onboarding hints
    LaunchedEffect(hasShownHints) {
        if (!hasShownHints) {
            // Wait a bit after screen opens
            kotlinx.coroutines.delay(1000)
            showHint = true
            // Save state immediately or after a delay
            viewModel.setHasShownGestureHints(true)
        }
    }

    if (showHint) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(5000)
            showHint = false
        }
    }
    
    val context = LocalContext.current
    
    val hasUnsavedChanges = title != initialTitle || 
                            content.text != initialContent || 
                            images != initialImages || 
                            isChecklist != initialIsChecklist || 
                            checklist.toList() != initialChecklist ||
                            isSecret != initialIsSecret ||
                            isPinned != initialIsPinned ||
                            tags != initialTags ||
                            noteColor != initialNoteColor ||
                            isMap != initialIsMap ||
                            linkedNoteIds != initialLinkedNoteIds
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val keyboardVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    
    // Markdown Helper Functions
    fun insertMarkdown(prefix: String, suffix: String = "") {
        val selection = content.selection
        val text = content.text
        
        val newText = text.substring(0, selection.start) + 
                     prefix + text.substring(selection.start, selection.end) + suffix +
                     text.substring(selection.end)
        
        val newSelection = if (selection.collapsed) {
            TextRange(selection.start + prefix.length)
        } else {
            TextRange(selection.start, selection.end + prefix.length + suffix.length)
        }
        
        content = content.copy(text = newText, selection = newSelection)
    }
    
    fun insertLinePrefix(prefix: String) {
        val text = content.text
        val selection = content.selection
        
        // Find the start of the current line
        var lineStart = selection.start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }
        
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newSelection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        
        content = content.copy(text = newText, selection = newSelection)
    }

    // --- Undo/Redo State ---
    val undoStack = remember { mutableStateListOf<Pair<String, TextFieldValue>>() }
    val redoStack = remember { mutableStateListOf<Pair<String, TextFieldValue>>() }
    var lastRecordedTitle by remember { mutableStateOf(title) }
    var lastRecordedContent by remember { mutableStateOf(content) }

    fun pushToHistory() {
        if (title != lastRecordedTitle || content.text != lastRecordedContent.text) {
            undoStack.add(lastRecordedTitle to lastRecordedContent)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            lastRecordedTitle = title
            lastRecordedContent = content
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val (uTitle, uContent) = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(title to content)
            title = uTitle
            content = uContent
            lastRecordedTitle = uTitle
            lastRecordedContent = uContent
            Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val (rTitle, rContent) = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(title to content)
            title = rTitle
            content = rContent
            lastRecordedTitle = rTitle
            lastRecordedContent = rContent
            Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show()
        }
    }

    // Debounced history recording
    LaunchedEffect(title, content) {
        delay(1000)
        pushToHistory()
    }

    // â”€â”€â”€ Word Count â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val wordCount = remember(content.text, checklist, isChecklist) {
        val text = if (isChecklist) {
            checklist.joinToString(" ") { it.text }
        } else {
            content.text
        }
        if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(content.text, checklist, isChecklist) {
        if (isChecklist) {
            checklist.sumOf { it.text.length }
        } else {
            content.text.length
        }
    }

    // â”€â”€â”€ Autosave (increased debounce to 1000ms for perf) â”€â”€â”€
    LaunchedEffect(title, content.text, images, isChecklist, checklist, isSecret, isPinned, tags, noteColor, isMap, mapItems) {
        if (title.isBlank() && content.text.isBlank() && checklist.isEmpty() && images.isEmpty() && !isMap) return@LaunchedEffect
        
        delay(1000) // Debounce 1s for efficiency
        
        linkedNoteIds = resolveWikiLinks(content.text, viewModel)
        
        val newId = viewModel.saveNote(
            id = currentNoteId,
            title = title,
            content = content.text,
            images = images,
            isChecklist = isChecklist,
            checklist = checklist,
            isSecret = isSecret,
            isPinned = isPinned,
            linkedNoteIds = linkedNoteIds,
            tags = tags,
            color = noteColor,
            clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
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
                content = TextFieldValue(note.content)
                images = note.images
                isChecklist = note.isChecklist
                checklist.clear()
                checklist.addAll(note.checklist)
                isSecret = note.isSecret
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
        if (!showContentAnimation || content.text.isBlank()) return@LaunchedEffect
        val elements = content.text.split(Regex("(?<=\\s)|(?=\\s)")).filter { it.isNotEmpty() }
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
            } catch (e: Exception) {
                    android.util.Log.w("NoteEditor", "Could not persist URI permission", e)
                }
            uri.toString()
        }
        images = images + newImages
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { images = images + it.toString() }
        }
    }

    val attemptBack: () -> Unit = {
        if (hasUnsavedChanges) {
            scope.launch {
                linkedNoteIds = resolveWikiLinks(content.text, viewModel)
                viewModel.saveNote(
                    id = currentNoteId,
                    title = title,
                    content = content.text,
                    images = images,
                    isChecklist = isChecklist,
                    checklist = checklist,
                    isSecret = isSecret,
                    isPinned = isPinned,
                    linkedNoteIds = linkedNoteIds,
                    tags = tags,
                    color = noteColor,
                    clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
                    isMap = isMap,
                    mapItems = mapItems
                )
                onBack()
            }
        } else {
            onBack()
        }
    }

    PredictiveBackHandler(enabled = hasUnsavedChanges) { progress ->
        try {
            progress.collect { /* No-op */ }
            attemptBack()
        } catch (e: Exception) {
            android.util.Log.e("NoteEditor", "Predictive back failed", e)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Word count bar (Legacy, but kept at top subtly)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$wordCount words Â· $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Main Content area
                NoteContent(
                    title = title,
                    onTitleChange = { title = it },
                    content = content,
                    onContentChange = { content = it },
                    images = images,
                    onImageRemove = { images = images - it },
                    isChecklist = isChecklist,
                    onIsChecklistChange = { 
                        if (it && !isChecklist) { // Conversion text -> check
                            if (content.text.isNotBlank()) {
                                val newItems = content.text.split("\n").filter { line -> line.isNotBlank() }.map { line -> ChecklistItem(text = line.trim()) }
                                checklist.clear()
                                checklist.addAll(newItems)
                            }
                            content = TextFieldValue("")
                        } else if (!it && isChecklist) { // Conversion check -> text
                            if (checklist.isNotEmpty()) {
                                val newText = checklist.joinToString("\n") { item -> (if (item.isChecked) "- [x] " else "- [ ] ") + item.text }
                                content = TextFieldValue(newText, selection = TextRange(newText.length))
                            }
                            checklist.clear()
                        }
                        isChecklist = it 
                    },
                    checklist = checklist,
                    isMap = isMap,
                    tags = tags,
                    noteColor = noteColor,
                    onImageClick = onImageClick,
                    showMarkdownPreview = showMarkdownPreview,
                    onNavigateToNote = onNavigateToNote,
                    modifier = Modifier.weight(1f),
                    noteId = noteId,
                    showTitleAnimation = showTitleAnimation,
                    showContentAnimation = showContentAnimation,
                    backlinks = backlinks,
                    attemptBack = attemptBack,
                    showTagInput = showTagInput,
                    onShowTagInput = { showTagInput = it },
                    scope = scope,
                    context = context,
                    viewModel = viewModel,
                    padding = padding
                )
            }

            // Full-screen scrim when pill menu or search is expanded
            if (isPillMenuExpanded || isTitleExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            isPillMenuExpanded = false
                            isTitleExpanded = false
                        }
                )
            }

            // JusBrowse-style Toolbar and Tabs
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gesture Hint Tip
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHint,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Tip: Swipe the pill left/right to Undo/Redo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = { showHint = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // DotPill (Always on top of the bars)
                com.folius.dotnotes.ui.components.DotPill(
                    isExpanded = isTitleExpanded,
                    onExpandedChange = { isTitleExpanded = it },
                    isMenuExpanded = isPillMenuExpanded,
                    onMenuExpandedChange = { isPillMenuExpanded = it },

                    searchText = title,
                    onSearchTextChange = { title = it },
                    onLongClick = { 
                        scope.launch {
                            linkedNoteIds = resolveWikiLinks(content.text, viewModel)
                            viewModel.saveNote(
                                id = currentNoteId,
                                title = title,
                                content = content.text,
                                images = images,
                                isChecklist = isChecklist,
                                checklist = checklist,
                                isSecret = isSecret,
                                isPinned = isPinned,
                                linkedNoteIds = linkedNoteIds,
                                tags = tags,
                                color = noteColor,
                                clearColor = noteColor == null && currentNoteId != null && currentNoteId != -1,
                                isMap = isMap,
                                mapItems = mapItems
                            )
                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    },
                    placeholderText = if (title.isBlank()) "Untitled Note" else title,
                    onSwipeRight = { handleUndo() },
                    onSwipeLeft = { handleRedo() },
                    pillColor = noteColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surface,
                    actions = {
                        IconButton(onClick = { isTitleExpanded = !isTitleExpanded }) {
                            Icon(if (isTitleExpanded) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Title")
                        }
                    },
                    menuContent = {
                        val pillBgColor = noteColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                        val contrastColor = if (pillBgColor.luminance() > 0.5f) Color.Black else Color.White

                        Text("Note Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contrastColor, modifier = Modifier.padding(bottom = 16.dp))
                        
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val menuItems = listOf(
                                Triple(Icons.Default.Palette, "Color", { isPillMenuExpanded = false; showColorPicker = true }),
                                Triple(Icons.Default.Label, "Tags", { isPillMenuExpanded = false; showTagInput = true }),
                                Triple(if (showMarkdownPreview) Icons.Default.EditNote else Icons.Default.Preview, if (showMarkdownPreview) "Edit" else "Preview", { isPillMenuExpanded = false; showMarkdownPreview = !showMarkdownPreview }),
                                Triple(if (isSecret) Icons.Default.LockOpen else Icons.Default.Lock, if (isSecret) "Unlock" else "Lock", { isPillMenuExpanded = false; isSecret = !isSecret }),
                                Triple(if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin, if (isPinned) "Unpin" else "Pin", { isPillMenuExpanded = false; isPinned = !isPinned }),
                                Triple(Icons.Default.Share, "Share", {
                                    isPillMenuExpanded = false
                                    val shareIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n${content.text}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
                                }),
                                Triple(Icons.Default.Image, "Add Image", {
                                    isPillMenuExpanded = false
                                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }),
                                Triple(if (isChecklist) Icons.Default.ListAlt else Icons.Default.CheckBox, if (isChecklist) "Text" else "Checklist", {
                                    isPillMenuExpanded = false
                                    if (!isChecklist) { // Converting Text to Checklist
                                        if (content.text.isNotBlank()) {
                                            val newItems = content.text.split("\n").filter { it.isNotBlank() }.map { ChecklistItem(text = it.trim()) }
                                            checklist.clear()
                                            checklist.addAll(newItems)
                                        }
                                        content = TextFieldValue("")
                                    } else { // Converting Checklist to Text
                                        if (checklist.isNotEmpty()) {
                                            val newText = checklist.joinToString("\n") { (if (it.isChecked) "- [x] " else "- [ ] ") + it.text }
                                            content = TextFieldValue(newText, selection = TextRange(newText.length))
                                        }
                                        checklist.clear()
                                    }
                                    isChecklist = !isChecklist
                                }),
                                Triple(Icons.Default.Map, "Add to Map", {
                                    isPillMenuExpanded = false
                                    showMapSelectionDialog = true
                                }),
                                Triple(Icons.Default.ContentCopy, "Duplicate", {
                                    isPillMenuExpanded = false
                                    scope.launch {
                                        val note = viewModel.getNoteById(currentNoteId ?: -1)
                                        note?.let { viewModel.duplicateNote(it) }
                                        Toast.makeText(context, "Duplicated", Toast.LENGTH_SHORT).show()
                                    }
                                }),
                                Triple(Icons.Default.AutoAwesome, "AI Assist", {
                                    isPillMenuExpanded = false
                                    showAiChat = true
                                }),
                                Triple(Icons.Default.Delete, "Delete", {
                                    isPillMenuExpanded = false
                                    val idToDelete = currentNoteId ?: existingNote?.id
                                    idToDelete?.let { viewModel.trashNoteById(it) }
                                    onBack()
                                })
                            )

                            menuItems.forEach { (icon, label, onClick) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onClick() }
                                        .padding(8.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            icon, 
                                            null, 
                                            tint = contrastColor, 
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = contrastColor)
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Animated transition between EditorToolbar and PinnedNotesBar
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPillMenuExpanded && !isTitleExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Top),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    AnimatedContent(
                        targetState = keyboardVisible,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "ToolbarConversion"
                ) { isKeyboardUp ->
                    if (isKeyboardUp) {
                        EditorToolbar(
                            isVisible = true,
                            onBold = { insertMarkdown("**", "**") },
                            onItalic = { insertMarkdown("*", "*") },
                            onStrikethrough = { insertMarkdown("~~", "~~") },
                            onH1 = { insertLinePrefix("# ") },
                            onH2 = { insertLinePrefix("## ") },
                            onCode = { insertMarkdown("`", "`") },
                            onUndo = { handleUndo() },
                            onRedo = { handleRedo() },
                            onLink = { insertMarkdown("[[", "]]") },
                            onCamera = { 
                                val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            },
                            onGallery = { 
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onChecklist = { 
                                if (!isChecklist) {
                                    if (content.text.isNotBlank()) {
                                        val newItems = content.text.split("\n").filter { it.isNotBlank() }.map { ChecklistItem(text = it.trim()) }
                                        checklist.clear()
                                        checklist.addAll(newItems)
                                    }
                                    content = TextFieldValue("")
                                } else {
                                    if (checklist.isNotEmpty()) {
                                        val newText = checklist.joinToString("\n") { (if (it.isChecked) "- [x] " else "- [ ] ") + it.text }
                                        content = TextFieldValue(newText, selection = TextRange(newText.length))
                                    }
                                    checklist.clear()
                                }
                                isChecklist = !isChecklist
                            }
                        )
                    } else {
                        PinnedNotesBar(
                            pinnedNotes = pinnedNotes,
                            activeNoteId = currentNoteId,
                            onNoteClick = { note -> 
                                if (note.id != currentNoteId) {
                                    onNavigateToNote?.invoke(note.id)
                                }
                            },
                            onNoteRemove = { viewModel.toggleNotePinnedStatus(it) },
                            onNewNote = { onBack(); viewModel.onSearchQueryChange("") }
                        )
                    }
                }
            }
        }
        }
    }



        // Color Picker Dialog
        if (showColorPicker) {
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Note Color") },
                text = {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteColors.forEach { (color, name) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (noteColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.large,
                                    )
                                    .clickable {
                                        noteColor = color
                                        showColorPicker = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (color == null) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Close")
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

        // Map Selection Dialog
        if (showMapSelectionDialog) {
            AlertDialog(
                onDismissRequest = { showMapSelectionDialog = false },
                title = { Text("Add to Map") },
                text = {
                    if (mapNotes.isEmpty()) {
                        Text("No map notes found. Create a map note first.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(mapNotes) { mapNote ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                // 1. Ensure current note is saved to get ID
                                                val nid = viewModel.saveNote(
                                                    id = currentNoteId,
                                                    title = title,
                                                    content = content.text,
                                                    images = images,
                                                    isChecklist = isChecklist,
                                                    checklist = checklist,
                                                    isSecret = isSecret,
                                                    isPinned = isPinned,
                                                    linkedNoteIds = linkedNoteIds,
                                                    tags = tags,
                                                    color = noteColor,
                                                    isMap = isMap,
                                                    mapItems = mapItems
                                                )
                                                // 2. Add this note to the selected map
                                                if (nid != -1 && nid !in mapNote.linkedNoteIds) {
                                                    viewModel.saveNote(
                                                        id = mapNote.id,
                                                        title = mapNote.title,
                                                        content = mapNote.content,
                                                        linkedNoteIds = mapNote.linkedNoteIds + nid
                                                    )
                                                    Toast.makeText(context, "Added to ${mapNote.title}", Toast.LENGTH_SHORT).show()
                                                } else if (nid != -1) {
                                                    Toast.makeText(context, "Already in ${mapNote.title}", Toast.LENGTH_SHORT).show()
                                                }
                                                showMapSelectionDialog = false
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(mapNote.title)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMapSelectionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }





        // --- AI Assist Sheet ---
        if (showAiChat) {
            ModalBottomSheet(
                onDismissRequest = { showAiChat = false },
                sheetState = sheetState
            ) {
                var aiAction by remember { mutableStateOf("Summary") }
                var aiQuery by remember { mutableStateOf("") }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        val actions = listOf("Summary", "Grammar & Clarity", "Formal Tone", "Casual Tone", "Bullets to Prose", "Prose to Bullets", "Q&A")
                        actions.forEach { action ->
                            InputChip(
                                selected = aiAction == action,
                                onClick = { aiAction = action },
                                label = { Text(action) }
                            )
                        }
                    }

                    if (aiAction == "Q&A") {
                        OutlinedTextField(
                            value = aiQuery,
                            onValueChange = { aiQuery = it },
                            placeholder = { Text("Ask something about this note...") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (content.text.isNotBlank() && aiQuery.isNotBlank()) {
                                        isSummarizing = true
                                        aiResponse = "Thinking..."
                                        viewModel.processAiAction("Q&A", "Context:\n${content.text}\n\nQuestion: $aiQuery") { result ->
                                            aiResponse = result ?: "Error: No response"
                                            isSummarizing = false
                                        }
                                    }
                                }) { Icon(Icons.Default.Send, null) }
                            }
                        )
                    } else {
                        Button(
                            onClick = {
                                if (content.text.isNotBlank()) {
                                    isSummarizing = true
                                    aiResponse = "Processing..."
                                    viewModel.processAiAction(aiAction, content.text) { result ->
                                        aiResponse = result ?: "Error: No response"
                                        isSummarizing = false
                                    }
                                } else {
                                    Toast.makeText(context, "Nothing to process", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            enabled = !isSummarizing
                        ) {
                            Text("Run $aiAction")
                        }
                    }
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 100.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isSummarizing) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Text(
                                text = aiResponse,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("AI Summary", aiResponse)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating")
                        ) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy")
                        }

                        Button(
                            onClick = {
                                val insertionText = "\n\n--- AI $aiAction ---\n$aiResponse"
                                val newText = content.text + insertionText
                                content = content.copy(text = newText, selection = TextRange(newText.length))
                                showAiChat = false
                                Toast.makeText(context, "Added to note", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating") && !aiResponse.startsWith("Processing") && !aiResponse.startsWith("Thinking")
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Append")
                        }
                        
                        Button(
                            onClick = {
                                content = content.copy(text = aiResponse, selection = TextRange(aiResponse.length))
                                showAiChat = false
                                Toast.makeText(context, "Replaced note content", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSummarizing && aiResponse.isNotBlank() && !aiResponse.startsWith("Error") && !aiResponse.startsWith("Generating") && !aiResponse.startsWith("Processing") && !aiResponse.startsWith("Thinking")
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Replace")
                        }
                    }
                }
            }
        }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteContent(
    title: String,
    onTitleChange: (String) -> Unit,
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    images: List<String>,
    onImageRemove: (String) -> Unit,
    isChecklist: Boolean,
    onIsChecklistChange: (Boolean) -> Unit,
    checklist: SnapshotStateList<ChecklistItem>,
    isMap: Boolean,
    tags: List<String>,
    noteColor: Int?,
    onImageClick: (String) -> Unit,
    showMarkdownPreview: Boolean,
    onNavigateToNote: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    noteId: Int?,
    showTitleAnimation: Boolean,
    showContentAnimation: Boolean,
    backlinks: List<Note>,
    attemptBack: () -> Unit,
    showTagInput: Boolean,
    onShowTagInput: (Boolean) -> Unit,
    scope: CoroutineScope,
    context: android.content.Context,
    viewModel: NoteViewModel,
    padding: PaddingValues
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val blocks = remember(content.text, colorScheme, showMarkdownPreview) {
        if (showMarkdownPreview) {
            MarkdownRenderer.parseBlocks(
                markdown = content.text,
                linkColor = colorScheme.primary,
                codeColor = colorScheme.secondary,
                codeBackground = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        } else emptyList()
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        checklist.add(to.index, checklist.removeAt(from.index))
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
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

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
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
                            onClick = { onShowTagInput(true) },
                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

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
                                onClick = { onImageRemove(imageUri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

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
                                    val index = checklist.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        checklist[index] = item.copy(isChecked = isChecked)
                                    }
                                }
                            )
                            val strikethroughProgress by animateFloatAsState(
                                targetValue = if (item.isChecked) 1f else 0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "strikethrough"
                            )
                            val colorScheme = MaterialTheme.colorScheme
                            
                            TextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    val index = checklist.indexOfFirst { it.id == item.id }
                                    if (index != -1) {
                                        checklist[index] = item.copy(text = newText)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .drawBehind {
                                        if (strikethroughProgress > 0f) {
                                            val strokeY = this.size.height / 2f + 4.dp.toPx()
                                            drawLine(
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                start = Offset(0f, strokeY),
                                                end = Offset(this.size.width * strikethroughProgress, strokeY),
                                                strokeWidth = 2.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (item.isChecked) colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else colorScheme.onSurface
                                )
                            )
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle()
                            ) {
                                Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                checklist.removeIf { it.id == item.id }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Item")
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = {
                    checklist.add(ChecklistItem(text = ""))
                }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        } else if (showMarkdownPreview) {
            items(blocks) { block ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    MarkdownBlockView(
                        block = block,
                        onNavigateToNote = { id -> onNavigateToNote?.invoke(id) },
                        onWikiLinkClick = { title ->
                            scope.launch {
                                val linkedNote = viewModel.getNoteByTitle(title)
                                if (linkedNote != null) onNavigateToNote?.invoke(linkedNote.id)
                                else Toast.makeText(context, "Note \"$title\" not found", Toast.LENGTH_SHORT).show()
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
                        onValueChange = { newValue -> 
                            if (newValue.text.endsWith("[[") && !content.text.endsWith("[[")) {
                                Toast.makeText(context, "Wiki Link: Type note title inside [[]]", Toast.LENGTH_SHORT).show()
                            }
                            
                            // [] checklist conversion
                            if (newValue.text.startsWith("[] ") || newValue.text.startsWith("- [] ") || newValue.text.startsWith("- [ ] ")) {
                                if (content.text.isBlank() || newValue.text.length <= 6) {
                                    onIsChecklistChange(true)
                                    onContentChange(TextFieldValue(""))
                                    return@TextField
                                }
                            }
                            
                            onContentChange(newValue) 
                        },
                        placeholder = { Text("Add yo content here -Folius") },
                        modifier = Modifier.fillMaxWidth().alpha(if (showContentAnimation && content.text.isNotBlank()) 0f else 1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                     if (showContentAnimation && content.text.isNotBlank()) {
                           BlurText(
                              text = content.text, 
                              style = MaterialTheme.typography.bodyLarge,
                              modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                              delay = 20
                           )
                     }
                }
            }
        }

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
}



/*"Abracadabra, amor-oo-na-na
Abracadabra, morta-oo-ga-ga
Abracadabra, abra-oo-na-na"
In her tongue she said, "Death or love tonight"
(Oooo look at an easter egg!) */