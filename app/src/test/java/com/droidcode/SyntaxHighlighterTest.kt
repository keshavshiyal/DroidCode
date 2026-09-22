package com.droidcode

import com.droidcode.language.SyntaxHighlighter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun testKotlinJavaKeywords() {
        val keywords = SyntaxHighlighter.getKeywordsForLanguage("kotlin")
        assertTrue(keywords.contains("fun"))
        assertTrue(keywords.contains("val"))
        assertTrue(keywords.contains("class"))
        assertTrue(keywords.contains("package"))

        val javaKeywords = SyntaxHighlighter.getKeywordsForLanguage("java")
        assertTrue(javaKeywords.contains("public"))
        assertTrue(javaKeywords.contains("void"))
        assertTrue(javaKeywords.contains("extends"))
    }

    @Test
    fun testJavaScriptTypeScriptKeywords() {
        val jsKeywords = SyntaxHighlighter.getKeywordsForLanguage("javascript")
        assertTrue(jsKeywords.contains("const"))
        assertTrue(jsKeywords.contains("let"))
        assertTrue(jsKeywords.contains("async"))
        assertTrue(jsKeywords.contains("await"))

        val tsKeywords = SyntaxHighlighter.getKeywordsForLanguage("ts")
        assertTrue(tsKeywords.contains("function"))
        assertTrue(tsKeywords.contains("export"))
    }

    @Test
    fun testPythonKeywords() {
        val pyKeywords = SyntaxHighlighter.getKeywordsForLanguage("python")
        assertTrue(pyKeywords.contains("def"))
        assertTrue(pyKeywords.contains("elif"))
        assertTrue(pyKeywords.contains("lambda"))
        assertTrue(pyKeywords.contains("None"))
    }

    @Test
    fun testSqlKeywords() {
        val sqlKeywords = SyntaxHighlighter.getKeywordsForLanguage("sql")
        assertTrue(sqlKeywords.contains("SELECT"))
        assertTrue(sqlKeywords.contains("FROM"))
        assertTrue(sqlKeywords.contains("WHERE"))
    }

    @Test
    fun testHtmlXmlKeywords() {
        val htmlKeywords = SyntaxHighlighter.getKeywordsForLanguage("html")
        assertTrue(htmlKeywords.contains("div"))
        assertTrue(htmlKeywords.contains("span"))

        val xmlKeywords = SyntaxHighlighter.getKeywordsForLanguage("xml")
        assertTrue(xmlKeywords.contains("manifest"))
        assertTrue(xmlKeywords.contains("resources"))
    }

    @Test
    fun testFallbackAndNullKeywords() {
        val defaultKeywords = SyntaxHighlighter.getKeywordsForLanguage(null)
        assertTrue(defaultKeywords.contains("return"))
        assertTrue(defaultKeywords.contains("if"))

        val unknownKeywords = SyntaxHighlighter.getKeywordsForLanguage("unknown_lang_123")
        assertTrue(unknownKeywords.contains("class"))
        assertFalse(unknownKeywords.contains("SELECT"))
    }
}
