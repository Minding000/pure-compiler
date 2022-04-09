package parsing.ast.control_flow

import linter.Linter
import linter.elements.control_flow.Case
import linter.elements.general.Unit
import linter.scopes.Scope
import parsing.ast.general.Element
import util.indent

class Case(private val condition: Element, private val result: Element): Element(condition.start, result.end) {

	override fun concretize(linter: Linter, scope: Scope): Case {
		return Case(this, condition.concretize(linter, scope), result.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}