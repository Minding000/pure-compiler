package logger.issues.modifiers

import components.syntax_parser.syntax_tree.definitions.Modifier
import logger.Issue
import logger.Severity

class DisallowedModifier(modifier: Modifier): Issue(Severity.WARNING, modifier) {
	override val text = "Modifier '${modifier.getValue()}' is not allowed here."
	override val description = "The modifier is not allowed here."
}
