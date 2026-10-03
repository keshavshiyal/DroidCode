package com.droidcode.editor

/**
 * Status of a line in the editor relative to its original saved state or Git baseline.
 */
enum class LineDiffStatus {
    NONE,
    ADDED,
    MODIFIED
}

/**
 * High-performance line diff engine for real-time editor gutter diff indicators.
 * Compares current buffer content with original saved buffer content.
 */
object LineDiffCalculator {

    fun computeDiff(original: String, current: String): Map<Int, LineDiffStatus> {
        if (original == current) return emptyMap()

        val origLines = original.lines()
        val currLines = current.lines()

        if (origLines.isEmpty() || (origLines.size == 1 && origLines[0].isEmpty())) {
            return currLines.indices.associateWith { LineDiffStatus.ADDED }
        }

        val result = mutableMapOf<Int, LineDiffStatus>()
        val origSize = origLines.size
        val currSize = currLines.size

        // 1. Match common prefix
        var prefixLen = 0
        val maxPrefix = minOf(origSize, currSize)
        while (prefixLen < maxPrefix && origLines[prefixLen] == currLines[prefixLen]) {
            prefixLen++
        }

        // 2. Match common suffix
        var suffixLen = 0
        val maxSuffix = minOf(origSize - prefixLen, currSize - prefixLen)
        while (suffixLen < maxSuffix && origLines[origSize - 1 - suffixLen] == currLines[currSize - 1 - suffixLen]) {
            suffixLen++
        }

        val origMiddleStart = prefixLen
        val origMiddleEnd = origSize - suffixLen
        val currMiddleStart = prefixLen
        val currMiddleEnd = currSize - suffixLen

        val origMiddleCount = origMiddleEnd - origMiddleStart
        val currMiddleCount = currMiddleEnd - currMiddleStart

        if (currMiddleCount <= 0) {
            // Lines were deleted only, no current lines to highlight as added/modified
            return result
        }

        if (origMiddleCount <= 0) {
            // All middle lines in current are newly added
            for (i in currMiddleStart until currMiddleEnd) {
                result[i] = LineDiffStatus.ADDED
            }
            return result
        }

        // Fast path for small-to-medium middle section: LCS diff
        if (origMiddleCount <= 500 && currMiddleCount <= 500) {
            val lcs = Array(origMiddleCount + 1) { IntArray(currMiddleCount + 1) }
            for (i in 0 until origMiddleCount) {
                for (j in 0 until currMiddleCount) {
                    if (origLines[origMiddleStart + i] == currLines[currMiddleStart + j]) {
                        lcs[i + 1][j + 1] = lcs[i][j] + 1
                    } else {
                        lcs[i + 1][j + 1] = maxOf(lcs[i + 1][j], lcs[i][j + 1])
                    }
                }
            }

            var i = origMiddleCount
            var j = currMiddleCount
            val matchedCurrIndices = mutableSetOf<Int>()
            while (i > 0 && j > 0) {
                if (origLines[origMiddleStart + i - 1] == currLines[currMiddleStart + j - 1]) {
                    matchedCurrIndices.add(currMiddleStart + j - 1)
                    i--
                    j--
                } else if (lcs[i - 1][j] >= lcs[i][j - 1]) {
                    i--
                } else {
                    j--
                }
            }

            val hasOrigReplacements = origMiddleCount > 0
            for (currIdx in currMiddleStart until currMiddleEnd) {
                if (!matchedCurrIndices.contains(currIdx)) {
                    val status = if (hasOrigReplacements && (currIdx - currMiddleStart) < origMiddleCount) {
                        LineDiffStatus.MODIFIED
                    } else {
                        LineDiffStatus.ADDED
                    }
                    result[currIdx] = status
                }
            }
        } else {
            // For larger diffs: mark all middle lines as modified/added without quadratic overhead
            for (currIdx in currMiddleStart until currMiddleEnd) {
                val status = if ((currIdx - currMiddleStart) < origMiddleCount) {
                    LineDiffStatus.MODIFIED
                } else {
                    LineDiffStatus.ADDED
                }
                result[currIdx] = status
            }
        }

        return result
    }
}
