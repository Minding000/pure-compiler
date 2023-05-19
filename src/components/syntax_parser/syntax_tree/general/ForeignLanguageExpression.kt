package components.syntax_parser.syntax_tree.general

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.literals.ForeignLanguageLiteral
import components.syntax_parser.syntax_tree.literals.Identifier
import components.semantic_analysis.semantic_model.general.ForeignLanguageExpression as SemanticForeignLanguageExpressionModel

class ForeignLanguageExpression(private val foreignParser: Identifier, private val content: ForeignLanguageLiteral):
	ValueElement(foreignParser.start, content.end) {

	override fun toSemanticModel(scope: MutableScope): SemanticForeignLanguageExpressionModel {
		return SemanticForeignLanguageExpressionModel(this, scope, foreignParser.toSemanticModel(scope), content.getValue())
	}

	override fun toString(): String {
		return "ForeignLanguageExpression [ $foreignParser ] { $content }"
	}
}
