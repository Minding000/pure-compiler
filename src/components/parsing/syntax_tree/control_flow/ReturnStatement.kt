package components.parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.ReturnStatement as SemanticReturnStatementModel
import linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import source_structure.Position

class ReturnStatement(start: Position, private val value: ValueElement?, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticReturnStatementModel {
		return SemanticReturnStatementModel(this, value?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Return { ${value ?: ""} }"
	}
}
