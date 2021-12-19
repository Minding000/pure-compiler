package parsing.tokenizer

import java.util.regex.Pattern

enum class WordAtom(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false): WordDescriptor {
	// Whitespace
	LINE_BREAK("\\n"),
	WHITESPACE("\\s", true),
	// Comments
	SINGLE_LINE_COMMENT("\\/\\/.*", true),
	MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
	// Operators
	INCREMENT("\\+\\+"),
	DECREMENT("--"),
	ADD("\\+="),
	SUBTRACT("-="),
	MULTIPLY("\\*="),
	DIVIDE("\\/="),
	AND("&"),
	OR("\\|"),
	EQUALS("=="),
	NOT_EQUALS("!="),
	GREATER_OR_EQUALS(">="),
	LOWER_OR_EQUALS("<="),
	GREATER(">"),
	LOWER("<"),
	PLUS("\\+"),
	MINUS("-"),
	STAR("\\*"),
	SLASH("\\/"),
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
	LOOP("loop\\b"),
	BREAK("break\\b"),
	NEXT("next\\b"),
	VAR("var\\b"),
	INIT("init\\b"),
	FUN("fun\\b"),
	CLASS("class\\b"),
	OBJECT("object\\b"),
	ECHO("echo\\b"),
	RETURN("return\\b"),
	NATIVE("native\\b"),
	GENERIC("generic\\b"),
	CONTAINING("containing\\b"),
	OPERATOR("operator\\b"),
	// Identifier
	IDENTIFIER("\\w+");

	val pattern = Pattern.compile("^(${pattern})")

	override fun includes(atom: WordAtom?): Boolean {
		return this == atom
	}
}