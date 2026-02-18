package com.folius.dotnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import com.folius.dotnotes.data.Folder
import com.folius.dotnotes.data.Note
import java.text.SimpleDateFormat
import java.util.*
import com.folius.dotnotes.ui.components.BlurText
import com.folius.dotnotes.ui.components.PinnedNoteItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    
    val pagerState = rememberPagerState(
        initialPage = when (selectedTab) {
            NoteTab.NOTES -> 0
            NoteTab.CHECKLISTS -> 1
            NoteTab.SEARCH -> 2
        },
        pageCount = { 3 }
    )

    // Sync state: Pager -> ViewModel
    LaunchedEffect(pagerState.settledPage) {
        val newTab = when (pagerState.settledPage) {
            0 -> NoteTab.NOTES
            1 -> NoteTab.CHECKLISTS
            2 -> NoteTab.SEARCH
            else -> NoteTab.NOTES
        }
        if (newTab != selectedTab) {
            viewModel.setSelectedTab(newTab)
        }
    }

    // Sync state: ViewModel -> Pager
    LaunchedEffect(selectedTab) {
        val targetPage = when (selectedTab) {
            NoteTab.NOTES -> 0
            NoteTab.CHECKLISTS -> 1
            NoteTab.SEARCH -> 2
        }
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    var active by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }

    LaunchedEffect(Unit) {
        // Give UI a moment to settle before loading notes
        kotlinx.coroutines.delay(300)
        viewModel.setLoaded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (selectedTab) {
                            NoteTab.NOTES -> "My Notes"
                            NoteTab.CHECKLISTS -> "My Checklists"
                            NoteTab.SEARCH -> "Search & Folders"
                        }
                    ) 
                },
                actions = {
                    // Sort dropdown
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            order.label,
                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == NoteTab.NOTES,
                    onClick = { viewModel.setSelectedTab(NoteTab.NOTES) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Notes") }
                )
                NavigationBarItem(
                    selected = selectedTab == NoteTab.CHECKLISTS,
                    onClick = { viewModel.setSelectedTab(NoteTab.CHECKLISTS) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Lists") },
                    label = { Text("Lists") }
                )
                NavigationBarItem(
                    selected = selectedTab == NoteTab.SEARCH,
                    onClick = { viewModel.setSelectedTab(NoteTab.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search & Folders") },
                    label = { Text("Search") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                val pageTab = when (pageIndex) {
                    0 -> NoteTab.NOTES
                    1 -> NoteTab.CHECKLISTS
                    2 -> NoteTab.SEARCH
                    else -> NoteTab.NOTES
                }

                if (pageTab == NoteTab.SEARCH) {
                    SearchAndFoldersTab(
                        viewModel = viewModel,
                        folders = folders,
                        selectedFolderId = selectedFolderId,
                        searchQuery = searchQuery,
                        notes = notes, 
                        isLoading = isLoading,
                        onCreateFolder = { showCreateFolderDialog = true },
                        onRenameFolder = { 
                            folderToRename = it
                            showRenameFolderDialog = true 
                        },
                        onDeleteFolder = {
                            folderToDelete = it
                            showDeleteFolderDialog = true
                        },
                        onNoteClick = onNoteClick
                    )
                } else {
                    NoteListContent(
                        notes = notes,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        onNoteClick = onNoteClick
                    )
                }
            }

            if (pinnedNotes.isNotEmpty() && selectedTab != NoteTab.SEARCH) {
                PinnedNotesSection(
                    notes = pinnedNotes,
                    onNoteClick = onNoteClick,
                    onUnpinClick = { viewModel.toggleNotePinnedStatus(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.addFolder(newFolderName)
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteFolderDialog && folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Folder") },
            text = { Text("Are you sure you want to delete '${folderToDelete?.name}'? Notes in this folder will remain but will be unassigned.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderToDelete?.let { viewModel.deleteFolder(it) }
                        showDeleteFolderDialog = false
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameFolderDialog && folderToRename != null) {
        var renamedFolderName by remember { mutableStateOf(folderToRename?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renamedFolderName,
                    onValueChange = { renamedFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renamedFolderName.isNotBlank()) {
                            folderToRename?.let { viewModel.renameFolder(it, renamedFolderName) }
                            showRenameFolderDialog = false
                            folderToRename = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit, onLongPress: () -> Unit) {
    val date = remember(note.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.timestamp))
    }
    val noteColorValue = note.color
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row {
            // Color indicator
            if (noteColorValue != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .background(Color(noteColorValue.toLong() or 0xFF000000L))
                )
            }
        Column(
            modifier = Modifier.padding(16.dp).weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.title.isNotBlank()) {
                    BlurText(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        delay = 50
                    )
                } else {
                     BlurText(
                        text = "Untitled",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        delay = 50
                    )
                }
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (note.isMap) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Map Note",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            // Tags
            if (note.tags.isNotEmpty()) {
                Text(
                    text = note.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (note.isChecklist) {
                val previewItems = note.checklist.take(3)
                Column {
                    previewItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (note.checklist.size > 3) {
                        Text("...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val truncatedContent = if (note.content.length > 100) {
                    note.content.take(100) + "..."
                } else {
                    note.content
                }
                BlurText(
                    text = truncatedContent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    delay = 20
                )
            }
        }
        } // end Row
    }
}

@Composable
fun NoteListContent(
    notes: List<Note>,
    isLoading: Boolean,
    searchQuery: String,
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(10) { NoteItemSkeleton() }
            }
        } else if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isEmpty()) "No notes here." else "No results found.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = notes,
                    key = { it.id },
                    contentType = { "note" }
                ) { note ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    viewModel.trashNote(note)
                                    true
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    viewModel.toggleNoteSecretStatus(note)
                                    true
                                }
                                else -> false
                            }
                        },
                        positionalThreshold = { distance -> distance * 0.75f }
                    )

                    // Haptic Detent: Trigger feel immediately when threshold is crossed
                    LaunchedEffect(dismissState.targetValue) {
                        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val isDismissed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                            
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }
                            val icon = when (direction) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Lock
                                else -> null
                            }

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = alignment
                            ) {
                                if (icon != null) {
                                    val scale by animateFloatAsState(
                                        targetValue = if (isDismissed) 1.3f else 1.0f
                                    )
                                    val alpha by animateFloatAsState(
                                        targetValue = if (isDismissed) 1.0f else 0.5f
                                    )
                                    Icon(
                                        icon, 
                                        contentDescription = null,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        NoteItem(
                            note = note, 
                            onClick = { onNoteClick(note.id) },
                            onLongPress = { viewModel.toggleNotePinnedStatus(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItemSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
fun SearchAndFoldersTab(
    viewModel: NoteViewModel,
    folders: List<com.folius.dotnotes.data.Folder>,
    selectedFolderId: Int?,
    searchQuery: String,
    notes: List<Note>,
    isLoading: Boolean,
    onCreateFolder: () -> Unit,
    onRenameFolder: (com.folius.dotnotes.data.Folder) -> Unit,
    onDeleteFolder: (com.folius.dotnotes.data.Folder) -> Unit,
    onNoteClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        NoteListContent(
            notes = notes,
            isLoading = isLoading,
            searchQuery = searchQuery,
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            modifier = Modifier.weight(1f)
        )

        Text("Folders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { viewModel.setSelectedFolder(null) },
                    label = { Text("All") }
                )
            }
            
            items(folders) { folder ->
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { viewModel.setSelectedFolder(folder.id) },
                    label = { Text(folder.name) },
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onRenameFolder(folder) }
                        )
                    },
                    trailingIcon = {
                        if (selectedFolderId == folder.id) {
                            IconButton(
                                onClick = { onDeleteFolder(folder) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Folder",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }
            
            item {
                FilterChip(
                    selected = false,
                    onClick = onCreateFolder,
                    label = { Icon(Icons.Default.Add, contentDescription = "New Folder", modifier = Modifier.size(16.dp)) }
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Search notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Tag filter chips
        val allTags by viewModel.allTags.collectAsState()
        val selectedTag by viewModel.selectedTag.collectAsState()
        if (allTags.isNotEmpty()) {
            Text("Tags", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { viewModel.setSelectedTag(null) },
                        label = { Text("All") },
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
                items(allTags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.setSelectedTag(if (selectedTag == tag) null else tag) },
                        label = { Text("#$tag") }
                    )
                }
            }
        }
        
        Text(
            text = "Active Folder Filters apply to the Notes and Lists tabs.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun PinnedNotesSection(
    notes: List<Note>,
    onNoteClick: (Int) -> Unit,
    onUnpinClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Pinned",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(notes) { note ->
                PinnedNoteItem(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onLongClick = { onUnpinClick(note) },
                    modifier = Modifier.size(85.dp)
                )
            }
        }
    }
}
