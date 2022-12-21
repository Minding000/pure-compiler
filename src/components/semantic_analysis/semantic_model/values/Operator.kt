package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.Linter
import components.syntax_parser.syntax_tree.general.Element
import messages.Message

class Operator(source: Element, val kind: Kind): Function(source, kind.stringRepresentation) {
	override val memberType = "operator"

	override fun validate(linter: Linter) {
		super.validate(linter)
		for(implementation in implementations) {
			if(kind == Kind.BRACKETS_SET) {
				if(!Linter.LiteralType.NOTHING.matches(implementation.signature.returnType))
					linter.addMessage(source, "Index operators can not accept and return a value at the same time.",
						Message.Type.WARNING)
			} else if(kind != Kind.BRACKETS_GET) {
				if(Linter.LiteralType.NOTHING.matches(implementation.signature.returnType)) {
					if(kind.returnsValue)
						linter.addMessage(source, "This operator is expected to return a value.", Message.Type.WARNING)
				} else {
					if(!kind.returnsValue)
						linter.addMessage(source, "This operator is not expected to return a value.", Message.Type.WARNING)
				}
				if(kind.isUnary) {
					if(implementation.parameters.size > 1 || !kind.isBinary && implementation.parameters.isNotEmpty())
						linter.addMessage(source, "Unary operators can't accept parameters.", Message.Type.WARNING)
				}
				if(kind.isBinary) {
					if(implementation.parameters.size > 1 || !kind.isUnary && implementation.parameters.isEmpty())
						linter.addMessage(source, "Binary operators need to accept exactly one parameter.", Message.Type.WARNING)
				}
			}
		}
	}


	enum class Kind(val stringRepresentation: String, val isUnary: Boolean, val isBinary: Boolean,
					val returnsValue: Boolean) {
		BRACKETS_GET("[]", true, false, true),
		BRACKETS_SET("[]=", false, true, false),
		EXCLAMATION_MARK("!", true, false, true),
		TRIPLE_DOT("...", true, false, true),
		DOUBLE_PLUS("++", true, false, false),
		DOUBLE_MINUS("--", true, false, false),
		DOUBLE_QUESTION_MARK("??", false, true, true),
		AND("&", false, true, true),
		PIPE("|", false, true, true),
		PLUS("+", false, true, true),
		MINUS("-", true, true, true),
		STAR("*", false, true, true),
		SLASH("/", false, true, true),
		PLUS_EQUALS("+=", false, true, false),
		MINUS_EQUALS("-=", false, true, false),
		STAR_EQUALS("*=", false, true, false),
		SLASH_EQUALS("/=", false, true, false),
		SMALLER_THAN("<", false, true, true),
		GREATER_THAN(">", false, true, true),
		SMALLER_THAN_OR_EQUAL_TO("<=", false, true, true),
		GREATER_THAN_OR_EQUAL_TO(">=", false, true, true),
		EQUAL_TO("==", false, true, true),
		NOT_EQUAL_TO("!=", false, true, true);

		override fun toString(): String = stringRepresentation
	}
}
