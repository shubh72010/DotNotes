package com.folius.dotnotes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.folius.dotnotes.data.Note

/**
 * PinnedNotesBar — shown at the bottom of NoteListScreen.
 *
 * Mirrors JusBrowse's BottomTabBar pattern:
 *  - Horizontally scrollable pinned note chips
 *  - Active chip (last opened) gets primary gradient highlight
 *  - × dismisses from bar (keeps note pinned, just removes from quick-access)
 *  - + creates a new note
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinnedNotesBar(
    pinnedNotes: List<Note>,
    activeNoteId: Int?,
    onNoteClick: (Note) -> Unit,
    onNoteRemove: (Note) -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pinnedNotes.isEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            NewNoteChip(onClick = onNewNote)
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pinnedNotes.forEach { note ->
                PinnedNoteChip(
                    note = note,
                    isActive = note.id == activeNoteId,
                    onClick = { onNoteClick(note) },
                    onClose = { onNoteRemove(note) }
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        NewNoteChip(onClick = onNewNote)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedNoteChip(
    note: Note,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    AnimatedContent(
        targetState = isActive,
        transitionSpec = {
            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                    scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                .togetherWith(
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                            scaleOut(targetScale = 0.92f)
                )
        },
        label = "pinnedChip_${note.id}"
    ) { active ->

        val chipBackground = if (active)
            Brush.linearGradient(listOf(primary.copy(alpha = 0.85f), primary.copy(alpha = 0.7f)))
        else
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.05f)))

        val borderColor = if (active) primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
        val borderWidth = if (active) 1.5.dp else 1.dp

        Row(
            modifier = modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(chipBackground)
                .border(borderWidth, borderColor, CircleShape)
                .combinedClickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Color.White.copy(alpha = 0.9f)
                        else primary.copy(alpha = 0.6f)
                    )
            )

            note.color?.let { colorInt ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                )
            }

            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.widthIn(max = 100.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from pinned bar",
                    modifier = Modifier.size(10.dp),
                    tint = if (active) primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun NewNoteChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(34.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Note",
            modifier = Modifier.size(16.dp)
        )
    }
}
