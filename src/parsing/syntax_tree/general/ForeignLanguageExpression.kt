package parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.ForeignLanguageExpression
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.literals.ForeignLanguageLiteral
import parsing.syntax_tree.literals.Identifier

class ForeignLanguageExpression(private val foreignParser: Identifier, private val content: ForeignLanguageLiteral):
	ValueElement(foreignParser.start, content.end) {

	override fun concretize(linter: Linter, scope: MutableScope): ForeignLanguageExpression {
		return ForeignLanguageExpression(this, foreignParser.concretize(linter, scope), content.getValue())
	}

	override fun toString(): String {
		return "ForeignLanguageExpression [ $foreignParser ] { $content }"
	}
}