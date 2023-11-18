package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class ExtraneousMonomorphicModifier(source: SyntaxTreeNode, kind: String): Issue(Severity.WARNING, source) {
	override val text = "'monomorphic' keyword is used, but the $kind doesn't used its own type as Self."
	override val description = "Only members using their own type as the Self type need to be marked as 'monomorphic'."
	override val suggestion = "Remove 'monomorphic' modifier."
	override val suggestedAction = RemoveModifier(WordAtom.MONOMORPHIC)
}
