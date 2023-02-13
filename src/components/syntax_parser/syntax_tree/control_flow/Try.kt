package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.Try as SemanticTryModel

class Try(private val expression: ValueElement, private val isOptional: Boolean, start: Position): ValueElement(start, expression.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticTryModel {
		return SemanticTryModel(this, expression.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "Try [ ${if(isOptional) "null" else "uncheck"} ] { $expression }"
	}
}
