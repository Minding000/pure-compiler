package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class MissingOverridingKeyword(source: Element, kind: String, signature: String): Issue(Severity.WARNING, source) {
	override val text = "$kind '$signature' is missing the 'overriding' keyword."
	override val description = "This member shadows a super member without overriding it."
	override val suggestion = "Add 'overriding' modifier."
	override val suggestedAction = AddModifier(WordAtom.OVERRIDING)
}
