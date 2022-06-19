package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.ReturnStatement
import linter.scopes.MutableScope
import parsing.ast.general.Element
import parsing.ast.general.ValueElement
import source_structure.Position

class ReturnStatement(start: Position, private val value: ValueElement?, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: MutableScope): ReturnStatement {
		return ReturnStatement(this, value?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Return { ${value ?: ""} }"
	}
}