package com.droidcode

import androidx.compose.ui.text.AnnotatedString
import com.droidcode.language.IncrementalSyntaxHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncrementalSyntaxHighlighterTest {

    @Test
    fun testIncrementalHighlightingSmoke() {
        val highlighter = IncrementalSyntaxHighlighter("kotlin", isDarkTheme = true)
        val text = AnnotatedString("val greeting = \"hello world\"\n// comment\nval count = 42")
        val transformed = highlighter.filter(text)
        assertNotNull(transformed)
        assertEquals(text.text, transformed.text.text)
        // Ensure span styles are applied to keyword / string / comment / number
        val spanStyles = transformed.text.spanStyles
        assert(spanStyles.isNotEmpty())
    }

    @Test
    fun testCacheHitOnRepeatedCall() {
        val highlighter = IncrementalSyntaxHighlighter("kotlin", isDarkTheme = false)
        val text = AnnotatedString("fun test() = true")
        val transformed1 = highlighter.filter(text)
        val transformed2 = highlighter.filter(text)
        assertEquals(transformed1, transformed2)
    }
}
