package components.syntax_parser.syntax_tree.control_flow

import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.syntax_parser.syntax_tree.general.Element
import components.syntax_parser.syntax_tree.general.ValueElement
import source_structure.Position
import components.semantic_analysis.semantic_model.control_flow.ReturnStatement as SemanticReturnStatementModel

class ReturnStatement(start: Position, private val value: ValueElement?, end: Position): Element(start, end) {

	override fun toSemanticModel(scope: MutableScope): SemanticReturnStatementModel {
		return SemanticReturnStatementModel(this, scope, value?.toSemanticModel(scope))
	}

	override fun toString(): String {
		var stringRepresentation = "Return"
		if(value != null)
			stringRepresentation += " { $value }"
		return stringRepresentation
	}
}
