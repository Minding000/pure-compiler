package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.RaiseStatement
import linter.scopes.MutableScope
import parsing.ast.general.Element
import source_structure.Position

class RaiseStatement(private val value: Element, start: Position): Element(start, value.end) {

	override fun concretize(linter: Linter, scope: MutableScope): RaiseStatement {
		return RaiseStatement(this, value.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Raise { $value }"
	}
}