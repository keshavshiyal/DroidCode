package com.droidcode.language

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * High-performance, incremental line-cached syntax highlighter.
 * Instead of re-running regular expressions across the entire document on every keystroke,
 * this engine caches token spans per line. Keystrokes on a single line only re-tokenize
 * that specific line, ensuring 60-120 FPS editing across large files.
 */
class IncrementalSyntaxHighlighter(
    private val languageId: String,
    private val isDarkTheme: Boolean
) : VisualTransformation {

    companion object {
        private val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        private val stringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
        private val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val singleLineCommentRegex = Regex("//.*|#.*|--.*")
    }

    private data class StyledSpan(
        val startInLine: Int,
        val endInLine: Int,
        val color: Color,
        val isBold: Boolean = false,
        val isMedium: Boolean = false
    )

    private data class CachedLine(
        val text: String,
        val spans: List<StyledSpan>
    )

    private val lineCache = mutableMapOf<Int, CachedLine>()
    private var lastRawText: String? = null
    private var lastTransformed: TransformedText? = null

    // Theme color palette
    private val keywordColor = if (isDarkTheme) Color(0xFFCF92D7) else Color(0xFF8E24AA)
    private val stringColor = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
    private val numberColor = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFE65100)
    private val commentColor = if (isDarkTheme) Color(0xFF78909C) else Color(0xFF546E7A)
    private val typeColor = if (isDarkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)

    private val keywords = SyntaxHighlighter.getKeywordsForLanguage(languageId)

    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text
        if (code == lastRawText && lastTransformed != null) {
            return lastTransformed!!
        }

        if (code.isEmpty()) {
            val empty = TransformedText(text, OffsetMapping.Identity)
            lastRawText = code
            lastTransformed = empty
            return empty
        }

        // Fast-path for extremely large files (> 50,000 lines)
        val shouldHighlight = code.length <= 150_000

        val highlighted = buildAnnotatedString {
            append(code)
            if (!shouldHighlight) return@buildAnnotatedString

            var lineStartIndex = 0
            var lineIndex = 0
            val len = code.length

            while (lineStartIndex < len) {
                var lineEndIndex = code.indexOf('\n', lineStartIndex)
                if (lineEndIndex == -1) {
                    lineEndIndex = len
                }

                val lineText = code.substring(lineStartIndex, lineEndIndex)
                val cached = lineCache[lineIndex]

                val spans = if (cached != null && cached.text == lineText) {
                    cached.spans
                } else {
                    val freshSpans = tokenizeLine(lineText)
                    lineCache[lineIndex] = CachedLine(lineText, freshSpans)
                    freshSpans
                }

                // Apply spans to current line
                for (span in spans) {
                    val start = lineStartIndex + span.startInLine
                    val end = lineStartIndex + span.endInLine
                    if (start in 0..len && end in start..len) {
                        val weight = when {
                            span.isBold -> FontWeight.Bold
                            span.isMedium -> FontWeight.Medium
                            else -> FontWeight.Normal
                        }
                        addStyle(SpanStyle(color = span.color, fontWeight = weight), start, end)
                    }
                }

                lineIndex++
                lineStartIndex = lineEndIndex + 1
            }

            // Evict stale line cache entries if document shrank
            if (lineCache.size > lineIndex + 100) {
                val keysToRemove = lineCache.keys.filter { it >= lineIndex }
                for (k in keysToRemove) {
                    lineCache.remove(k)
                }
            }
        }

        val result = TransformedText(highlighted, OffsetMapping.Identity)
        lastRawText = code
        lastTransformed = result
        return result
    }

    private fun tokenizeLine(lineText: String): List<StyledSpan> {
        if (lineText.isBlank()) return emptyList()

        val spans = mutableListOf<StyledSpan>()

        // 1. Single-line comment check (takes precedence for rest of line)
        val commentMatch = singleLineCommentRegex.find(lineText)
        val commentStart = commentMatch?.range?.first ?: -1

        if (commentMatch != null) {
            spans.add(
                StyledSpan(
                    startInLine = commentMatch.range.first,
                    endInLine = commentMatch.range.last + 1,
                    color = commentColor
                )
            )
        }

        val codeSlice = if (commentStart != -1) lineText.substring(0, commentStart) else lineText

        // 2. Strings
        for (m in stringRegex.findAll(codeSlice)) {
            spans.add(
                StyledSpan(
                    startInLine = m.range.first,
                    endInLine = m.range.last + 1,
                    color = stringColor
                )
            )
        }

        // 3. Numbers
        for (m in numberRegex.findAll(codeSlice)) {
            spans.add(
                StyledSpan(
                    startInLine = m.range.first,
                    endInLine = m.range.last + 1,
                    color = numberColor
                )
            )
        }

        // 4. Words (Keywords & Types)
        for (m in wordRegex.findAll(codeSlice)) {
            val word = m.value
            if (keywords.contains(word) || keywords.contains(word.lowercase())) {
                spans.add(
                    StyledSpan(
                        startInLine = m.range.first,
                        endInLine = m.range.last + 1,
                        color = keywordColor,
                        isBold = true
                    )
                )
            } else if (word.isNotEmpty() && word.first().isUpperCase()) {
                spans.add(
                    StyledSpan(
                        startInLine = m.range.first,
                        endInLine = m.range.last + 1,
                        color = typeColor,
                        isMedium = true
                    )
                )
            }
        }

        return spans
    }
}
