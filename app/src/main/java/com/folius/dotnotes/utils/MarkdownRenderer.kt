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
 * Lightweight Markdown → AnnotatedString renderer.
 * Supports: headings, bold, italic, code, strikethrough, [[wiki-links]], and [links](url).
 */
object MarkdownRenderer {

    private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.*)")
    private val BOLD_REGEX = Regex("\\*\\*(.+?)\\*\\*")
    private val ITALIC_REGEX = Regex("\\*(.+?)\\*")
    private val CODE_REGEX = Regex("`(.+?)`")
    private val STRIKETHROUGH_REGEX = Regex("~~(.+?)~~")
    private val WIKI_LINK_REGEX = Regex("\\[\\[(.+?)]]")
    private val MD_LINK_REGEX = Regex("\\[(.+?)]\\((.+?)\\)")

    fun render(
        markdown: String,
        baseColor: Color = Color.Unspecified,
        linkColor: Color = Color(0xFF4FC3F7),
        codeColor: Color = Color(0xFF81C784),
        codeBackground: Color = Color(0x33808080)
    ): AnnotatedString {
        return buildAnnotatedString {
            val lines = markdown.lines()
            lines.forEachIndexed { lineIndex, line ->
                val headingMatch = HEADING_REGEX.matchEntire(line)
                if (headingMatch != null) {
                    val level = headingMatch.groupValues[1].length
                    val text = headingMatch.groupValues[2]
                    val fontSize = when (level) {
                        1 -> 28.sp
                        2 -> 24.sp
                        3 -> 20.sp
                        4 -> 18.sp
                        else -> 16.sp
                    }
                    withStyle(SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        color = baseColor
                    )) {
                        appendInlineFormatted(this, text, linkColor, codeColor, codeBackground)
                    }
                } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
                    append("  • ")
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                    appendInlineFormatted(this, content, linkColor, codeColor, codeBackground)
                } else if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                    val num = line.trimStart().substringBefore('.')
                    val content = line.trimStart().substringAfter('.').trimStart()
                    append("  $num. ")
                    appendInlineFormatted(this, content, linkColor, codeColor, codeBackground)
                } else {
                    appendInlineFormatted(this, line, linkColor, codeColor, codeBackground)
                }
                if (lineIndex < lines.size - 1) append("\n")
            }
        }
    }

    private fun appendInlineFormatted(
        builder: AnnotatedString.Builder,
        text: String,
        linkColor: Color,
        codeColor: Color,
        codeBackground: Color
    ) {
        data class FormatSpan(val start: Int, val end: Int, val style: SpanStyle, val annotation: String? = null, val annotationTag: String? = null, val displayText: String)

        val spans = mutableListOf<FormatSpan>()

        // Collect all regex matches; order by start position
        for (match in WIKI_LINK_REGEX.findAll(text)) {
            spans.add(FormatSpan(
                start = match.range.first,
                end = match.range.last + 1,
                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                annotation = match.groupValues[1],
                annotationTag = "wiki_link",
                displayText = match.groupValues[1]
            ))
        }
        for (match in MD_LINK_REGEX.findAll(text)) {
            spans.add(FormatSpan(
                start = match.range.first,
                end = match.range.last + 1,
                style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                annotation = match.groupValues[2],
                annotationTag = "url",
                displayText = match.groupValues[1]
            ))
        }
        for (match in CODE_REGEX.findAll(text)) {
            spans.add(FormatSpan(
                start = match.range.first,
                end = match.range.last + 1,
                style = SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, background = codeBackground),
                displayText = match.groupValues[1]
            ))
        }
        for (match in BOLD_REGEX.findAll(text)) {
            if (spans.none { it.start <= match.range.first && it.end >= match.range.last + 1 }) {
                spans.add(FormatSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    displayText = match.groupValues[1]
                ))
            }
        }
        for (match in ITALIC_REGEX.findAll(text)) {
            if (spans.none { it.start <= match.range.first && it.end >= match.range.last + 1 }) {
                spans.add(FormatSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    style = SpanStyle(fontStyle = FontStyle.Italic),
                    displayText = match.groupValues[1]
                ))
            }
        }
        for (match in STRIKETHROUGH_REGEX.findAll(text)) {
            if (spans.none { it.start <= match.range.first && it.end >= match.range.last + 1 }) {
                spans.add(FormatSpan(
                    start = match.range.first,
                    end = match.range.last + 1,
                    style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                    displayText = match.groupValues[1]
                ))
            }
        }

        // Sort by start pos
        val sorted = spans.sortedBy { it.start }

        var cursor = 0
        for (span in sorted) {
            if (span.start < cursor) continue // overlapping, skip
            if (span.start > cursor) {
                builder.append(text.substring(cursor, span.start))
            }
            if (span.annotationTag != null && span.annotation != null) {
                builder.pushStringAnnotation(tag = span.annotationTag, annotation = span.annotation)
                builder.withStyle(span.style) {
                    append(span.displayText)
                }
                builder.pop()
            } else {
                builder.withStyle(span.style) {
                    append(span.displayText)
                }
            }
            cursor = span.end
        }
        if (cursor < text.length) {
            builder.append(text.substring(cursor))
        }
    }
}
