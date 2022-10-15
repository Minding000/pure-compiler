package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.values.Value
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.operations.BinaryOperator

class BinaryOperator(override val source: BinaryOperator, val left: Value, val right: Value, val operator: String):
	Value(source) {

	init {
		units.add(left)
		units.add(right)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		left.type?.let { leftType ->
			val operator = leftType.scope.resolveOperator(operator, right)
			if(operator == null) {
				linter.addMessage(source, "Operator '${this.operator}(${right.type})' hasn't been declared yet.",
						Message.Type.ERROR)
			} else {
				type = operator.returnType
			}
		}
		staticValue = calculateStaticResult()
	}

	private fun calculateStaticResult(): Value? = null //TODO implement
}
