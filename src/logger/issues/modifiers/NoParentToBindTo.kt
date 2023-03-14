package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class NoParentToBindTo(source: Element): Issue(Severity.WARNING, source) {
	override val text = "Can't bind type definition, because it doesn't have a parent."
	override val description = "To bind a type definition it needs to have a parent type definition to bind to."
	override val suggestion = "Remove 'bound' keyword."
	override val suggestedAction = RemoveModifier(WordAtom.BOUND)
}
