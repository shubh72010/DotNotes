package com.folius.dotnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folius.dotnotes.utils.MarkdownBlock

@Composable
fun MarkdownBlockView(
    block: MarkdownBlock,
    onNavigateToNote: (Int) -> Unit,
    onWikiLinkClick: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    when (block) {
        is MarkdownBlock.Heading -> {
            val fontSize = when (block.level) {
                1 -> 28.sp
                2 -> 24.sp
                3 -> 20.sp
                4 -> 18.sp
                else -> 16.sp
            }
            Text(
                text = block.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        is MarkdownBlock.Paragraph -> {
            MarkdownClickableText(
                text = block.text,
                onWikiLinkClick = onWikiLinkClick,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        is MarkdownBlock.Quote -> {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .matchParentSize()
                        .background(colorScheme.primary.copy(alpha = 0.5f))
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    block.blocks.forEach { nestedBlock ->
                        MarkdownBlockView(nestedBlock, onNavigateToNote, onWikiLinkClick)
                    }
                }
            }
        }
        is MarkdownBlock.CodeBlock -> {
            Surface(
                color = colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (block.language != null) {
                        Text(
                            text = block.language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
        is MarkdownBlock.ListItem -> {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .padding(start = (block.level * 16).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (block.isChecklist) {
                    Icon(
                        imageVector = if (block.isChecked == true) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp),
                        tint = if (block.isChecked == true) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (block.isOrdered) "${block.number ?: 1}." else "•",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(24.dp),
                        color = colorScheme.primary
                    )
                }
                MarkdownClickableText(
                    text = block.text,
                    onWikiLinkClick = onWikiLinkClick,
                    modifier = Modifier.alpha(if (block.isChecklist && block.isChecked == true) 0.6f else 1f)
                )
            }
        }
        is MarkdownBlock.Table -> {
            Box(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
                    .border(1.dp, colorScheme.outlineVariant, MaterialTheme.shapes.small)
            ) {
                Column {
                    // Header
                    Row(modifier = Modifier.background(colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        block.headers.forEach { header ->
                            TableCell(header, isHeader = true, onWikiLinkClick = onWikiLinkClick)
                        }
                    }
                    // Rows
                    block.rows.forEach { row ->
                        HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        Row {
                            row.forEach { cell ->
                                TableCell(cell, isHeader = false, onWikiLinkClick = onWikiLinkClick)
                            }
                        }
                    }
                }
            }
        }
        MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth().alpha(0.3f),
                thickness = 1.dp,
                color = colorScheme.outline
            )
        }
    }
}

@Composable
fun TableCell(
    text: AnnotatedString,
    isHeader: Boolean,
    onWikiLinkClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .widthIn(min = 120.dp) // Removed 200dp max constraint for large tables
    ) {
        MarkdownClickableText(
            text = text,
            onWikiLinkClick = onWikiLinkClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
fun MarkdownClickableText(
    text: AnnotatedString,
    onWikiLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ClickableText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            text.getStringAnnotations("wiki_link", offset, offset).firstOrNull()?.let { annotation ->
                onWikiLinkClick(annotation.item)
            }
            text.getStringAnnotations("url", offset, offset).firstOrNull()?.let { annotation ->
                // Handle URL click if needed
            }
        }
    )
}
