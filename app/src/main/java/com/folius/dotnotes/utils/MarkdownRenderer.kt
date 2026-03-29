package com.folius.dotnotes.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Robust Markdown → Block-based renderer for DotNotes.
 */
sealed class MarkdownBlock {
    data class Heading(val text: AnnotatedString, val level: Int) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class Quote(val blocks: List<MarkdownBlock>, val level: Int) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock()
    data class ListItem(val text: AnnotatedString, val level: Int, val isOrdered: Boolean, val number: Int? = null, val isChecklist: Boolean = false, val isChecked: Boolean? = null) : MarkdownBlock()
    data class Table(val headers: List<AnnotatedString>, val rows: List<List<AnnotatedString>>) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

object MarkdownRenderer {

    // Regex constants
    private val BOLD_ITALIC_REGEX = Regex("\\*\\*\\*(.+?)\\*\\*\\*|___(.+?)___")
    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__")
    private val ITALIC_REGEX = Regex("\\*(.+?)\\*|_(.+?)_")
    private val STRIKETHROUGH_REGEX = Regex("~~(.+?)~~")
    private val CODE_REGEX = Regex("`(.+?)`")
    private val WIKI_LINK_REGEX = Regex("\\[\\[(.+?)]]")
    private val MD_LINK_REGEX = Regex("\\[(.+?)]\\((.+?)\\)")
    private val TAG_REGEX = Regex("(^|\\s)(#[a-zA-Z0-9_]{1,30})")

    fun parseBlocks(
        markdown: String,
        linkColor: Color = Color(0xFF4FC3F7),
        codeColor: Color = Color(0xFF81C784),
        codeBackground: Color = Color(0x33808080)
    ): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()

            // 1. Fenced Code Block
            if (trimmedLine.startsWith("```")) {
                val language = trimmedLine.removePrefix("```").trim().ifEmpty { null }
                i++
                val codeLines = mutableListOf<String>()
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
                i++
                continue
            }

            // 2. Horizontal Rule (Allowing spaces like * * * or - - -)
            val hrRegex = Regex("^([-*=_])(?:\\s*\\1){2,}\\s*$")
            if (hrRegex.matches(trimmedLine)) {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // 3. Heading
            val headingMatch = Regex("^(#{1,6})\\s+(.*)").matchEntire(trimmedLine)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2]
                blocks.add(MarkdownBlock.Heading(renderInline(text, linkColor, codeColor, codeBackground), level))
                i++
                continue
            }

