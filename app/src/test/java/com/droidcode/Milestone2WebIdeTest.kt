package com.droidcode

import androidx.compose.ui.graphics.Color
import com.droidcode.language.css.CssColorHelper
import com.droidcode.language.js.DiagnosticSeverity
import com.droidcode.language.js.JsSyntaxChecker
import com.droidcode.language.json.JsonToolHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Milestone2WebIdeTest {

    // ==========================================
    // JSON Tools Tests
    // ==========================================

    @Test
    fun testJsonFormatAndMinify() {
        val compactJson = "{\"name\":\"DroidCode\",\"version\":2,\"features\":[\"web\",\"json\",\"css\"]}"
        val formatResult = JsonToolHelper.formatJson(compactJson)
        assertTrue("Formatting should succeed", formatResult.isSuccess)
        val formatted = formatResult.getOrThrow()
        assertTrue("Formatted JSON should contain newlines", formatted.contains("\n"))
        assertTrue("Formatted JSON should contain indentation", formatted.contains("  \"name\""))

        val minifyResult = JsonToolHelper.minifyJson(formatted)
        assertTrue("Minifying should succeed", minifyResult.isSuccess)
        val minified = minifyResult.getOrThrow()
        assertFalse("Minified JSON should not contain newlines", minified.contains("\n"))
        assertTrue("Minified JSON should retain content", minified.contains("\"DroidCode\""))
    }

    @Test
    fun testJsonValidation() {
        val validJson = "{\"status\": \"ok\", \"code\": 200}"
        val validRes = JsonToolHelper.validateJson(validJson)
        assertTrue("Valid JSON should report isValid=true", validRes.isValid)

        val invalidJson = "{\"status\": \"ok\", \"code\": }"
        val invalidRes = JsonToolHelper.validateJson(invalidJson)
        assertFalse("Invalid JSON should report isValid=false", invalidRes.isValid)
        assertNotNull("Invalid JSON should report an error message", invalidRes.errorMessage)
    }

    @Test
    fun testJsonTreeHierarchy() {
        val sample = """
            {
                "title": "DroidCode",
                "count": 42,
                "active": true,
                "tags": ["android", "ide", "web"],
                "author": { "name": "Keshav" }
            }
        """.trimIndent()
        val tree = JsonToolHelper.buildTree(sample)
        assertEquals("Root should be object", "object", tree.type)
        assertEquals("Root should have 5 children", 5, tree.children.size)

        val tagsNode = tree.children.find { it.key == "tags" }
        assertNotNull("Tags node must exist", tagsNode)
        assertEquals("Tags should be array", "array", tagsNode!!.type)
        assertEquals("Tags array should have 3 items", 3, tagsNode.children.size)
    }

    // ==========================================
    // CSS Color Helper Tests
    // ==========================================

    @Test
    fun testCssColorParsing() {
        // Hex colors
        val red6 = CssColorHelper.parseColor("#FF0000")
        assertNotNull("Should parse #FF0000", red6)
        assertEquals(1f, red6!!.red, 0.01f)
        assertEquals(0f, red6.green, 0.01f)
        assertEquals(0f, red6.blue, 0.01f)

        val green3 = CssColorHelper.parseColor("#0F0")
        assertNotNull("Should parse #0F0", green3)
        assertEquals(0f, green3!!.red, 0.01f)
        assertEquals(1f, green3.green, 0.01f)
        assertEquals(0f, green3.blue, 0.01f)

        // RGB / RGBA
        val blueRgb = CssColorHelper.parseColor("rgb(0, 0, 255)")
        assertNotNull("Should parse rgb(0, 0, 255)", blueRgb)
        assertEquals(1f, blueRgb!!.blue, 0.01f)

        val alphaRgba = CssColorHelper.parseColor("rgba(255, 255, 0, 0.5)")
        assertNotNull("Should parse rgba(255, 255, 0, 0.5)", alphaRgba)
        assertEquals(0.5f, alphaRgba!!.alpha, 0.05f)

        // HSL
        val hslRed = CssColorHelper.parseColor("hsl(0, 100%, 50%)")
        assertNotNull("Should parse hsl(0, 100%, 50%)", hslRed)
        assertEquals(1f, hslRed!!.red, 0.05f)
    }

    @Test
    fun testFindColorsInCssText() {
        val css = """
            body {
                background-color: #1E1E1E;
                color: #FFFFFF;
                border: 1px solid rgb(255, 0, 0);
            }
        """.trimIndent()
        val found = CssColorHelper.findAllColorsInText(css)
        assertEquals("Should find 3 color definitions", 3, found.size)
        assertEquals("#1E1E1E", found[0].rawText)
        assertEquals("#FFFFFF", found[1].rawText)
        assertEquals("rgb(255, 0, 0)", found[2].rawText)
    }

    @Test
    fun testColorToHex() {
        val c = Color(red = 1f, green = 0f, blue = 0f, alpha = 1f)
        assertEquals("#FF0000", CssColorHelper.toHexString(c, includeAlpha = false))
        assertEquals("#FFFF0000", CssColorHelper.toHexString(c, includeAlpha = true))
    }

    // ==========================================
    // JS Lightweight Syntax Checker Tests
    // ==========================================

    @Test
    fun testJsCleanSyntax() {
        val cleanJs = """
            function greet(name) {
                const msg = `Hello, ${'$'}{name}!`;
                console.log(msg);
                return [1, 2, 3];
            }
        """.trimIndent()
        val diagnostics = JsSyntaxChecker.checkSyntax(cleanJs)
        assertTrue("Valid JS should have no syntax errors", diagnostics.isEmpty())
    }

    @Test
    fun testJsUnmatchedBrackets() {
        val unclosedBrace = "function test() { console.log('hi');"
        val diags = JsSyntaxChecker.checkSyntax(unclosedBrace)
        assertTrue("Should detect unclosed brace", diags.any { it.message.contains("Unclosed") && it.message.contains("{") })

        val mismatched = "const arr = [1, 2, 3);"
        val diags2 = JsSyntaxChecker.checkSyntax(mismatched)
        assertTrue("Should detect mismatched bracket", diags2.any { it.message.contains("Mismatched") })
    }

    @Test
    fun testJsUnclosedLiterals() {
        val unclosedStr = "const str = 'hello world;"
        val diags = JsSyntaxChecker.checkSyntax(unclosedStr)
        assertTrue("Should detect unclosed string literal", diags.any { it.message.contains("Unclosed string") })

        val unclosedTemplate = "const str = `hello world;"
        val diags2 = JsSyntaxChecker.checkSyntax(unclosedTemplate)
        assertTrue("Should detect unclosed template literal", diags2.any { it.message.contains("Unclosed template literal") })
    }
}
