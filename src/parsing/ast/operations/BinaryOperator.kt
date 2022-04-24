package parsing.ast.operations

import linter.Linter
import linter.scopes.Scope
import linter.elements.operations.BinaryOperator
import parsing.ast.general.Element
import util.indent

class BinaryOperator(val left: Element, val right: Element, val operator: String): Element(left.start, right.end) {

	override fun concretize(linter: Linter, scope: Scope): BinaryOperator {
		return BinaryOperator(this, left.concretize(linter, scope), right.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}