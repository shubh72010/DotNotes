package com.folius.dotnotes.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.folius.dotnotes.data.MapItem
import com.folius.dotnotes.data.Note
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteMapScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    mapItems: List<com.folius.dotnotes.data.MapItem>,
    onMapItemsChange: (List<com.folius.dotnotes.data.MapItem>) -> Unit,
    onNavigateToNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    val allNotes by viewModel.allNonDeletedNotes.collectAsState(initial = emptyList())
    // Find the current "Map Note"
    val mapNote = allNotes.find { it.id == noteId }

    if (mapNote == null) {
        // Handle loading or error
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Local state for map items to allow smooth dragging before save
    var localMapItems by remember { mutableStateOf(mapItems) }
    
    // Sync local state when external items change (added/removed)
    LaunchedEffect(mapItems) {
        val localIds = localMapItems.map { it.id }.toSet()
        val externalIds = mapItems.map { it.id }.toSet()
        if (localIds != externalIds) {
            localMapItems = mapItems
        }
    }
    
    // Dialog state
    var showAddNoteDialog by remember { mutableStateOf(false) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offset += panChange
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(mapNote.title.ifBlank { "Untitled Map" })
                        Text("Map View", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Could add logic to rename map note here
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNoteDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note to Map")
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            // Grid Background
            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSize = 40.dp.toPx()
                val scaledGrid = gridSize * scale
                
                val startX = (offset.x % scaledGrid)
                val startY = (offset.y % scaledGrid)
                
                var x = startX - scaledGrid
                while (x < size.width + scaledGrid) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += scaledGrid
                }
                
                var y = startY - scaledGrid
                while (y < size.height + scaledGrid) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += scaledGrid
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(transformableState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                // Render Connections (Lines)
                val density = LocalDensity.current
                val connectionColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                
                val cardWidthPx = remember(density) { with(density) { 200.dp.toPx() } }
                val cardHeightPx = remember(density) { with(density) { 100.dp.toPx() } }
                
                val connections = remember(localMapItems, allNotes, cardWidthPx, cardHeightPx) {
                    val list = mutableListOf<Pair<Offset, Offset>>()
                    
                    localMapItems.forEach { sourceItem ->
                        val sourceNote = allNotes.find { it.id == sourceItem.noteId }
                        sourceNote?.linkedNoteIds?.forEach { targetNoteId ->
                            val targetItem = localMapItems.find { it.noteId == targetNoteId }
                            if (targetItem != null) {
                                val start = Offset(sourceItem.x + cardWidthPx / 2, sourceItem.y + cardHeightPx / 2)
                                val end = Offset(targetItem.x + cardWidthPx / 2, targetItem.y + cardHeightPx / 2)
                                list.add(start to end)
                            }
                        }
                    }
                    list
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    connections.forEach { (start, end) ->
                        drawLine(
                            color = connectionColor,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Render Map Items
                localMapItems.forEach { item ->
                    val linkedNote = allNotes.find { it.id == item.noteId }
                    if (linkedNote != null) {
                        MapNoteCard(
                            item = item,
                            note = linkedNote,
                            scale = scale,
                            onPositionCommitted = { newX, newY ->
                                // Update local state and parent state together on commit
                                val updated = localMapItems.map { 
                                    if (it.id == item.id) it.copy(x = newX, y = newY) else it 
                                }
                                localMapItems = updated
                                onMapItemsChange(updated)
                            },
                            onClick = { onNavigateToNote(linkedNote.id) }
                        )
                    }
                }
            }
            
            if (localMapItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap + to add notes to this map",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            
            if (showAddNoteDialog) {
                AddNoteToMapDialog(
                    allNotes = allNotes.filter { note -> 
                        !note.isMap && !note.isDeleted && 
                        note.id != noteId &&
                        localMapItems.none { item -> item.noteId == note.id }
                    },
                    onDismiss = { showAddNoteDialog = false },
                    onNoteSelected = { selectedNote ->
                        // Add to center of screen (relative to offset/scale)
                        val centerX = (canvasWidth / 2 - offset.x) / scale
                        val centerY = (canvasHeight / 2 - offset.y) / scale
                        
                        val newItem = MapItem(
                            noteId = selectedNote.id,
                            x = centerX, 
                            y = centerY
                        )
                        val newItems = localMapItems + newItem
                        localMapItems = newItems
                        onMapItemsChange(newItems)
                        showAddNoteDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun MapNoteCard(
    item: MapItem,
    note: Note,
    scale: Float,
    onPositionCommitted: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var posX by remember { mutableFloatStateOf(item.x) }
    var posY by remember { mutableFloatStateOf(item.y) }
    val updatedScale = rememberUpdatedState(scale)
    
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "dragScale"
    )
    
    Card(
        modifier = Modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
            .width(200.dp)
            .heightIn(min = 100.dp)
            .pointerInput(item.id, "tap") {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(item.id, "drag") {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { 
                        isDragging = false
                        onPositionCommitted(posX, posY)
                    },
                    onDragCancel = { 
                        isDragging = false
                        onPositionCommitted(posX, posY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    posX += dragAmount.x / updatedScale.value
                    posY += dragAmount.y / updatedScale.value
                }
            }
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AddNoteToMapDialog(
    allNotes: List<Note>,
    onDismiss: () -> Unit,
    onNoteSelected: (Note) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Add Note to Map",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allNotes) { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteSelected(note) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(note.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { "Untitled" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
