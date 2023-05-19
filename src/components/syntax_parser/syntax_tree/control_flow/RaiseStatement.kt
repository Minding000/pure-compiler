package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.RaiseStatement as SemanticRaiseStatementModel

class RaiseStatement(private val value: ValueElement, start: Position): Element(start, value.end) {

	override fun concretize(scope: MutableScope): SemanticRaiseStatementModel {
		return SemanticRaiseStatementModel(this, scope, value.concretize(scope))
	}

	override fun toString(): String {
		return "Raise { $value }"
	}
}
