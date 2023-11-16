package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class MissingSpecificKeyword(source: SyntaxTreeNode, kind: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "$kind '$signature' is missing the 'specific' keyword."
	override val description = "Members using their own type as the Self type need to be marked as 'specific'."
	override val suggestion = "Add 'specific' modifier."
	override val suggestedAction = AddModifier(WordAtom.SPECIFIC)
}
