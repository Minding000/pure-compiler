package linter.elements.operations

import linter.Linter
import linter.elements.values.Value
import linter.messages.Message
import linter.scopes.Scope
import parsing.ast.operations.BinaryOperator

class BinaryOperator(override val source: BinaryOperator, val left: Value, val right: Value, val operator: String):
	Value(source) {

	init {
		units.add(left)
		units.add(right)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		left.type?.let {
			val operator = it.scope.resolveOperator(operator, right.type)
			if(operator == null)
				linter.messages.add(
					Message(
						"${source.getStartString()}: Operator '${this.operator}(${right.type})' hasn't been declared yet.",
						Message.Type.ERROR
					)
				)
			else
				type = operator.returnType
		}
	}
}