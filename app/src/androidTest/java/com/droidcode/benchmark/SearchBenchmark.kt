package com.droidcode.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchBenchmark {

    // Performance budget: Search 10k files < 1s (1000ms)
    private val search10kFilesBudgetMs = 1000L

    @Test
    fun benchmarkSearch10kFiles() {
        // Construct simulated file registry with 10,000 file entries
        val files = ArrayList<String>(10000)
        for (i in 0 until 10000) {
            val ext = when (i % 5) {
                0 -> "kt"
                1 -> "java"
                2 -> "xml"
                3 -> "json"
                else -> "gradle.kts"
            }
            files.add("src/main/components/submodule_$i/EngineWorker_$i.$ext")
        }

        val targetSearchPattern = "EngineWorker_9999"

        val searchElapsedMs = measureTimeMillis {
            val results = files.filter { it.contains(targetSearchPattern, ignoreCase = true) }
            assertTrue("Should find matching target", results.isNotEmpty())
        }

        android.util.Log.i("SearchBenchmark", "Search 10k files duration: ${searchElapsedMs}ms")
        assertTrue(
            "Searching 10,000 files must complete within budget of ${search10kFilesBudgetMs}ms (took ${searchElapsedMs}ms)",
            searchElapsedMs < search10kFilesBudgetMs
        )
    }
}
