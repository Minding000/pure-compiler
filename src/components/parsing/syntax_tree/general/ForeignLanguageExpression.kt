package components.parsing.syntax_tree.general

import components.linting.Linter
import components.linting.semantic_model.general.ForeignLanguageExpression as SemanticForeignLanguageExpressionModel
import components.linting.semantic_model.scopes.MutableScope
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
