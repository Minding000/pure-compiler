package logger.issues.loops

import components.semantic_model.values.LocalVariableDeclaration
import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class TooManyPluralTypeVariableDeclarations(source: SyntaxTreeNode, variableDeclarations: List<LocalVariableDeclaration>):
	Issue(Severity.ERROR, source) {
	override val text = "Plural types only provide index and element (2) values, but ${variableDeclarations.size} were declared."
	override val description = "When iterating over plural types only to variables can be declared:" +
		" One holding the index and one holding the element."
}
