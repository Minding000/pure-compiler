package objects

import java.util.regex.Pattern

enum class WordType(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false) {
    // Whitespace
    NEW_LINE("\\n"),
    WHITESPACE("\\s", true),
    // Comments
    SINGLE_LINE_COMMENT("\\/\\/.*", true),
    MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
    // Symbols
    ASSIGNMENT("="),
    COMMA(","),
    PARENTHESES_OPEN("\\("),
    PARENTHESES_CLOSE("\\)"),
    // Operators
    ADDITION("[+-]"),
    MULTIPLICATION("[*/]"),
    // Literals
    NUMBER_LITERAL("\\d+"),
    STRING_LITERAL("\".*?\""),
    // Keywords
    VAR("var\\b"),
    ECHO("echo\\b"),
    // Identifier
    IDENTIFIER("\\w+");

    val pattern: Pattern

    init {
        this.pattern = Pattern.compile("^${pattern}")
    }
}