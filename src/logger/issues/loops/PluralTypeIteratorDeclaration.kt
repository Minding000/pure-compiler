package logger.issues.loops

import components.syntax_parser.syntax_tree.general.SyntaxTreeNode
import logger.Issue
import logger.Severity

class PluralTypeIteratorDeclaration(source: SyntaxTreeNode): Issue(Severity.ERROR, source) {
	override val text = "Plural types don't provide an iterator."
	override val description = "The iterator variable cannot be declared, because the provided source has a plural type" +
		" which doesn't provide an iterator."
}
