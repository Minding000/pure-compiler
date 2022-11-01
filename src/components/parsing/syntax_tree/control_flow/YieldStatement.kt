package components.parsing.syntax_tree.control_flow

import components.linting.Linter
import components.linting.semantic_model.control_flow.YieldStatement as SemanticYieldStatementModel
import components.linting.semantic_model.scopes.MutableScope
import components.parsing.syntax_tree.general.Element
import components.parsing.syntax_tree.general.ValueElement
import source_structure.Position

class YieldStatement(start: Position, private val key: Element?, private val value: ValueElement):
	ValueElement(start, value.end) {

	override fun concretize(linter: Linter, scope: MutableScope): SemanticYieldStatementModel {
		return SemanticYieldStatementModel(this, key?.concretize(linter, scope), value.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Yield { ${if(key == null) "" else "$key "}$value }"
	}
}
