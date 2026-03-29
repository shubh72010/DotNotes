package com.folius.dotnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import com.folius.dotnotes.data.Note
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.core.exponentialDecay
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.*
import com.folius.dotnotes.ui.components.BlurText
import com.folius.dotnotes.ui.components.PinnedSubpill
import com.folius.dotnotes.ui.components.DotPill
import com.folius.dotnotes.ui.components.PinnedNotesBar
import com.folius.dotnotes.ui.components.TabSwitcherPill
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class DragAnchors {
    Settled,
    RevealedStart,
    DismissedStart,
    RevealedEnd,
    DismissedEnd,
    StartToEnd,
    EndToStart
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddNoteClick: (NoteTab) -> Unit,
    onSettingsClick: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val mapNotes by viewModel.mapNotes.collectAsState()
    val selectedMapId by viewModel.selectedMapId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    
    val scope = rememberCoroutineScope()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isPillMenuExpanded by remember { mutableStateOf(false) }
    var showMapSelectionDialog by remember { mutableStateOf(false) }
    var noteToAddToMap by remember { mutableStateOf<Note?>(null) }

    val hasShownHints by viewModel.hasShownGestureHints.collectAsState()
    var showListHint by remember { mutableStateOf(false) }

    LaunchedEffect(hasShownHints) {
        if (!hasShownHints) {
            delay(2000)
            showListHint = true
        }
    }

    if (showListHint) {
        LaunchedEffect(Unit) {
            delay(6000)
            showListHint = false
        }
    }

    LaunchedEffect(Unit) {
        // Give UI a moment to settle before loading notes
        kotlinx.coroutines.delay(300)
        viewModel.setLoaded()
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Large Header
                Text(
                    text = "DotNotes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (selectedTab == NoteTab.SEARCH) {
                    SearchAndMapsTab(
                        viewModel = viewModel,
                        mapNotes = mapNotes,
                        selectedMapId = selectedMapId,
                        searchQuery = searchQuery,
                        notes = notes,
                        isLoading = isLoading,
                        onNoteClick = onNoteClick,
                        onMapNoteClick = onMapNoteClick,
                        onAddToMap = { note ->
                            noteToAddToMap = note
                            showMapSelectionDialog = true
                        }
                    )
                } else {
                    NoteListContent(
                        notes = notes,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        viewModel = viewModel,
                        onNoteClick = onNoteClick,
                        onMapNoteClick = onMapNoteClick,
                        onAddToMap = { noteToAddToMap = it },
                        modifier = Modifier.weight(1f),
                        showHint = showListHint,
                        onDismissHint = { showListHint = false }
                    )
                }
            }

            // Full-screen scrim when pill menu or search is expanded
            if (isPillMenuExpanded || isSearchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            isPillMenuExpanded = false
                            isSearchExpanded = false
                        }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Added a subtle gradient background for better visibility
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 0.dp, top = 8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DotPill Integration (Top)
                com.folius.dotnotes.ui.components.DotPill(
                    isExpanded = isSearchExpanded,
                    onExpandedChange = { isSearchExpanded = it },
                    isMenuExpanded = isPillMenuExpanded,
                    onMenuExpandedChange = { isPillMenuExpanded = it },
                    searchText = searchQuery,
                    onSearchTextChange = { viewModel.onSearchQueryChange(it) },
                    onLongClick = { 
                        if (selectedTab == NoteTab.MAPS) {
                            viewModel.createMapNote("New Map")
                        } else {
                            onAddNoteClick(selectedTab) 
                        }
                    },
                    placeholderText = if (selectedMapId != null) {
                        mapNotes.find { it.id == selectedMapId }?.title ?: "Search notes..."
                    } else "Search notes...",
                    actions = {
                        IconButton(onClick = { isSearchExpanded = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    menuContent = {
                        Text(
                            "Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(Icons.Default.Sort, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sort: ${sortOrder.label}")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(order.label) },
                                            onClick = {
                                                viewModel.setSortOrder(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }


                        if (allTags.isNotEmpty()) {
                            Text(
                                "Tags",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 8.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedTag == null,
                                        onClick = { viewModel.setSelectedTag(null) },
                                        label = { Text("All Tags") }
                                    )
                                }
                                items(allTags) { tag ->
                                    FilterChip(
                                        selected = selectedTag == tag,
                                        onClick = { viewModel.setSelectedTag(tag) },
                                        label = { Text("#$tag") }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onSettingsClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Settings, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("App Settings")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Tab Switcher Pill (Notes vs Lists) - Now in the middle
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPillMenuExpanded && !isSearchExpanded,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    TabSwitcherPill(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.setSelectedTab(it) }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Pinned Notes Bar (Bottom) - Hidden when menu is open
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isPillMenuExpanded && !isSearchExpanded,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Top),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    PinnedNotesBar(
                        pinnedNotes = pinnedNotes,
                        activeNoteId = null,
                        onNoteClick = { note -> 
                            if (note.isMap) onMapNoteClick(note.id)
                            else onNoteClick(note.id) 
                        },
                        onNoteRemove = { viewModel.toggleNotePinnedStatus(it) },
                        onNewNote = { 
                            if (selectedTab == NoteTab.MAPS) {
                                viewModel.createMapNote("New Map")
                            } else {
                                onAddNoteClick(selectedTab) 
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
        }
    }
}

    if (showMapSelectionDialog && noteToAddToMap != null) {
        MapNoteSelectionDialog(
            mapNotes = mapNotes,
            onMapSelected = { mapNote ->
                if (noteToAddToMap!!.id !in mapNote.linkedNoteIds) {
                    scope.launch {
                        viewModel.saveNote(
                            id = mapNote.id,
                            title = mapNote.title,
                            content = mapNote.content,
                            linkedNoteIds = mapNote.linkedNoteIds + noteToAddToMap!!.id
                        )
                    }
                }
                showMapSelectionDialog = false
                noteToAddToMap = null
            },
            onDismiss = {
                showMapSelectionDialog = false
                noteToAddToMap = null
            }
        )
    }

    // Folder dialogs removed.
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
                        .clip(MaterialTheme.shapes.small)
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
fun MapNoteSelectionDialog(
    mapNotes: List<Note>,
    onMapSelected: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                                .clickable { onMapSelected(mapNote) }
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NoteListContent(
    notes: List<Note>,
    isLoading: Boolean,
    searchQuery: String,
    viewModel: NoteViewModel,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddToMap: (Note) -> Unit,
    modifier: Modifier = Modifier,
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                items(
                    items = notes,
                    key = { it.id },
                    contentType = { "note" }
                ) { note ->
                    val density = LocalDensity.current
                    val midwayThreshold = with(density) { 120.dp.toPx() }
                    val dismissThreshold = with(density) { 250.dp.toPx() } // Lowered threshold for full action
                    
                    val decaySpec = exponentialDecay<Float>()
                    val state = remember {
                        AnchoredDraggableState<DragAnchors>(
                            initialValue = DragAnchors.Settled,
                            positionalThreshold = { distance: Float -> distance * 0.5f },
                            velocityThreshold = { with(density) { 100.dp.toPx() } },
                            snapAnimationSpec = tween(),
                            decayAnimationSpec = decaySpec
                        )
                    }

                    SideEffect {
                        state.updateAnchors(
                            DraggableAnchors {
                                DragAnchors.Settled at 0f
                                // Left swipe (EndToStart)
                                DragAnchors.RevealedEnd at -midwayThreshold
                                DragAnchors.DismissedEnd at -dismissThreshold
                                // Right swipe (StartToEnd)
                                DragAnchors.RevealedStart at midwayThreshold
                                DragAnchors.DismissedStart at dismissThreshold
                            }
                        )
                    }

                    // Haptic Detent: Trigger feel when snapping to midway or dismissal
                    LaunchedEffect(state.currentValue) {
                        if (state.currentValue != DragAnchors.Settled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    // Trigger actions when dismissed
                    LaunchedEffect(state.currentValue) {
                        when (state.currentValue) {
                            DragAnchors.DismissedStart -> {
                                viewModel.trashNote(note)
                                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                state.snapTo(DragAnchors.Settled)
                            }
                            DragAnchors.DismissedEnd -> {
                                viewModel.toggleNoteSecretStatus(note)
                                Toast.makeText(context, if (note.isSecret) "Note unlocked" else "Note locked", Toast.LENGTH_SHORT).show()
                                state.snapTo(DragAnchors.Settled)
                            }
                            else -> {}
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .anchoredDraggable(state, Orientation.Horizontal)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        // Background (Actions)
                        val offset = if (state.anchors.size > 0) state.requireOffset() else 0f
                        val direction = if (offset > 0) DragAnchors.StartToEnd else DragAnchors.EndToStart
                        
                        val isRevealed = (abs(offset) >= midwayThreshold * 0.9f)
                        val isDismissing = (abs(offset) >= (midwayThreshold + dismissThreshold) / 2f)

                        val backgroundColor by animateColorAsState(
                            targetValue = when {
                                isDismissing && direction == DragAnchors.StartToEnd -> MaterialTheme.colorScheme.error
                                isDismissing && direction == DragAnchors.EndToStart -> MaterialTheme.colorScheme.tertiary
                                direction == DragAnchors.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isRevealed) 1f else 0.4f)
                                direction == DragAnchors.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isRevealed) 1f else 0.4f)
                                else -> Color.Transparent
                            }
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(backgroundColor)
                                .clickable {
                                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                    if (state.currentValue == DragAnchors.RevealedEnd) {
                                        viewModel.toggleNoteSecretStatus(note)
                                        Toast.makeText(context, if (note.isSecret) "Note unlocked" else "Note locked", Toast.LENGTH_SHORT).show()
                                        scope.launch { state.snapTo(DragAnchors.Settled) }
                                    } else if (state.currentValue == DragAnchors.RevealedStart) {
                                        viewModel.trashNote(note)
                                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                        scope.launch { state.snapTo(DragAnchors.Settled) }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = if (offset > 0) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when {
                                        offset > 0 -> Icons.Default.Delete
                                        else -> if (note.isSecret) Icons.Default.LockOpen else Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    tint = if (isDismissing) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = if (isDismissing) 1.3f else 1.0f
                                        scaleY = if (isDismissing) 1.3f else 1.0f
                                    }
                                )
                                if (isRevealed) {
                                    Text(
                                        text = if (isDismissing) "Release to Confirm" else "Action Ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDismissing) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Content (Note Item)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offset.roundToInt(), 0) }
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            NoteItem(
                                note = note, 
                                onClick = { 
                                    if (note.isMap) onMapNoteClick(note.id)
                                    else onNoteClick(note.id)
                                },
                                onLongPress = { 
                                    onAddToMap(note)
                                }
                            )
                        }
                    }
                }
            }
            
            // Gesture Hint Over Note List
            androidx.compose.animation.AnimatedVisibility(
                visible = showHint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.TouchApp, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Try swiping notes to Delete or Lock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(onClick = onDismissHint, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                        }
                    }
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
fun SearchAndMapsTab(
    viewModel: NoteViewModel,
    mapNotes: List<Note>,
    selectedMapId: Int?,
    searchQuery: String,
    notes: List<Note>,
    isLoading: Boolean,
    onNoteClick: (Int) -> Unit,
    onMapNoteClick: (Int) -> Unit,
    onAddToMap: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        NoteListContent(
            notes = notes,
            isLoading = isLoading,
            searchQuery = searchQuery,
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            onMapNoteClick = onMapNoteClick,
            onAddToMap = onAddToMap,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Map Views", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedMapId == null,
                    onClick = { viewModel.setSelectedMap(null) },
                    label = { Text("All") }
                )
            }
            
            items(mapNotes, key = { it.id }) { note ->
                FilterChip(
                    selected = selectedMapId == note.id,
                    onClick = { viewModel.setSelectedMap(note.id) },
                    label = { Text(note.title) }
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
                items(allTags, key = { it }) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.setSelectedTag(if (selectedTag == tag) null else tag) },
                        label = { Text("#$tag") }
                    )
                }
            }
        }
        
        Text(
            text = "Notes are filtered by their links to the selected Map Note.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
