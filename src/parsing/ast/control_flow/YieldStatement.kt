package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.YieldStatement
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import source_structure.Position

class YieldStatement(start: Position, private val key: Element?, private val value: Element): Element(start, value.end) {

	override fun concretize(linter: Linter, scope: Scope): YieldStatement {
		return YieldStatement(this, key?.concretize(linter, scope), value.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Yield { ${if(key == null) "" else "$key "}$value }"
	}
}