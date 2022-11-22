package components.tokenizer

import java.util.regex.Pattern

enum class WordAtom(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false): WordDescriptor {
	// Whitespace
	LINE_BREAK("\\n"),
	WHITESPACE("\\s", true),
	// Comments
	SINGLE_LINE_COMMENT("\\/\\/.*", true),
	MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
	// Operators
	ARROW_CAPPED("=>\\|"),
	ARROW("=>"),
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
	GREATER_OR_EQUALS_THAN(">="),
	LOWER_OR_EQUALS_THAN("<="),
	GREATER_THAN(">"),
	LOWER_THAN("<"),
	PLUS("\\+"),
	MINUS("-"),
	STAR("\\*"),
	SLASH("\\/"),
	NOT("!"),
	NULL_COALESCENCE("\\?\\?"),
	OPTIONAL_ACCESSOR("\\?\\."),
	FOREIGN_EXPRESSION("::"),
	SPREAD_GROUP("\\.\\.\\."),
	// Symbols
	ASSIGNMENT("="),
	COMMA(","),
	SEMICOLON(";"),
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
	NUMBER_LITERAL("\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:e-?\\d+)?"),
	STRING_LITERAL("\"(?:[^\"\\\\]|\\\\.)*\"", false, true),
	// Keywords
	FORCE_CAST("as!"),
	OPTIONAL_CAST("as\\?"),
	AS("as\\b"),
	IS_NOT("is!"),
	IS("is\\b"),
	IF("if\\b"),
	ELSE("else\\b"),
	SWITCH("switch\\b"),
	LOOP("loop\\b"),
	OVER("over\\b"),
	WHILE("while\\b"),
	BREAK("break\\b"),
	NEXT("next\\b"),
	CONST("const\\b"),
	VAR("var\\b"),
	VAL("val\\b"),
	GETS("gets\\b"),
	SETS("sets\\b"),
	INIT("init\\b"),
	DEINIT("deinit\\b"),
	IT("it\\b"),
	TO("to\\b"),
	GENERATE("generate\\b"),
	ALIAS("alias\\b"),
	CLASS("class\\b"),
	OBJECT("object\\b"),
	ENUM("enum\\b"),
	ECHO("echo\\b"), //TODO remove
	RETURN("return\\b"),
	YIELD("yield\\b"),
	RAISE("raise\\b"),
	TRY_OPTIONAL("try\\?"),
	TRY_UNCHECK("try!"),
	HANDLE("handle\\b"),
	ALWAYS("always\\b"),
	IMMUTABLE("immutable\\b"),
	MUTABLE("mutable\\b"),
	MUTATING("mutating\\b"),
	NATIVE("native\\b"),
	OVERRIDING("overriding\\b"),
	INSTANCES("instances\\b"),
	CONTAINING("containing\\b"),
	CONSUMING("consuming\\b"), // Alternative: 'taking'
	PRODUCING("producing\\b"), // Alternative: 'giving'
	OPERATOR("operator\\b"),
	REFERENCING("referencing\\b"),
	SELF_REFERENCE("this\\b"),
	// Identifier
	IDENTIFIER("\\w+"),
	// Synthetic words (invalid regular expression to avoid match)
	FOREIGN_LANGUAGE("$$");

	val pattern: Pattern = Pattern.compile(pattern)

	override fun includes(atom: WordAtom?): Boolean {
		return this == atom
	}
}
