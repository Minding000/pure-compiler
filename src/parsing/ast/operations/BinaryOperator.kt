package parsing.ast.operations

import linter.Linter
import linter.scopes.MutableScope
import linter.elements.operations.BinaryOperator
import parsing.ast.general.ValueElement
import util.indent

class BinaryOperator(val left: ValueElement, val right: ValueElement, val operator: String): ValueElement(left.start, right.end) {

	override fun concretize(linter: Linter, scope: MutableScope): BinaryOperator {
		return BinaryOperator(this, left.concretize(linter, scope), right.concretize(linter, scope), operator)
	}

	override fun toString(): String {
		return "BinaryOperator {${"\n$left $operator $right".indent()}\n}"
	}
}