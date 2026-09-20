package com.example.language;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SyntaxHighlighter {

    private static final Set<String> JS_KEYWORDS = new HashSet<>(Arrays.asList(
            "const", "let", "var", "function", "return", "if", "else", "for", "while",
            "switch", "case", "break", "continue", "default", "try", "catch", "finally",
            "async", "await", "import", "export", "class", "extends", "new", "this",
            "true", "false", "null", "undefined", "typeof", "instanceof"
    ));

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE",
            "TABLE", "DROP", "ALTER", "ADD", "JOIN", "INNER", "LEFT", "RIGHT", "ON",
            "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "PRIMARY", "KEY", "FOREIGN",
            "NOT", "NULL", "AND", "OR", "AS", "INTEGER", "TEXT", "VARCHAR", "DATETIME"
    ));

    private static final Set<String> PY_KEYWORDS = new HashSet<>(Arrays.asList(
            "def", "class", "return", "if", "elif", "else", "for", "while", "import",
            "from", "as", "try", "except", "finally", "with", "raise", "pass", "break",
            "continue", "True", "False", "None", "and", "or", "not", "is", "in", "lambda"
    ));

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "public", "private", "protected", "class", "interface", "extends", "implements",
            "import", "package", "static", "final", "void", "int", "boolean", "double",
            "float", "char", "String", "return", "if", "else", "for", "while", "new",
            "this", "super", "try", "catch", "finally", "throw", "throws"
    ));

    public static Set<String> getKeywordsForLanguage(String languageId) {
        if (languageId == null) return new HashSet<>();
        switch (languageId.toLowerCase()) {
            case "javascript":
            case "js":
                return JS_KEYWORDS;
            case "sql":
                return SQL_KEYWORDS;
            case "python":
            case "py":
                return PY_KEYWORDS;
            case "java":
            case "kotlin":
                return JAVA_KEYWORDS;
            default:
                return new HashSet<>();
        }
    }
}
