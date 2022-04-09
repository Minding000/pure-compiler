package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.ReturnStatement
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import source_structure.Position

class ReturnStatement(start: Position, private val value: Element?, end: Position): Element(start, end) {

	override fun concretize(linter: Linter, scope: Scope): ReturnStatement {
		return ReturnStatement(this, value?.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Return { ${value ?: ""} }"
	}
}