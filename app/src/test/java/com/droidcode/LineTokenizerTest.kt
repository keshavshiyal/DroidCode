package com.droidcode

import com.droidcode.editor.engine.EditorTheme
import com.droidcode.editor.engine.LineState
import com.droidcode.editor.engine.LineTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LineTokenizerTest {

    private val theme = EditorTheme.darkTheme()

    @Test
    fun testValXEqualsFiveComment() {
        val tokenizer = LineTokenizer("kotlin", theme)
        val line = "val x = 5 // hi"
        val tokens = tokenizer.tokenizeLine(line)

        // Must be sorted and non-overlapping
        for (i in 0 until tokens.size - 1) {
            assertTrue("Tokens must be sorted", tokens[i].startCol <= tokens[i + 1].startCol)
            assertTrue("Tokens must not overlap", tokens[i].endCol <= tokens[i + 1].startCol)
        }

        // val (0..3)
        val valToken = tokens.find { it.startCol == 0 }
        assertEquals(3, valToken?.endCol)
        assertEquals(theme.keywordColor, valToken?.color)

        // 5 (8..9)
        val numToken = tokens.find { it.startCol == 8 }
        assertEquals(9, numToken?.endCol)
        assertEquals(theme.numberColor, numToken?.color)

        // // hi (10..15)
        val commentToken = tokens.find { it.startCol == 10 }
        assertEquals(15, commentToken?.endCol)
        assertEquals(theme.commentColor, commentToken?.color)
    }

    @Test
    fun testStringWithKeywordAndNumber() {
        val tokenizer = LineTokenizer("kotlin", theme)
        val line = "\"if 1\""
        val tokens = tokenizer.tokenizeLine(line)

        // Entire line must be a single string token
        assertEquals(1, tokens.size)
        assertEquals(0, tokens[0].startCol)
        assertEquals(6, tokens[0].endCol)
        assertEquals(theme.stringColor, tokens[0].color)
    }

    @Test
    fun testCommentInsideStringIgnored() {
        val tokenizer = LineTokenizer("kotlin", theme)
        val line = "val url = \"http://example.com\" // real comment"
        val tokens = tokenizer.tokenizeLine(line)

        // Must have keyword 'val', string '"http://example.com"', and comment '// real comment'
        val strToken = tokens.find { it.color == theme.stringColor }
        val commentToken = tokens.find { it.color == theme.commentColor }

        assertEquals(10, strToken?.startCol)
        assertEquals(30, strToken?.endCol)

        assertEquals(31, commentToken?.startCol)
        assertEquals(46, commentToken?.endCol)
    }

    @Test
    fun testEscapedQuotesInString() {
        val tokenizer = LineTokenizer("kotlin", theme)
        val line = "val s = \"a\\\"b\""
        val tokens = tokenizer.tokenizeLine(line)

        val strToken = tokens.find { it.color == theme.stringColor }
        assertEquals(8, strToken?.startCol)
        assertEquals(14, strToken?.endCol)
    }

    @Test
    fun testCssCommentsAndNoPoundOrDashComments() {
        val tokenizer = LineTokenizer("css", theme)
        val line = "#main { color: #fff; --accent: 10px; /* comment */ }"
        val tokens = tokenizer.tokenizeLine(line)

        // In CSS, # and -- are NOT comments! Only /* comment */ is.
        val comments = tokens.filter { it.color == theme.commentColor }
        assertEquals(1, comments.size)
        assertEquals(37, comments[0].startCol)
        assertEquals(50, comments[0].endCol)
    }

    @Test
    fun testMultiLineBlockCommentState() {
        val tokenizer = LineTokenizer("kotlin", theme)

        // Line 1: opens block comment
        val res1 = tokenizer.tokenizeLine("/* start of comment", LineState.NORMAL)
        assertEquals(LineState.BLOCK_COMMENT, res1.endState)
        assertEquals(1, res1.tokens.size)
        assertEquals(theme.commentColor, res1.tokens[0].color)

        // Line 2: middle of block comment
        val res2 = tokenizer.tokenizeLine("   still comment", LineState.BLOCK_COMMENT)
        assertEquals(LineState.BLOCK_COMMENT, res2.endState)
        assertEquals(1, res2.tokens.size)
        assertEquals(theme.commentColor, res2.tokens[0].color)

        // Line 3: closes comment and has code
        val res3 = tokenizer.tokenizeLine("   end */ val y = 42", LineState.BLOCK_COMMENT)
        assertEquals(LineState.NORMAL, res3.endState)

        val commentToken = res3.tokens.find { it.color == theme.commentColor }
        assertEquals(0, commentToken?.startCol)
        assertEquals(9, commentToken?.endCol)

        val valToken = res3.tokens.find { it.color == theme.keywordColor }
        assertEquals(10, valToken?.startCol)
        assertEquals(13, valToken?.endCol)
    }

    @Test
    fun testPythonDocstringAndComments() {
        val tokenizer = LineTokenizer("python", theme)

        val res1 = tokenizer.tokenizeLine("\"\"\"multiline docstring", LineState.NORMAL)
        assertEquals(LineState.PYTHON_DOCSTRING_DQ, res1.endState)
        assertEquals(theme.stringColor, res1.tokens[0].color)

        val res2 = tokenizer.tokenizeLine("closes docstring\"\"\" # then comment", LineState.PYTHON_DOCSTRING_DQ)
        assertEquals(LineState.NORMAL, res2.endState)

        val strToken = res2.tokens.find { it.color == theme.stringColor }
        val commentToken = res2.tokens.find { it.color == theme.commentColor }

        assertEquals(0, strToken?.startCol)
        assertEquals(19, strToken?.endCol)

        assertEquals(20, commentToken?.startCol)
        assertEquals(34, commentToken?.endCol)
    }

    @Test
    fun testHexAndFloatNumbers() {
        val tokenizer = LineTokenizer("kotlin", theme)
        val line = "val hex = 0xFF; val floatVal = 1e-3f; val big = 1_000_000L"
        val tokens = tokenizer.tokenizeLine(line)

        val numTokens = tokens.filter { it.color == theme.numberColor }
        assertEquals(3, numTokens.size)

        // 0xFF (10..14)
        assertEquals(10, numTokens[0].startCol)
        assertEquals(14, numTokens[0].endCol)

        // 1e-3f (31..36)
        assertEquals(31, numTokens[1].startCol)
        assertEquals(36, numTokens[1].endCol)

        // 1_000_000L (48..59)
        assertEquals(48, numTokens[2].startCol)
        assertEquals(59, numTokens[2].endCol)
    }

    @Test
    fun testLongLineCapDoesNotANR() {
        val tokenizer = LineTokenizer("javascript", theme)
        val longLine = "val x = 1; ".repeat(1000) // 11,000 characters
        val res = tokenizer.tokenizeLine(longLine, LineState.NORMAL)

        // All tokens must have endCol <= 2000
        assertTrue(res.tokens.all { it.endCol <= 2000 })
    }
}
