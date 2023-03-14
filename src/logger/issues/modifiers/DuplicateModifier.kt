package logger.issues.modifiers

import components.syntax_parser.syntax_tree.definitions.Modifier
import logger.Issue
import logger.Severity

class DuplicateModifier(modifier: Modifier): Issue(Severity.WARNING, modifier) {
	override val text = "Duplicate '${modifier.getValue()}' modifier."
	override val description = "The modifier is not allowed here."
}
