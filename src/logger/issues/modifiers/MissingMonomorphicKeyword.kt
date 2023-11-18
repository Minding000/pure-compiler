package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class MissingMonomorphicKeyword(source: SyntaxTreeNode, kind: String, signature: String): Issue(Severity.ERROR, source) {
	override val text = "$kind '$signature' is missing the 'monomorphic' keyword."
	override val description = "Members taking self type parameters need to be marked as 'monomorphic'."
	override val suggestion = "Add 'monomorphic' modifier."
	override val suggestedAction = AddModifier(WordAtom.MONOMORPHIC)
}
