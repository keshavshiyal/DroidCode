package com.droidcode.language.json

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

enum class JsonNodeType {
    OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
}

data class JsonTreeNode(
    val key: String?,
    val value: Any?,
    val type: JsonNodeType,
    val children: List<JsonTreeNode> = emptyList(),
    val summary: String = ""
)

data class JsonValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val line: Int = 1,
    val column: Int = 1
)

object JsonToolHelper {

    /**
     * Formats JSON text with standard indentation (defaults to 2 spaces).
     */
    fun formatJson(content: String, indentSpaces: Int = 2): Result<String> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.success("")

        return try {
            val tokener = JSONTokener(trimmed)
            val firstChar = trimmed[0]
            val formatted = when (firstChar) {
                '{' -> {
                    val obj = JSONObject(tokener)
                    obj.toString(indentSpaces)
                }
                '[' -> {
                    val arr = JSONArray(tokener)
                    arr.toString(indentSpaces)
                }
                else -> {
                    // Try parsing as object or array
                    val nextVal = tokener.nextValue()
                    if (nextVal is JSONObject) {
                        nextVal.toString(indentSpaces)
                    } else if (nextVal is JSONArray) {
                        nextVal.toString(indentSpaces)
                    } else {
                        return Result.failure(IllegalArgumentException("Content is not a JSON object or array"))
                    }
                }
            }
            Result.success(formatted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Minifies JSON text by removing insignificant whitespace outside string literals.
     */
    fun minifyJson(content: String): Result<String> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.success("")

        return try {
            val tokener = JSONTokener(trimmed)
            val firstChar = trimmed[0]
            val minified = when (firstChar) {
                '{' -> JSONObject(tokener).toString()
                '[' -> JSONArray(tokener).toString()
                else -> {
                    val nextVal = tokener.nextValue()
                    if (nextVal is JSONObject) nextVal.toString()
                    else if (nextVal is JSONArray) nextVal.toString()
                    else return Result.failure(IllegalArgumentException("Content is not a JSON object or array"))
                }
            }
            Result.success(minified)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates JSON text, reporting syntax errors with exact line and column numbers.
     */
    fun validateJson(content: String): JsonValidationResult {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return JsonValidationResult(isValid = true)
        }

        try {
            val tokener = JSONTokener(trimmed)
            val value = tokener.nextValue()
            if (value !is JSONObject && value !is JSONArray) {
                return JsonValidationResult(
                    isValid = false,
                    errorMessage = "Root value must be an object '{ ... }' or array '[ ... ]'",
                    line = 1,
                    column = 1
                )
            }
            // Ensure no trailing unparsed tokens (e.g. extra closing braces)
            if (tokener.more()) {
                val next = tokener.nextClean()
                if (next != 0.toChar()) {
                    return JsonValidationResult(
                        isValid = false,
                        errorMessage = "Unexpected trailing characters after root JSON object",
                        line = computeLine(content, tokener.toString()),
                        column = 1
                    )
                }
            }
            return JsonValidationResult(isValid = true)
        } catch (e: Exception) {
            val msg = e.message ?: "Invalid JSON syntax"
            val (line, col) = parseLineAndColFromError(msg, content)
            return JsonValidationResult(
                isValid = false,
                errorMessage = msg,
                line = line,
                column = col
            )
        }
    }

    /**
     * Builds an in-memory hierarchical tree representation from JSON text.
     */
    fun buildJsonTree(content: String): Result<JsonTreeNode> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.success(JsonTreeNode(key = "root", value = null, type = JsonNodeType.NULL, summary = "empty"))
        }

        return try {
            val tokener = JSONTokener(trimmed)
            val root = tokener.nextValue()
            val treeNode = parseNode(key = "root", value = root)
            Result.success(treeNode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseNode(key: String?, value: Any?): JsonTreeNode {
        return when (value) {
            null, JSONObject.NULL -> JsonTreeNode(
                key = key,
                value = null,
                type = JsonNodeType.NULL,
                summary = "null"
            )
            is JSONObject -> {
                val keys = value.keys()
                val children = ArrayList<JsonTreeNode>()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = value.opt(k)
                    children.add(parseNode(key = k, value = v))
                }
                JsonTreeNode(
                    key = key,
                    value = value,
                    type = JsonNodeType.OBJECT,
                    children = children,
                    summary = "{ ${children.size} properties }"
                )
            }
            is JSONArray -> {
                val children = ArrayList<JsonTreeNode>()
                for (i in 0 until value.length()) {
                    val item = value.opt(i)
                    children.add(parseNode(key = "[$i]", value = item))
                }
                JsonTreeNode(
                    key = key,
                    value = value,
                    type = JsonNodeType.ARRAY,
                    children = children,
                    summary = "[ ${children.size} items ]"
                )
            }
            is String -> JsonTreeNode(
                key = key,
                value = value,
                type = JsonNodeType.STRING,
                summary = "\"$value\""
            )
            is Number -> JsonTreeNode(
                key = key,
                value = value,
                type = JsonNodeType.NUMBER,
                summary = value.toString()
            )
            is Boolean -> JsonTreeNode(
                key = key,
                value = value,
                type = JsonNodeType.BOOLEAN,
                summary = value.toString()
            )
            else -> JsonTreeNode(
                key = key,
                value = value.toString(),
                type = JsonNodeType.STRING,
                summary = value.toString()
            )
        }
    }

    private fun parseLineAndColFromError(errorMsg: String, content: String): Pair<Int, Int> {
        // org.json often throws: "at character 45 of ..."
        val charMatch = Regex("at character (\\d+)").find(errorMsg)
        if (charMatch != null) {
            val charIdx = charMatch.groupValues[1].toIntOrNull() ?: 0
            val prefix = content.take(charIdx)
            val lines = prefix.split("\n")
            val line = lines.size
            val col = lines.last().length + 1
            return Pair(line, col)
        }
        return Pair(1, 1)
    }

    private fun computeLine(content: String, remaining: String): Int {
        val consumedLen = content.length - remaining.length
        if (consumedLen <= 0) return 1
        return content.take(consumedLen).count { it == '\n' } + 1
    }
}
