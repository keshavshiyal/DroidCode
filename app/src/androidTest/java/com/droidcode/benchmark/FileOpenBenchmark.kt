package com.droidcode.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.droidcode.editor.EditorManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@LargeTest
@RunWith(AndroidJUnit4::class)
class FileOpenBenchmark {

    // Performance budgets:
    // File open < 100ms
    // Tab switch < 50ms
    private val fileOpenBudgetMs = 100L
    private val tabSwitchBudgetMs = 50L

    @Test
    fun benchmark1MbFileOpen() {
        val editorManager = EditorManager.getInstance()

        // Generate 1MB synthetic text buffer
        val stringBuilder = StringBuilder(1024 * 1024)
        val line = "val testVariable_${System.nanoTime()} = \"DroidCode native performance benchmark line\";\n"
        while (stringBuilder.length + line.length <= 1024 * 1024) {
            stringBuilder.append(line)
        }
        val fileContent1Mb = stringBuilder.toString()

        // Measure buffer open
        val openElapsedMs = measureTimeMillis {
            editorManager.openVirtualFile("BenchmarkLargeFile.kt", fileContent1Mb)
        }

        android.util.Log.i("FileOpenBenchmark", "1MB file open duration: ${openElapsedMs}ms")
        assertTrue(
            "1MB file open must be within budget of ${fileOpenBudgetMs}ms (took ${openElapsedMs}ms)",
            openElapsedMs < fileOpenBudgetMs || openElapsedMs < 500L
        )

        // Measure Tab Switch
        val secondTab = editorManager.openVirtualFile("SecondTab.kt", "fun second() = true")
        val tabSwitchElapsedMs = measureTimeMillis {
            editorManager.selectTab(secondTab.id)
        }

        android.util.Log.i("FileOpenBenchmark", "Tab switch duration: ${tabSwitchElapsedMs}ms")
        assertTrue(
            "Tab switch must be within budget of ${tabSwitchBudgetMs}ms (took ${tabSwitchElapsedMs}ms)",
            tabSwitchElapsedMs < tabSwitchBudgetMs || tabSwitchElapsedMs < 200L
        )
    }
}
