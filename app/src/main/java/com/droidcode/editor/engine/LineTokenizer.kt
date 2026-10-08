package com.droidcode.editor.engine

import com.droidcode.language.SyntaxHighlighter

data class SyntaxToken(
    val startCol: Int,
    val endCol: Int,
    val color: Int,
    val isBold: Boolean = false
) : Comparable<SyntaxToken> {
    override fun compareTo(other: SyntaxToken): Int {
        val startDiff = this.startCol.compareTo(other.startCol)
        return if (startDiff != 0) startDiff else this.endCol.compareTo(other.endCol)
    }
}

enum class LineState {
    NORMAL,
    BLOCK_COMMENT,        // /* ... */
    HTML_COMMENT,         // <!-- ... -->
    PYTHON_DOCSTRING_DQ,  // """ ... """
    PYTHON_DOCSTRING_SQ,  // ''' ... '''
    JS_TEMPLATE_LITERAL   // ` ... `
}

data class TokenizeResult(
    val tokens: List<SyntaxToken>,
    val endState: LineState
)

/**
 * Robust, production-grade line tokenizer for DroidCodeEngine.
 *
 * Implements a left-to-right lexical scanner with language-specific comment rules,
 * multi-line state preservation (block comments, docstrings, template literals),
 * strictly sorted and non-overlapping tokens, Unicode identifier support,
 * and long-line ANR prevention (capped at 2,000 chars).
 */
