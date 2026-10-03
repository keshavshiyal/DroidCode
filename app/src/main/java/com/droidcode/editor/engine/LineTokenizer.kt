package com.droidcode.editor.engine

import com.droidcode.language.SyntaxHighlighter

data class SyntaxToken(
    val startCol: Int,
    val endCol: Int,
    val color: Int,
    val isBold: Boolean = false
)

/**
 * High-speed, viewport-only line tokenizer for DroidCodeEngine.
 *
 * Instead of running regexes across the entire 100,000-line file on every keystroke,
 * this engine only tokenizes the ~35 lines visible in the active viewport,
 * caching results per line content.
 */
class LineTokenizer(
    private var languageId: String,
    private var theme: EditorTheme
) {
    companion object {
        private val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        private val stringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
        private val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        private val singleLineCommentRegex = Regex("//.*|#.*|--.*")
    }

    private var keywords = SyntaxHighlighter.getKeywordsForLanguage(languageId)
    private val lineCache = HashMap<String, List<SyntaxToken>>()

    fun clearCache() {
        lineCache.clear()
    }

    fun updateConfig(newLanguageId: String, newTheme: EditorTheme) {
        if (this.languageId != newLanguageId || this.theme != newTheme) {
            this.languageId = newLanguageId
            this.theme = newTheme
            this.keywords = SyntaxHighlighter.getKeywordsForLanguage(newLanguageId)
            this.lineCache.clear()
        }
    }

    fun tokenizeLine(lineIndex: Int, lineText: String): List<SyntaxToken> {
        return tokenizeLine(lineText)
    }

    fun tokenizeLine(lineText: String): List<SyntaxToken> {
        if (lineText.isBlank()) return emptyList()

        val cached = lineCache[lineText]
        if (cached != null) {
            return cached
        }

        val tokens = ArrayList<SyntaxToken>()

        // 1. Single-line comment check (takes precedence for rest of line)
        val commentMatch = singleLineCommentRegex.find(lineText)
        val commentStart = commentMatch?.range?.first ?: -1

        if (commentMatch != null) {
            tokens.add(
                SyntaxToken(
                    startCol = commentMatch.range.first,
                    endCol = commentMatch.range.last + 1,
                    color = theme.commentColor
                )
            )
        }

        val codeSlice = if (commentStart != -1) lineText.substring(0, commentStart) else lineText

        // 2. Strings
        for (m in stringRegex.findAll(codeSlice)) {
            tokens.add(
                SyntaxToken(
                    startCol = m.range.first,
                    endCol = m.range.last + 1,
                    color = theme.stringColor
                )
            )
        }

        // 3. Numbers
        for (m in numberRegex.findAll(codeSlice)) {
            tokens.add(
                SyntaxToken(
                    startCol = m.range.first,
                    endCol = m.range.last + 1,
                    color = theme.numberColor
                )
            )
        }

        // 4. Keywords & Types
        for (m in wordRegex.findAll(codeSlice)) {
            val word = m.value
            if (keywords.contains(word) || keywords.contains(word.lowercase())) {
                tokens.add(
                    SyntaxToken(
                        startCol = m.range.first,
                        endCol = m.range.last + 1,
                        color = theme.keywordColor,
                        isBold = true
                    )
                )
            } else if (word.isNotEmpty() && word.first().isUpperCase()) {
                tokens.add(
                    SyntaxToken(
                        startCol = m.range.first,
                        endCol = m.range.last + 1,
                        color = theme.typeColor
                    )
                )
            }
        }

        // Keep cache bounded
        if (lineCache.size > 2000) {
            lineCache.clear()
        }
        lineCache[lineText] = tokens
        return tokens
    }
}
