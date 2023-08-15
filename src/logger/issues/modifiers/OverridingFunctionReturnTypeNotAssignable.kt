package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class OverridingFunctionReturnTypeNotAssignable(source: SyntaxTreeNode, memberType: String, overridingSignature: String,
												superSignature: String): Issue(Severity.ERROR, source) {
	override val text = "Return type of overriding $memberType '$overridingSignature' is not assignable to the return type " +
		"of the overridden $memberType '$superSignature'."
	override val description = "The return type of the overriding $memberType doesn't match the return type of the overridden $memberType."
	override val suggestion = "Change the return type to match the super ${memberType}s return type."
}
