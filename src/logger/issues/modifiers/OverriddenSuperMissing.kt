package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class OverriddenSuperMissing(source: Element, kind: String): Issue(Severity.WARNING, source) {
	override val text = "'overriding' keyword is used, but the $kind doesn't have a super $kind."
	override val description = "This member tries to override a nonexistent super member."
	override val suggestion = "Remove 'overriding' modifier."
	override val suggestedAction = RemoveModifier(WordAtom.OVERRIDING)
}
