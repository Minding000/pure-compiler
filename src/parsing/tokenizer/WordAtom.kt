package parsing.tokenizer

import java.util.regex.Pattern

enum class WordAtom(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false): WordDescriptor {
	FOREIGN_LANGUAGE("$"),
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
	NULL_COALESCENCE("\\?\\?"),
	DOUBLE_COLON("::"),
	// Symbols
	ASSIGNMENT("="),
	COMMA(","),
	COLON(":"),
	QUESTION_MARK("\\?"),
	DOT("\\."),
	PARENTHESES_OPEN("\\("),
	PARENTHESES_CLOSE("\\)"),
	BRACKETS_OPEN("\\["),
	BRACKETS_CLOSE("]"),
	BRACES_OPEN("\\{"),
	BRACES_CLOSE("\\}"),
	// Literals
	NULL_LITERAL("null\\b"),
	BOOLEAN_LITERAL("(yes|no)\\b"),
	NUMBER_LITERAL("\\d+"),
	STRING_LITERAL("\"(?:[^\"\\\\]|\\\\.)*\"", false, true),
	// Keywords
	FORCE_CAST("as!"),
	OPTIONAL_CAST("as\\?"),
	AS("as\\b"),
	IS_NOT("is!"),
	IS("is\\b"),
	IF("if\\b"),
	ELSE("else\\b"),
	LOOP("loop\\b"),
	BREAK("break\\b"),
	NEXT("next\\b"),
	CONST("const\\b"),
	VAR("var\\b"),
	VAL("val\\b"),
	GET("get\\b"),
	SET("set\\b"),
	INIT("init\\b"),
	IT("it\\b"),
	TO("to\\b"),
	CLASS("class\\b"),
	OBJECT("object\\b"),
	TRAIT("trait\\b"),
	ENUM("enum\\b"),
	ECHO("echo\\b"),
	RETURN("return\\b"),
	IMM("imm\\b"),
	MUT("mut\\b"),
	NATIVE("native\\b"),
	INSTANCES("instances\\b"),
	CONTAINING("containing\\b"),
	CONSUMING("consuming\\b"),
	PRODUCING("producing\\b"),
	OPERATOR("operator\\b"),
	REFERENCING("referencing\\b"),
	// Identifier
	IDENTIFIER("\\w+");

	val pattern = Pattern.compile("^(${pattern})")

	override fun includes(atom: WordAtom?): Boolean {
		return this == atom
	}
}