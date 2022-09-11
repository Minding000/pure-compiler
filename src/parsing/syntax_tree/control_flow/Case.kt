package parsing.syntax_tree.control_flow

import linting.Linter
import linting.semantic_model.control_flow.Case
import linting.semantic_model.scopes.MutableScope
import parsing.syntax_tree.general.Element
import util.indent

class Case(private val condition: Element, private val result: Element): Element(condition.start, result.end) {

	override fun concretize(linter: Linter, scope: MutableScope): Case {
		return Case(this, condition.concretize(linter, scope), result.concretize(linter, scope))
	}

	override fun toString(): String {
		return "Case [ $condition ] {${"\n$result".indent()}\n}"
	}
}