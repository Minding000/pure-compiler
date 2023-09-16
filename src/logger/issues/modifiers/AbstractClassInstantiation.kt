package logger.issues.modifiers

import components.semantic_model.declarations.Class
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import components.tokenizer.WordAtom
import logger.Issue
import logger.Severity
import logger.actions.RemoveModifier

class AbstractClassInstantiation(source: SyntaxTreeNode, `class`: Class): Issue(Severity.ERROR, source) {
	override val text = "Abstract class '${`class`.name}' cannot be instantiated."
	override val description = "Abstract classes cannot be instantiated."
	override val suggestion = "Remove the 'abstract' modifier."
	override val suggestedAction = RemoveModifier(WordAtom.ABSTRACT)
}
