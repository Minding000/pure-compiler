package components.tokenizer

import java.util.regex.Pattern

enum class WordAtom(pattern: String, val ignore: Boolean = false, val isMultiline: Boolean = false): WordDescriptor {
	// Whitespace
	LINE_BREAK("\\n"),
	WHITESPACE("[^\\S\\n]+", true),
	// Comments
	SINGLE_LINE_COMMENT("\\/\\/.*", true),
	MULTI_LINE_COMMENT("\\/\\*[\\s\\S]*?\\*\\/", true, true),
	// Operators
	CAPPED_ARROW("=>\\|"),
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
	SPREAD("\\.\\.\\."),
	NULL_COALESCENCE("\\?\\?"),
	OPTIONAL_ACCESSOR("\\?\\."),
	FOREIGN_EXPRESSION("::"),
	// Symbols
	ASSIGNMENT("="),
	DOT("\\."),
	COMMA(","),
	COLON(":"),
	SEMICOLON(";"),
	QUESTION_MARK("\\?"),
	OPENING_PARENTHESIS("\\("),
	CLOSING_PARENTHESIS("\\)"),
	OPENING_BRACKET("\\["),
	CLOSING_BRACKET("]"),
	OPENING_BRACE("\\{"),
	CLOSING_BRACE("\\}"),
	// Literals
	NULL_LITERAL("null\\b"),
	BOOLEAN_LITERAL("(yes|no)\\b"),
	NUMBER_LITERAL("(?:[1-9][\\d_]*|0)(?:\\.\\d[\\d_]*)?(?:e-?\\d+)?"),
	STRING_LITERAL("\"(?:[^\"\\\\]|\\\\.)*\"", false, true),
	// Keywords
	INSTANCES("instances\\b"),
	CONST("const\\b"),
	VAL("val\\b"),
	VAR("var\\b"),
	GETS("gets\\b"),
	SETS("sets\\b"),
	INITIALIZER("init\\b"),
	DEINITIALIZER("deinit\\b"),
	IT("it\\b"),
	TO("to\\b"),
	OPERATOR("operator\\b"),
	GENERATOR("generate\\b"),
	CONTAINING("containing\\b"),
	TYPE_ALIAS("alias\\b"),
	CLASS("class\\b"),
	OBJECT("object\\b"),
	ENUM("enum\\b"),
	IF("if\\b"),
	ELSE("else\\b"),
	SWITCH("switch\\b"),
	LOOP("loop\\b"),
	OVER("over\\b"),
	WHILE("while\\b"),
	BREAK("break\\b"),
	NEXT("next\\b"),
	RETURN("return\\b"),
	YIELD("yield\\b"),
	RAISE("raise\\b"),
	UNCHECKED_CAST("as!"),
	OPTIONAL_CAST("as\\?"),
	AS("as\\b"),
	IS_NOT("is!"),
	IS("is\\b"),
	OPTIONAL_TRY("try\\?"),
	UNCHECKED_TRY("try!"),
	HANDLE("handle\\b"),
	ALWAYS("always\\b"),
	ABSTRACT("abstract\\b"),
	IMMUTABLE("immutable\\b"),
	MUTABLE("mutable\\b"),
	MUTATING("mutating\\b"),
	NATIVE("native\\b"),
	OVERRIDING("overriding\\b"),
	CONSUMING("consuming\\b"), // Alternative: 'taking'
	PRODUCING("producing\\b"), // Alternative: 'giving'
	REFERENCING("referencing\\b"),
	SELF_REFERENCE("this\\b"),
	// Identifier
	IDENTIFIER("[\\p{L}][\\p{L}\\p{N}_]*"),
	// Synthetic words (invalid regular expression to avoid match)
	FOREIGN_LANGUAGE("$$");

	val pattern: Pattern = Pattern.compile(pattern)

	override fun includes(atom: WordAtom?): Boolean {
		return this == atom
	}
}
