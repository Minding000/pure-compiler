package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class ExtraneousBody(source: SyntaxTreeNode, isAbstract: Boolean, memberType: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "${if(isAbstract) "Abstract" else "Native"} $memberType '$signature' defines a body."
	override val description = "The body is disallowed when abstract or native modifiers are used."
	override val suggestion = "Remove '${if(isAbstract) "abstract" else "native"}' modifier."
	override val suggestedAction = RemoveModifier(if(isAbstract) WordAtom.ABSTRACT else WordAtom.NATIVE)
}
