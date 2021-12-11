package word_generation

import java.util.regex.Pattern

enum class WordType(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false) {
	// Whitespace
	LINE_BREAK("\\n"),
	WHITESPACE("\\s", true),
	// Comments
	SINGLE_LINE_COMMENT("\\/\\/.*", true),
	MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
	// Symbols
	ASSIGNMENT("="),
	COMMA(","),
	COLON(":"),
	DOT("\\."),
	PARENTHESES_OPEN("\\("),
	PARENTHESES_CLOSE("\\)"),
	BRACES_OPEN("\\{"),
	BRACES_CLOSE("\\}"),
	// Operators
	ADDITION("[+-]"),
	MULTIPLICATION("[*/]"),
	EXPONENTIATION("\\^"),
	// Literals
	NUMBER_LITERAL("\\d+"),
	STRING_LITERAL("\".*?\""),
	// Keywords
	IF("if\\b"),
	ELSE("else\\b"),
	VAR("var\\b"),
	FUN("fun\\b"),
	CLASS("class\\b"),
	ECHO("echo\\b"),
	// Identifier
	IDENTIFIER("\\w+");

	val pattern: Pattern

	init {
		this.pattern = Pattern.compile("^${pattern}")
	}
}