package components.parsing.syntax_tree.control_flow

import components.linting.Linter
import components.linting.semantic_model.control_flow.RaiseStatement as SemanticRaiseStatementModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import source_structure.Position

class RaiseStatement(private val value: ValueElement, start: Position): Element(start, value.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticRaiseStatementModel {
		return SemanticRaiseStatementModel(this, value.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Raise { $value }"
	}
}
