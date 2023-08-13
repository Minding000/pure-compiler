package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.context.SpecialType
import components.semantic_analysis.semantic_model.scopes.Scope
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.issues.declaration.*

class Operator(source: SyntaxTreeNode, scope: Scope, val kind: Kind): Function(source, scope, kind.stringRepresentation) {
	override val memberType = "operator"

	override fun validate() {
		super.validate()
		validateNonVariadic()
		if(kind == Kind.BRACKETS_SET)
			validateIndexSetter()
		else if(kind != Kind.BRACKETS_GET)
			validateNonIndexOperators()
	}

	private fun validateNonVariadic() {
		for(implementation in implementations) {
			for(parameter in implementation.parameters) {
				if(parameter.isVariadic)
					context.addIssue(VariadicParameterInOperator(parameter.source))
			}
		}
	}

	private fun validateIndexSetter() {
		for(implementation in implementations) {
			if(!SpecialType.NOTHING.matches(implementation.signature.returnType))
				context.addIssue(ReadWriteIndexOperator(source))
		}
	}

	private fun validateNonIndexOperators() {
		for(implementation in implementations) {
			if(SpecialType.NOTHING.matches(implementation.signature.returnType)) {
				if(kind.returnsValue)
					context.addIssue(OperatorExpectedToReturn(source))
			} else {
				if(!kind.returnsValue)
					context.addIssue(OperatorExpectedToNotReturn(source))
			}
			if(kind.isUnary) {
				if(implementation.parameters.size > 1 || !kind.isBinary && implementation.parameters.isNotEmpty())
					context.addIssue(ParameterInUnaryOperator(source))
			}
			if(kind.isBinary) {
				if(implementation.parameters.size > 1 || !kind.isUnary && implementation.parameters.isEmpty())
					context.addIssue(BinaryOperatorWithInvalidParameterCount(source))
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
