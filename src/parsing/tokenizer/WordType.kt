package parsing.tokenizer

import java.util.regex.Pattern

enum class WordType(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false) {
	// Whitespace
	LINE_BREAK("\\n"),
	WHITESPACE("\\s", true),
	// Comments
	SINGLE_LINE_COMMENT("\\/\\/.*", true),
	MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
	// Operators
	UNARY_MODIFICATION("\\+\\+|--"),
	BINARY_MODIFICATION("\\+=|-=|\\*=|\\/=|\\^="),
	BINARY_BOOLEAN_OPERATOR("&|\\|"),
	EQUALITY("==|!="),
	COMPARISON(">=|<=|>|<"),
	ADDITION("[+-]"),
	MULTIPLICATION("[*/]"),
	EXPONENTIATION("\\^"),
	NOT("!"),
	// Symbols
	ASSIGNMENT("="),
	COMMA(","),
	COLON(":"),
	DOT("\\."),
	PARENTHESES_OPEN("\\("),
	PARENTHESES_CLOSE("\\)"),
	BRACES_OPEN("\\{"),
	BRACES_CLOSE("\\}"),
	// Literals
	NULL_LITERAL("null\\b"),
	BOOLEAN_LITERAL("(yes|no)\\b"),
	NUMBER_LITERAL("\\d+"),
	STRING_LITERAL("\".*?\""),
	// Keywords
	IF("if\\b"),
	ELSE("else\\b"),
	VAR("var\\b"),
	FUN("fun\\b"),
	CLASS("class\\b"),
	ECHO("echo\\b"),
	RETURN("return\\b"),
	// Identifier
	IDENTIFIER("\\w+");

	val pattern = Pattern.compile("^(${pattern})")
}