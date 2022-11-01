package components.parsing.syntax_tree.general

import linting.Linter
import linting.semantic_model.general.ForeignLanguageExpression as SemanticForeignLanguageExpressionModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.literals.ForeignLanguageLiteral
import components.parsing.syntax_tree.literals.Identifier

class ForeignLanguageExpression(private val foreignParser: Identifier, private val content: ForeignLanguageLiteral):
	ValueElement(foreignParser.start, content.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticForeignLanguageExpressionModel {
		return SemanticForeignLanguageExpressionModel(this, foreignParser.concretize(linter, scope),
			content.getValue())
	}

	override fun toString(): String {
		return "ForeignLanguageExpression [ $foreignParser ] { $content }"
	}
}
