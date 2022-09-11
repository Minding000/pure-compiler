package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.ReturnStatement
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import parsing.syntax_tree.general.ValueElement
import source_structure.Position

class ReturnStatement(start: Position, private val value: ValueElement?, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): ReturnStatement {
		return ReturnStatement(this, value?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Return { ${value ?: ""} }"
	}
}