package com.droidcode.language;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SyntaxHighlighter {

    private static final Set<String> KOTLIN_JAVA_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "package", "import", "class", "interface", "fun", "val", "var", "public", "private",
            "protected", "return", "if", "else", "for", "while", "when", "try", "catch", "throw",
            "throws", "object", "sealed", "data", "override", "final", "static", "new", "null",
            "true", "false", "void", "extends", "implements", "this", "super", "int", "boolean",
            "double", "float", "char", "String"
    )));

    private static final Set<String> JS_TS_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "import", "export", "from", "class", "function", "const", "let", "var", "return",
            "if", "else", "for", "while", "switch", "case", "break", "continue", "default",
            "try", "catch", "finally", "async", "await", "extends", "new", "this", "true",
            "false", "null", "undefined", "typeof", "instanceof"
    )));

    private static final Set<String> PYTHON_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "def", "class", "import", "from", "return", "if", "elif", "else", "for", "while",
            "try", "except", "finally", "with", "as", "pass", "break", "continue", "raise",
            "None", "True", "False", "lambda", "yield", "and", "or", "not", "is", "in"
    )));

    private static final Set<String> HTML_XML_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "div", "span", "p", "a", "body", "head", "html", "script", "style", "link",
            "meta", "resources", "string", "layout", "manifest"
    )));

    private static final Set<String> SQL_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE", "TABLE",
            "DROP", "ALTER", "ADD", "JOIN", "INNER", "LEFT", "RIGHT", "ON", "GROUP", "BY",
            "ORDER", "HAVING", "LIMIT", "PRIMARY", "KEY", "FOREIGN", "NOT", "NULL", "AND",
            "OR", "AS", "INTEGER", "TEXT", "VARCHAR", "DATETIME"
    )));

    private static final Set<String> DEFAULT_KEYWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "val", "var", "fun", "def", "class", "function", "return", "if", "else",
            "import", "public", "private"
    )));

    public static Set<String> getKeywordsForLanguage(String languageId) {
        if (languageId == null) {
            return DEFAULT_KEYWORDS;
        }
        switch (languageId.toLowerCase().trim()) {
            case "kotlin":
            case "kt":
            case "java":
                return KOTLIN_JAVA_KEYWORDS;
            case "javascript":
            case "js":
            case "typescript":
            case "ts":
            case "jsx":
            case "tsx":
                return JS_TS_KEYWORDS;
            case "python":
            case "py":
                return PYTHON_KEYWORDS;
            case "html":
            case "xml":
            case "svg":
                return HTML_XML_KEYWORDS;
            case "sql":
                return SQL_KEYWORDS;
            default:
                return DEFAULT_KEYWORDS;
        }
    }
}
