package com.folius.dotnotes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folius.dotnotes.ui.NoteTab

@Composable
fun TabSwitcherPill(
    selectedTab: NoteTab,
    onTabSelected: (NoteTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .padding(4.dp)
            .height(44.dp)
            .width(240.dp),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
                .background(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .pointerInput(selectedTab) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDrag += dragAmount
                                if (totalDrag > 80f) {
                                    val next = when (selectedTab) { 
                                        NoteTab.NOTES -> NoteTab.CHECKLISTS 
                                        NoteTab.CHECKLISTS -> NoteTab.MAPS 
                                        else -> NoteTab.MAPS 
                                    }
                                    if (next != selectedTab) onTabSelected(next)
                                    totalDrag = 0f
                                } else if (totalDrag < -80f) {
                                    val prev = when (selectedTab) { 
                                        NoteTab.MAPS -> NoteTab.CHECKLISTS 
                                        NoteTab.CHECKLISTS -> NoteTab.NOTES 
                                        else -> NoteTab.NOTES 
                                    }
                                    if (prev != selectedTab) onTabSelected(prev)
                                    totalDrag = 0f
                                }
                            }
                        )
                    }
            ) {
                val tabWidth = maxWidth / 3f
                val offset by animateDpAsState(
                    targetValue = when (selectedTab) {
                        NoteTab.NOTES -> 0.dp
                        NoteTab.CHECKLISTS -> tabWidth
                        NoteTab.MAPS -> tabWidth * 2
                        else -> 0.dp
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabIndicator"
                )

                // The sliding indicator
                Box(
                    modifier = Modifier
                        .offset(x = offset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.3f))
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItem(
                        selected = selectedTab == NoteTab.NOTES,
                        icon = Icons.Default.Description,
                        label = "Notes",
                        onClick = { onTabSelected(NoteTab.NOTES) },
                        modifier = Modifier.weight(1f)
                    )
                    TabItem(
                        selected = selectedTab == NoteTab.CHECKLISTS,
                        icon = Icons.Default.List,
                        label = "Lists",
                        onClick = { onTabSelected(NoteTab.CHECKLISTS) },
                        modifier = Modifier.weight(1f)
                    )
                    TabItem(
                        selected = selectedTab == NoteTab.MAPS,
                        icon = androidx.compose.material.icons.Icons.Default.Map,
                        label = "Maps",
                        onClick = { onTabSelected(NoteTab.MAPS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = spring(),
        label = "tabBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        animationSpec = spring(),
        label = "tabContent"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                ),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
    }
}
