package logger.issues.definition

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class MultipleInstanceLists(source: SyntaxTreeNode): Issue(Severity.WARNING, source) {
	override val text = "Instance declarations can be merged."
	override val description = "One instance declaration can declare multiple instances."
	override val suggestion = "Merge instance declarations."
}