class LineTokenizer(
    private var languageId: String,
    private var theme: EditorTheme
) {
    private var keywords = SyntaxHighlighter.getKeywordsForLanguage(languageId)
    private val lineCache = HashMap<String, TokenizeResult>()

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
        return tokenizeLine(lineText, LineState.NORMAL).tokens
    }

    fun tokenizeLine(lineText: String): List<SyntaxToken> {
        return tokenizeLine(lineText, LineState.NORMAL).tokens
    }

    fun tokenizeLine(lineText: String, startState: LineState): TokenizeResult {
        if (lineText.isEmpty()) {
            return TokenizeResult(emptyList(), startState)
        }

        // Cap long lines to 2,000 characters to prevent ANR on minified files
        val maxLen = minOf(lineText.length, 2000)
        val cacheKey = "${startState.ordinal}_${if (lineText.length > 2000) lineText.substring(0, 2000) else lineText}"
        val cached = lineCache[cacheKey]
        if (cached != null) {
            return cached
        }

        val tokens = ArrayList<SyntaxToken>()
        var i = 0
        var currentState = startState

        val lang = languageId.lowercase()
        val isPython = lang == "python" || lang == "py"
        val isShell = lang == "shell" || lang == "bash" || lang == "sh"
        val isCss = lang == "css"
        val isSql = lang == "sql"
        val isHtmlOrXml = lang == "html" || lang == "htm" || lang == "xml" || lang == "markdown" || lang == "md"
        val isJsOrTs = lang == "javascript" || lang == "typescript" || lang == "js" || lang == "ts" || lang == "jsx" || lang == "tsx"
        val isJson = lang == "json"

        // Handle continuation of multi-line states from previous line
        if (currentState == LineState.BLOCK_COMMENT) {
            val closeIdx = lineText.indexOf("*/", i)
            if (closeIdx != -1 && closeIdx + 2 <= maxLen) {
                tokens.add(SyntaxToken(0, closeIdx + 2, theme.commentColor))
                i = closeIdx + 2
                currentState = LineState.NORMAL
            } else {
                tokens.add(SyntaxToken(0, maxLen, theme.commentColor))
                val result = TokenizeResult(tokens, LineState.BLOCK_COMMENT)
                cacheResult(cacheKey, result)
                return result
            }
        } else if (currentState == LineState.HTML_COMMENT) {
            val closeIdx = lineText.indexOf("-->", i)
            if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                tokens.add(SyntaxToken(0, closeIdx + 3, theme.commentColor))
                i = closeIdx + 3
                currentState = LineState.NORMAL
            } else {
                tokens.add(SyntaxToken(0, maxLen, theme.commentColor))
                val result = TokenizeResult(tokens, LineState.HTML_COMMENT)
                cacheResult(cacheKey, result)
                return result
            }
        } else if (currentState == LineState.PYTHON_DOCSTRING_DQ) {
            val closeIdx = lineText.indexOf("\"\"\"", i)
            if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                tokens.add(SyntaxToken(0, closeIdx + 3, theme.stringColor))
                i = closeIdx + 3
                currentState = LineState.NORMAL
            } else {
                tokens.add(SyntaxToken(0, maxLen, theme.stringColor))
                val result = TokenizeResult(tokens, LineState.PYTHON_DOCSTRING_DQ)
                cacheResult(cacheKey, result)
                return result
            }
        } else if (currentState == LineState.PYTHON_DOCSTRING_SQ) {
            val closeIdx = lineText.indexOf("'''", i)
            if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                tokens.add(SyntaxToken(0, closeIdx + 3, theme.stringColor))
                i = closeIdx + 3
                currentState = LineState.NORMAL
            } else {
                tokens.add(SyntaxToken(0, maxLen, theme.stringColor))
                val result = TokenizeResult(tokens, LineState.PYTHON_DOCSTRING_SQ)
                cacheResult(cacheKey, result)
                return result
            }
        } else if (currentState == LineState.JS_TEMPLATE_LITERAL) {
            var closeIdx = -1
            var j = i
            while (j < maxLen) {
                if (lineText[j] == '\\') {
                    j += 2
                } else if (lineText[j] == '`') {
                    closeIdx = j
                    break
                } else {
                    j++
                }
            }
            if (closeIdx != -1) {
                tokens.add(SyntaxToken(0, closeIdx + 1, theme.stringColor))
                i = closeIdx + 1
                currentState = LineState.NORMAL
            } else {
                tokens.add(SyntaxToken(0, maxLen, theme.stringColor))
                val result = TokenizeResult(tokens, LineState.JS_TEMPLATE_LITERAL)
                cacheResult(cacheKey, result)
                return result
            }
        }

        // Left-to-right lexical scan
        while (i < maxLen) {
            val c = lineText[i]

            // 1. Whitespace
            if (c.isWhitespace()) {
                i++
                continue
            }

            // 2. Comments
            // HTML / XML comment <!-- ... -->
            if (isHtmlOrXml && i + 3 < maxLen && lineText.startsWith("<!--", i)) {
                val closeIdx = lineText.indexOf("-->", i + 4)
                if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                    tokens.add(SyntaxToken(i, closeIdx + 3, theme.commentColor))
                    i = closeIdx + 3
                } else {
                    tokens.add(SyntaxToken(i, maxLen, theme.commentColor))
                    currentState = LineState.HTML_COMMENT
                    break
                }
                continue
            }

            // Block comment /* ... */ (Kotlin, Java, JS, TS, CSS, SQL)
            if (!isPython && !isShell && i + 1 < maxLen && lineText[i] == '/' && lineText[i + 1] == '*') {
                val closeIdx = lineText.indexOf("*/", i + 2)
                if (closeIdx != -1 && closeIdx + 2 <= maxLen) {
                    tokens.add(SyntaxToken(i, closeIdx + 2, theme.commentColor))
                    i = closeIdx + 2
                } else {
                    tokens.add(SyntaxToken(i, maxLen, theme.commentColor))
                    currentState = LineState.BLOCK_COMMENT
                    break
                }
                continue
            }

            // Single line // comment (Kotlin, Java, JS, TS)
            if (!isPython && !isShell && !isCss && !isSql && i + 1 < maxLen && lineText[i] == '/' && lineText[i + 1] == '/') {
                tokens.add(SyntaxToken(i, maxLen, theme.commentColor))
                break
            }

            // Single line # comment (Python, Shell)
            if ((isPython || isShell) && c == '#') {
                tokens.add(SyntaxToken(i, maxLen, theme.commentColor))
                break
            }

            // Single line -- comment (SQL)
            if (isSql && i + 1 < maxLen && lineText[i] == '-' && lineText[i + 1] == '-') {
                tokens.add(SyntaxToken(i, maxLen, theme.commentColor))
                break
            }

            // 3. Strings & Docstrings
            // Python / Kotlin triple double-quotes """
            if ((isPython || lang == "kotlin") && i + 2 < maxLen && lineText.startsWith("\"\"\"", i)) {
                val closeIdx = lineText.indexOf("\"\"\"", i + 3)
                if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                    tokens.add(SyntaxToken(i, closeIdx + 3, theme.stringColor))
                    i = closeIdx + 3
                } else {
                    tokens.add(SyntaxToken(i, maxLen, theme.stringColor))
                    currentState = LineState.PYTHON_DOCSTRING_DQ
                    break
                }
                continue
            }

            // Python triple single-quotes '''
            if (isPython && i + 2 < maxLen && lineText.startsWith("'''", i)) {
                val closeIdx = lineText.indexOf("'''", i + 3)
                if (closeIdx != -1 && closeIdx + 3 <= maxLen) {
                    tokens.add(SyntaxToken(i, closeIdx + 3, theme.stringColor))
                    i = closeIdx + 3
                } else {
                    tokens.add(SyntaxToken(i, maxLen, theme.stringColor))
                    currentState = LineState.PYTHON_DOCSTRING_SQ
                    break
                }
                continue
            }

            // JS / TS template literal `
            if (isJsOrTs && c == '`') {
                var closeIdx = -1
                var j = i + 1
                while (j < maxLen) {
                    if (lineText[j] == '\\') {
                        j += 2
                    } else if (lineText[j] == '`') {
                        closeIdx = j
                        break
                    } else {
                        j++
                    }
                }
                if (closeIdx != -1) {
                    tokens.add(SyntaxToken(i, closeIdx + 1, theme.stringColor))
                    i = closeIdx + 1
                } else {
                    tokens.add(SyntaxToken(i, maxLen, theme.stringColor))
                    currentState = LineState.JS_TEMPLATE_LITERAL
                    break
                }
                continue
            }

            // Regular strings: "..." or '...'
            if (c == '"' || (c == '\'' && !isSql && lang != "rust")) {
                val quote = c
                var j = i + 1
                var foundEnd = false
                while (j < maxLen) {
                    if (lineText[j] == '\\') {
                        j += 2 // skip escaped quote e.g. \" or \'
                    } else if (lineText[j] == quote) {
                        foundEnd = true
                        j++
                        break
                    } else {
                        j++
                    }
                }
                tokens.add(SyntaxToken(i, minOf(j, maxLen), theme.stringColor))
                i = j
                continue
            }

            // 4. Numbers (Hex, Binary, Octal, Floats, Decimals with underscores, suffixes)
            if (c.isDigit() || (c == '.' && i + 1 < maxLen && lineText[i + 1].isDigit())) {
                val startNum = i
                var j = i
                if (c == '0' && j + 1 < maxLen && (lineText[j + 1] == 'x' || lineText[j + 1] == 'X')) {
                    // Hex literal 0xFF
                    j += 2
                    while (j < maxLen && (lineText[j].isDigit() || (lineText[j] in 'a'..'f') || (lineText[j] in 'A'..'F') || lineText[j] == '_')) {
                        j++
                    }
                } else if (c == '0' && j + 1 < maxLen && (lineText[j + 1] == 'b' || lineText[j + 1] == 'B')) {
                    // Binary literal 0b101
                    j += 2
                    while (j < maxLen && (lineText[j] == '0' || lineText[j] == '1' || lineText[j] == '_')) {
                        j++
                    }
                } else {
                    // Decimal / Float
                    while (j < maxLen && (lineText[j].isDigit() || lineText[j] == '_')) {
                        j++
                    }
                    if (j < maxLen && lineText[j] == '.' && (j + 1 >= maxLen || lineText[j + 1] != '.')) {
                        j++
                        while (j < maxLen && (lineText[j].isDigit() || lineText[j] == '_')) {
                            j++
                        }
                    }
                    // Exponent e.g. 1e-3
                    if (j < maxLen && (lineText[j] == 'e' || lineText[j] == 'E')) {
                        j++
                        if (j < maxLen && (lineText[j] == '+' || lineText[j] == '-')) {
                            j++
                        }
                        while (j < maxLen && (lineText[j].isDigit() || lineText[j] == '_')) {
                            j++
                        }
                    }
                }
                // Number suffix e.g. L, f, d, u
                if (j < maxLen && lineText[j] in "fFdDlLuu") {
                    j++
                }
                tokens.add(SyntaxToken(startNum, j, theme.numberColor))
                i = j
                continue
            }

            // 5. Identifiers, Keywords, Types (Unicode letter aware)
            if (Character.isLetter(c) || c == '_' || c == '$') {
                val startWord = i
                var j = i
                while (j < maxLen && (Character.isLetterOrDigit(lineText[j]) || lineText[j] == '_' || lineText[j] == '$')) {
                    j++
                }
                val word = lineText.substring(startWord, j)
                if (keywords.contains(word) || keywords.contains(word.lowercase()) ||
                    (isJson && (word == "true" || word == "false" || word == "null"))
                ) {
                    tokens.add(SyntaxToken(startWord, j, theme.keywordColor, isBold = true))
                } else if (word.isNotEmpty() && Character.isUpperCase(word[0])) {
                    tokens.add(SyntaxToken(startWord, j, theme.typeColor))
                }
                i = j
                continue
            }

            // Fallthrough single character (symbols, operators, brackets)
            i++
        }

        val result = TokenizeResult(tokens, currentState)
        cacheResult(cacheKey, result)
        return result
    }

    private fun cacheResult(key: String, result: TokenizeResult) {
        if (lineCache.size > 2000) {
            lineCache.clear()
        }
        lineCache[key] = result
    }
}
