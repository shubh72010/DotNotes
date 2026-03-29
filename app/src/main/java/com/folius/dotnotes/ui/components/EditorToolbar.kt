package com.folius.dotnotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EditorToolbar — shown at the bottom of NoteEditorScreen.
 *
 * Appears when keyboard is open, slides away when keyboard dismisses.
 * Sits between the pill and the keyboard.
 */
@Composable
fun EditorToolbar(
    isVisible: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrikethrough: () -> Unit,
    onH1: () -> Unit,
    onH2: () -> Unit,
    onCode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onLink: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(icon = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", onClick = onUndo)
                ToolbarIconButton(icon = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", onClick = onRedo)
                
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp)
                )

                ToolbarButton(label = "B", fontWeight = FontWeight.Black, onClick = onBold)
                ToolbarButton(label = "I", italic = true, onClick = onItalic)
                ToolbarButton(label = "S", strikethrough = true, onClick = onStrikethrough)
                ToolbarButton(label = "H1", onClick = onH1)
                ToolbarButton(label = "H2", onClick = onH2)
                ToolbarButton(label = "<>", onClick = onCode, isMonospace = true)
                ToolbarIconButton(icon = Icons.Default.Link, contentDescription = "Wiki Link", onClick = onLink)
            }

            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )

            Spacer(modifier = Modifier.width(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarIconButton(icon = Icons.Default.CameraAlt, contentDescription = "Camera", onClick = onCamera)
                ToolbarIconButton(icon = Icons.Default.AddPhotoAlternate, contentDescription = "Gallery", onClick = onGallery)
                ToolbarIconButton(icon = Icons.Default.CheckBox, contentDescription = "Checklist", onClick = onChecklist)
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    onClick: () -> Unit,
    fontWeight: FontWeight = FontWeight.Normal,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    isMonospace: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = fontWeight,
        fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (strikethrough) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
        fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else null,
        fontSize = 13.sp
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
    }
}
