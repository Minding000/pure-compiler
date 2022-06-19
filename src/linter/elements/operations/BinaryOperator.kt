package linter.elements.operations

import linter.Linter
import linter.elements.values.Value
import linter.scopes.Scope
import parsing.ast.operations.BinaryOperator

class BinaryOperator(val source: BinaryOperator, val left: Value, val right: Value, val operator: String): Value() {

	init {
		units.add(left)
		units.add(right)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		type = left.type?.scope?.resolveOperator(operator, right.type.toString())?.returnType
	}
}