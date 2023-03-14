package logger.issues.modifiers

import components.syntax_parser.syntax_tree.general.Element
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.AddModifier

class OverridingInitializerMissingConvertingKeyword(source: Element): Issue(Severity.ERROR, source) {
	override val text = "Overriding initializer of converting initializer needs to be converting."
	override val description = "Overriding initializers of converting initializers need to be converting as well."
	override val suggestion = "Add 'converting' modifier."
	override val suggestedAction = AddModifier(WordAtom.CONVERTING)
}
