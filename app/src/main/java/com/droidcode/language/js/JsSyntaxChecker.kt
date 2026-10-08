package com.droidcode.language.js

enum class DiagnosticSeverity {
    ERROR, WARNING, INFO
}

data class JsDiagnostic(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: DiagnosticSeverity = DiagnosticSeverity.ERROR
)

object JsSyntaxChecker {

    private data class BracketLocation(
        val char: Char,
        val line: Int,
        val col: Int
    )

    /**
     * Performs lightweight, high-performance static syntax analysis on JavaScript/TypeScript code.
     */
    fun checkSyntax(code: String): List<JsDiagnostic> {
        if (code.isBlank()) return emptyList()

        val diagnostics = ArrayList<JsDiagnostic>()
        val lines = code.split("\n")
        val bracketStack = ArrayDeque<BracketLocation>()

        var inBlockComment = false
        var inTemplateLiteral = false
        var templateStartLine = 0
        var templateStartCol = 0

        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineNum = lineIdx + 1
            var col = 0
            val len = line.length

            while (col < len) {
                // Multi-line block comments /* ... */
                if (inBlockComment) {
                    val endComment = line.indexOf("*/", col)
                    if (endComment != -1) {
                        inBlockComment = false
                        col = endComment + 2
                    } else {
                        break // whole remainder of line is comment
                    }
                    continue
                }

                // Template literals ` ... `
                if (inTemplateLiteral) {
                    if (line[col] == '\\') {
                        col += 2 // skip escaped char
                        continue
                    }
                    if (line[col] == '`') {
                        inTemplateLiteral = false
                        col++
                    } else {
                        col++
                    }
                    continue
                }

                // Check line comment //
                if (col < len - 1 && line[col] == '/' && line[col + 1] == '/') {
                    break // rest of line is line comment
                }

                // Check block comment start /*
                if (col < len - 1 && line[col] == '/' && line[col + 1] == '*') {
                    inBlockComment = true
                    col += 2
                    continue
                }

                val ch = line[col]

                // String literals '...' and "..."
                if (ch == '"' || ch == '\'') {
                    val quote = ch
                    val startCol = col + 1
                    var escaped = false
                    var closed = false
                    col++
                    while (col < len) {
                        val c = line[col]
                        if (escaped) {
                            escaped = false
                        } else if (c == '\\') {
                            escaped = true
                        } else if (c == quote) {
                            closed = true
                            col++
                            break
                        }
                        col++
                    }
                    if (!closed) {
                        diagnostics.add(
                            JsDiagnostic(
                                line = lineNum,
                                column = startCol,
                                message = "Unclosed string literal",
                                severity = DiagnosticSeverity.ERROR
                            )
                        )
                    }
                    continue
                }

                // Template string literal `...`
                if (ch == '`') {
                    inTemplateLiteral = true
                    templateStartLine = lineNum
                    templateStartCol = col + 1
                    col++
                    continue
                }

                // Brackets opening
                if (ch == '(' || ch == '[' || ch == '{') {
                    bracketStack.addLast(BracketLocation(ch, lineNum, col + 1))
                    col++
                    continue
                }

                // Brackets closing
                if (ch == ')' || ch == ']' || ch == '}') {
                    if (bracketStack.isEmpty()) {
                        diagnostics.add(
                            JsDiagnostic(
                                line = lineNum,
                                column = col + 1,
                                message = "Unmatched closing bracket '$ch'",
                                severity = DiagnosticSeverity.ERROR
                            )
                        )
                    } else {
                        val top = bracketStack.removeLast()
                        val expected = when (top.char) {
                            '(' -> ')'
                            '[' -> ']'
                            '{' -> '}'
                            else -> ' '
                        }
                        if (ch != expected) {
                            diagnostics.add(
                                JsDiagnostic(
                                    line = lineNum,
                                    column = col + 1,
                                    message = "Mismatched closing bracket: found '$ch' but expected '$expected' (opened at line ${top.line}, col ${top.col})",
                                    severity = DiagnosticSeverity.ERROR
                                )
                            )
                        }
                    }
                    col++
                    continue
                }

                // Check common operator syntax anomalies (e.g. triple plus '+++')
                if (col < len - 2 && ch == '+' && line[col + 1] == '+' && line[col + 2] == '+') {
                    diagnostics.add(
                        JsDiagnostic(
                            line = lineNum,
                            column = col + 1,
                            message = "Invalid sequence '+++'",
                            severity = DiagnosticSeverity.WARNING
                        )
                    )
                }

                col++
            }
        }

        // Unclosed template literal at EOF
        if (inTemplateLiteral) {
            diagnostics.add(
                JsDiagnostic(
                    line = templateStartLine,
                    column = templateStartCol,
                    message = "Unclosed template literal '`' reaching end of file",
                    severity = DiagnosticSeverity.ERROR
                )
            )
        }

        // Remaining unclosed brackets
        while (bracketStack.isNotEmpty()) {
            val unclosed = bracketStack.removeLast()
            diagnostics.add(
                JsDiagnostic(
                    line = unclosed.line,
                    column = unclosed.col,
                    message = "Unclosed opening bracket '${unclosed.char}'",
                    severity = DiagnosticSeverity.ERROR
                )
            )
        }

        return diagnostics.sortedWith(compareBy({ it.line }, { it.column }))
    }
}
