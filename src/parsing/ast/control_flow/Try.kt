package parsing.ast.control_flow

import linter.Linter
import linter.scopes.MutableScope
import parsing.ast.general.Element
import source_structure.Position
import linter.elements.control_flow.Try
import parsing.ast.general.ValueElement

class Try(private val expression: Element, private val isOptional: Boolean, start: Position): ValueElement(start, expression.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Try {
		return Try(this, expression.concretize(linter, scope), isOptional)
	}

	override fun toString(): String {
		return "Try [ ${if(isOptional) "null" else "uncheck"} ] { $expression }"
	}
}