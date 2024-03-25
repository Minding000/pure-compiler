package logger.issues.declaration

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier
import util.uppercaseFirstChar

class MissingBody(source: SyntaxTreeNode, memberType: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "${memberType.uppercaseFirstChar()} '$signature' is missing a body."
	override val description = "A body is required unless declared abstract or native."
	override val suggestion = "Add 'abstract' modifier."
	override val suggestedAction = AddModifier(WordAtom.ABSTRACT)
}
