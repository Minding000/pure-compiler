package linter.elements.operations

import linter.Linter
import linter.elements.values.Value
import linter.scopes.Scope
import parsing.ast.operations.BinaryOperator

class BinaryOperator(override val source: BinaryOperator, val left: Value, val right: Value, val operator: String):
	Value(source) {

	init {
		units.add(left)
		units.add(right)
	}

	override fun linkReferences(linter: Linter, scope: Scope) {
		super.linkReferences(linter, scope)
		//TODO get return type depending on operator type
		// -> make operator an enum with return type property
		type = left.type?.scope?.resolveOperator(operator, right)?.returnType
	}
}