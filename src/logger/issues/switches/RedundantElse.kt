package logger.issues.switches

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity
import logger.actions.RemoveElement

class RedundantElse(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "The else branch is redundant, because the switch is already exhaustive without it."
	override val description = "The else branch handles all remaining values, but the cases already cover all possible values."
	override val suggestion = "Remove else branch."
	override val suggestedAction = RemoveElement(source)
}
