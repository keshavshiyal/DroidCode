package com.droidcode.language.css

import android.graphics.Color
import androidx.annotation.ColorInt

data class CssColorMatch(
    val startCol: Int,
    val endCol: Int,
    val rawText: String,
    @ColorInt val color: Int
)

object CssColorHelper {

    // Regex for HEX: #RGB, #RGBA, #RRGGBB, #RRGGBBAA
    private val HEX_PATTERN = Regex("#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})\\b")

    // Regex for rgb/rgba: rgb(255, 0, 0), rgba(255, 0, 0, 0.5), rgb(255 0 0 / 0.5)
    private val RGB_PATTERN = Regex("rgba?\\(\\s*(\\d{1,3}%?)\\s*[,\\s]\\s*(\\d{1,3}%?)\\s*[,\\s]\\s*(\\d{1,3}%?)(?:\\s*[,/]\\s*([0-9.]+%?))?\\s*\\)")

    // Regex for hsl/hsla: hsl(120, 100%, 50%), hsla(120, 100%, 50%, 0.5)
    private val HSL_PATTERN = Regex("hsla?\\(\\s*(\\d{1,3}(?:deg)?)\\s*[,\\s]\\s*(\\d{1,3}%)\\s*[,\\s]\\s*(\\d{1,3}%)(?:\\s*[,/]\\s*([0-9.]+%?))?\\s*\\)")

    /**
     * Parses a CSS color string into an Android ARGB color integer.
     */
    fun parseColor(colorString: String): Int? {
        val trimmed = colorString.trim()
        try {
            // Hex color
            if (trimmed.startsWith("#")) {
                val hex = trimmed.substring(1)
                return when (hex.length) {
                    3 -> {
                        // #RGB -> #RRGGBB
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        Color.rgb(r, g, b)
                    }
                    4 -> {
                        // #RGBA -> #AARRGGBB
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        val a = hex[3].toString().repeat(2).toInt(16)
                        Color.argb(a, r, g, b)
                    }
                    6 -> {
                        Color.parseColor(trimmed)
                    }
                    8 -> {
                        // In CSS #RRGGBBAA, in Android #AARRGGBB
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        val a = hex.substring(6, 8).toInt(16)
                        Color.argb(a, r, g, b)
                    }
                    else -> null
                }
            }

            // RGB / RGBA
            val rgbMatch = RGB_PATTERN.matchEntire(trimmed)
            if (rgbMatch != null) {
                val r = parseComponent(rgbMatch.groupValues[1], 255)
                val g = parseComponent(rgbMatch.groupValues[2], 255)
                val b = parseComponent(rgbMatch.groupValues[3], 255)
                val aStr = rgbMatch.groupValues[4]
                val a = if (aStr.isNotEmpty()) parseAlpha(aStr) else 255
                return Color.argb(a, r, g, b)
            }

            // HSL / HSLA
            val hslMatch = HSL_PATTERN.matchEntire(trimmed)
            if (hslMatch != null) {
                val h = hslMatch.groupValues[1].removeSuffix("deg").toFloatOrNull() ?: 0f
                val s = hslMatch.groupValues[2].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                val l = hslMatch.groupValues[3].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                val aStr = hslMatch.groupValues[4]
                val a = if (aStr.isNotEmpty()) parseAlpha(aStr) else 255
                val rgb = hslToRgb(h, s, l)
                return Color.argb(a, rgb[0], rgb[1], rgb[2])
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    /**
     * Finds all CSS color matches within a line of code.
     */
    fun findColorsInLine(line: String): List<CssColorMatch> {
        if (line.isEmpty() || (!line.contains('#') && !line.contains("rgb") && !line.contains("hsl"))) {
            return emptyList()
        }

        val results = ArrayList<CssColorMatch>()

        // 1. Search Hex
        for (match in HEX_PATTERN.findAll(line)) {
            val color = parseColor(match.value)
            if (color != null) {
                results.add(CssColorMatch(match.range.first, match.range.last + 1, match.value, color))
            }
        }

        // 2. Search RGB/RGBA
        for (match in RGB_PATTERN.findAll(line)) {
            val color = parseColor(match.value)
            if (color != null) {
                results.add(CssColorMatch(match.range.first, match.range.last + 1, match.value, color))
            }
        }

        // 3. Search HSL/HSLA
        for (match in HSL_PATTERN.findAll(line)) {
            val color = parseColor(match.value)
            if (color != null) {
                results.add(CssColorMatch(match.range.first, match.range.last + 1, match.value, color))
            }
        }

        return results.sortedBy { it.startCol }
    }

    /**
     * Formats an ARGB color as a standard Hex string `#RRGGBB` or `#RRGGBBAA`.
     */
    fun toHexString(@ColorInt color: Int, includeAlpha: Boolean = false): String {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return if (includeAlpha && a < 255) {
            String.format("#%02X%02X%02X%02X", r, g, b, a)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    /**
     * Formats an ARGB color as `rgb(r, g, b)` or `rgba(r, g, b, a)`.
     */
    fun toRgbString(@ColorInt color: Int): String {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return if (a < 255) {
            val alphaFloat = (a / 255f)
            String.format(java.util.Locale.US, "rgba(%d, %d, %d, %.2f)", r, g, b, alphaFloat)
        } else {
            "rgb($r, $g, $b)"
        }
    }

    private fun parseComponent(str: String, max: Int): Int {
        return if (str.endsWith("%")) {
            val pct = str.removeSuffix("%").toFloatOrNull() ?: 0f
            ((pct / 100f) * max).toInt().coerceIn(0, max)
        } else {
            str.toIntOrNull()?.coerceIn(0, max) ?: 0
        }
    }

    private fun parseAlpha(str: String): Int {
        return if (str.endsWith("%")) {
            val pct = str.removeSuffix("%").toFloatOrNull() ?: 0f
            ((pct / 100f) * 255).toInt().coerceIn(0, 255)
        } else {
            val fl = str.toFloatOrNull() ?: 1f
            (fl * 255).toInt().coerceIn(0, 255)
        }
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        var r = 0f
        var g = 0f
        var b = 0f

        when {
            h < 60f -> { r = c; g = x; b = 0f }
            h < 120f -> { r = x; g = c; b = 0f }
            h < 180f -> { r = 0f; g = c; b = x }
            h < 240f -> { r = 0f; g = x; b = c }
            h < 300f -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        return intArrayOf(
            ((r + m) * 255f).toInt().coerceIn(0, 255),
            ((g + m) * 255f).toInt().coerceIn(0, 255),
            ((b + m) * 255f).toInt().coerceIn(0, 255)
        )
    }
}
