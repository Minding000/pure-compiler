package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class OverriddenSuperInitializerMissing(source: Element): Issue(Severity.WARNING, source) {
	override val text = "'overriding' keyword is used, but the initializer doesn't have an abstract super initializer."
	override val description = "This initializer tries to override a nonexistent or non-abstract super initializer."
	override val suggestion = "Remove 'overriding' modifier."
	override val suggestedAction = RemoveModifier(WordAtom.OVERRIDING)
}