            // 4. Blockquote (Nested)
            if (trimmedLine.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                    i++
                }
                // Recursively parse inner blocks for the quote
                val nestedBlocks = parseBlocks(quoteLines.joinToString("\n"), linkColor, codeColor, codeBackground)
                blocks.add(MarkdownBlock.Quote(nestedBlocks, 1)) // Basic nesting support
                continue
            }

            // 5. List Item (Including checklist support)
            val bulletMatch = Regex("^(\\s*)([-*+])\\s+(.*)").matchEntire(line)
            val orderedMatch = Regex("^(\\s*)(\\d+)\\.\\s+(.*)").matchEntire(line)
            if (bulletMatch != null) {
                val indent = bulletMatch.groupValues[1].length / 2
                var contentText = bulletMatch.groupValues[3]
                var isChecklist = false
                var isChecked: Boolean? = null
                
                if (contentText.startsWith("[ ] ")) {
                    isChecklist = true
                    isChecked = false
                    contentText = contentText.removePrefix("[ ] ")
                } else if (contentText.startsWith("[x] ") || contentText.startsWith("[X] ")) {
                    isChecklist = true
                    isChecked = true
                    contentText = contentText.removePrefix("[x] ").removePrefix("[X] ")
                }
                
                blocks.add(MarkdownBlock.ListItem(renderInline(contentText, linkColor, codeColor, codeBackground), indent, false, isChecklist = isChecklist, isChecked = isChecked))
                i++
                continue
            } else if (orderedMatch != null) {
                val indent = orderedMatch.groupValues[1].length / 2
                val number = orderedMatch.groupValues[2].toInt()
                val text = orderedMatch.groupValues[3]
                blocks.add(MarkdownBlock.ListItem(renderInline(text, linkColor, codeColor, codeBackground), indent, true, number))
                i++
                continue
            }

            // 6. Table (Improved detection, optional outer pipes)
            val isTableHeader = trimmedLine.contains("|") && i + 1 < lines.size && 
                                lines[i+1].trim().matches(Regex("^[|\\s:-]*--[|\\s:-]*$"))
            
            if (isTableHeader) {
                val headerRow = trimmedLine.trim('|').split("|").map { it.trim() }
                val headers = headerRow.map { renderInline(it, linkColor, codeColor, codeBackground) }
                i += 2 // Skip header and separator
                val rows = mutableListOf<List<AnnotatedString>>()
                while (i < lines.size && lines[i].contains("|")) {
                    val rowText = lines[i].trim().trim('|')
                    val rowCells = rowText.split("|").map { renderInline(it.trim(), linkColor, codeColor, codeBackground) }
                    rows.add(rowCells)
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
                continue
            }

            // 7. Paragraph
            if (trimmedLine.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(renderInline(line, linkColor, codeColor, codeBackground)))
            }
            i++
        }

        return blocks
    }

    private fun renderInline(
        text: String,
        linkColor: Color,
        codeColor: Color,
        codeBackground: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            var lastPos = 0
            
            data class StyleToken(val range: IntRange, val style: SpanStyle? = null, val tag: String? = null, val annotation: String? = null, val content: String? = null)
            
            val tokens = mutableListOf<StyleToken>()

            // Find all tokens
            WIKI_LINK_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), "wiki_link", it.groupValues[1], it.groupValues[1])) }
            MD_LINK_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), "url", it.groupValues[2], it.groupValues[1])) }
            CODE_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, background = codeBackground), content = it.groupValues[1])) }
            BOLD_ITALIC_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), content = it.groupValues[1].ifEmpty { it.groupValues[2] })) }
            BOLD_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(fontWeight = FontWeight.Bold), content = it.groupValues[1].ifEmpty { it.groupValues[2] })) }
            ITALIC_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(fontStyle = FontStyle.Italic), content = it.groupValues[1].ifEmpty { it.groupValues[2] })) }
            STRIKETHROUGH_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(textDecoration = TextDecoration.LineThrough), content = it.groupValues[1])) }
            TAG_REGEX.findAll(text).forEach { tokens.add(StyleToken(it.range, SpanStyle(color = linkColor, fontWeight = FontWeight.Bold), "tag", it.groupValues[2])) }

            // Sort tokens: primary by start position, secondary by length (longer first)
            val sortedTokens = tokens.sortedWith(compareBy({ it.range.first }, { -(it.range.last - it.range.first) }))
            val processedRanges = mutableListOf<IntRange>()

            for (token in sortedTokens) {
                // Skip if this range is already covered by a parent token (since we sorted by length/start)
                if (processedRanges.any { it.first <= token.range.first && it.last >= token.range.last }) continue
                
                // Append text before the token
                if (token.range.first > lastPos) {
                    append(text.substring(lastPos, token.range.first))
                }
                
                // Handle annotations
                if (token.tag != null && token.annotation != null) {
                    pushStringAnnotation(tag = token.tag, annotation = token.annotation)
                }
                
                // Append styled (potentially recursive) content
                if (token.style != null) {
                    withStyle(token.style) {
                        if (token.content != null && token.tag == null) { // Recursive call if it's a style-only token (nested markdown)
                            append(renderInline(token.content, linkColor, codeColor, codeBackground))
                        } else {
                            append(token.content ?: text.substring(token.range))
                        }
                    }
                } else {
                    append(token.content ?: text.substring(token.range))
                }
                
                if (token.tag != null && token.annotation != null) pop()
                
                lastPos = token.range.last + 1
                processedRanges.add(token.range)
            }
            
            // Append remaining text
            if (lastPos < text.length) {
                append(text.substring(lastPos))
            }
        }
    }

    // Legacy support (fallback)
    fun render(
        markdown: String,
        baseColor: Color = Color.Unspecified,
        linkColor: Color = Color(0xFF4FC3F7),
        codeColor: Color = Color(0xFF81C784),
        codeBackground: Color = Color(0x33808080)
    ): AnnotatedString {
        val blocks = parseBlocks(markdown, linkColor, codeColor, codeBackground)
        return buildAnnotatedString {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (28 - (block.level * 2)).sp)) {
                            append(block.text)
                        }
                    }
                    else -> append("Block")
                }
                append("\n")
            }
        }
    }
}
