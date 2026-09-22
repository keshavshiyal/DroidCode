package com.droidcode

import com.droidcode.ui.FileIconUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FileIconUtilsTest {

    @Test
    fun testWebLanguagesCategorization() {
        assertEquals("HTML", FileIconUtils.classifyFile("index.html"))
        assertEquals("HTML", FileIconUtils.classifyFile("about.htm"))
        assertEquals("CSS", FileIconUtils.classifyFile("styles.css"))
        assertEquals("CSS", FileIconUtils.classifyFile("theme.scss"))
        assertEquals("CSS", FileIconUtils.classifyFile("variables.sass"))
        assertEquals("CSS", FileIconUtils.classifyFile("style.less"))
        assertEquals("JAVASCRIPT", FileIconUtils.classifyFile("bundle.js"))
        assertEquals("JAVASCRIPT", FileIconUtils.classifyFile("component.jsx"))
        assertEquals("TYPESCRIPT", FileIconUtils.classifyFile("service.ts"))
        assertEquals("TYPESCRIPT", FileIconUtils.classifyFile("widget.tsx"))
        assertEquals("JSON", FileIconUtils.classifyFile("package.json"))
        assertEquals("MARKDOWN", FileIconUtils.classifyFile("notes.md"))
        assertEquals("README", FileIconUtils.classifyFile("README.md"))
        assertEquals("SQL", FileIconUtils.classifyFile("schema.sql"))
    }

    @Test
    fun testProgrammingLanguagesCategorization() {
        assertEquals("PYTHON", FileIconUtils.classifyFile("script.py"))
        assertEquals("JAVA", FileIconUtils.classifyFile("MainActivity.java"))
        assertEquals("KOTLIN", FileIconUtils.classifyFile("App.kt"))
        assertEquals("KOTLIN", FileIconUtils.classifyFile("build.gradle.kts"))
        assertEquals("C", FileIconUtils.classifyFile("main.c"))
        assertEquals("CPP", FileIconUtils.classifyFile("engine.cpp"))
        assertEquals("CPP", FileIconUtils.classifyFile("header.hpp"))
        assertEquals("CSHARP", FileIconUtils.classifyFile("Program.cs"))
        assertEquals("GO", FileIconUtils.classifyFile("server.go"))
        assertEquals("RUST", FileIconUtils.classifyFile("lib.rs"))
        assertEquals("RUBY", FileIconUtils.classifyFile("app.rb"))
        assertEquals("PHP", FileIconUtils.classifyFile("index.php"))
        assertEquals("SWIFT", FileIconUtils.classifyFile("ContentView.swift"))
    }

    @Test
    fun testDataAndConfigFilesCategorization() {
        assertEquals("XML", FileIconUtils.classifyFile("AndroidManifest.xml"))
        assertEquals("YAML", FileIconUtils.classifyFile("docker-compose.yml"))
        assertEquals("TOML", FileIconUtils.classifyFile("libs.versions.toml"))
        assertEquals("CONFIG", FileIconUtils.classifyFile("app.properties"))
        assertEquals("CONFIG", FileIconUtils.classifyFile("setup.ini"))
        assertEquals("ENV", FileIconUtils.classifyFile(".env"))
        assertEquals("DATA", FileIconUtils.classifyFile("data.csv"))
        assertEquals("DATA", FileIconUtils.classifyFile("records.tsv"))
    }

    @Test
    fun testBuildAndToolchainFiles() {
        assertEquals("DOCKER", FileIconUtils.classifyFile("Dockerfile"))
        assertEquals("BUILD", FileIconUtils.classifyFile("Makefile"))
        assertEquals("MAVEN", FileIconUtils.classifyFile("pom.xml"))
        assertEquals("GIT", FileIconUtils.classifyFile(".gitignore"))
        assertEquals("GIT", FileIconUtils.classifyFile(".gitmodules"))
        assertEquals("LICENSE", FileIconUtils.classifyFile("LICENSE"))
        assertEquals("CONFIG", FileIconUtils.classifyFile(".editorconfig"))
    }

    @Test
    fun testMediaAndAssets() {
        assertEquals("IMAGE", FileIconUtils.classifyFile("banner.png"))
        assertEquals("IMAGE", FileIconUtils.classifyFile("photo.jpg"))
        assertEquals("IMAGE", FileIconUtils.classifyFile("vector.svg"))
        assertEquals("IMAGE", FileIconUtils.classifyFile("image.avif"))
        assertEquals("IMAGE", FileIconUtils.classifyFile("raw.heic"))
        assertEquals("PDF", FileIconUtils.classifyFile("document.pdf"))
        assertEquals("VIDEO", FileIconUtils.classifyFile("intro.mp4"))
    }
}
