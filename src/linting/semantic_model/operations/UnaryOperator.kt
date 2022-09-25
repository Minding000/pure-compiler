package linting.semantic_model.operations

import linting.Linter
import linting.semantic_model.scopes.Scope
import linting.semantic_model.values.Value
import messages.Message
import parsing.syntax_tree.operations.UnaryOperator

class UnaryOperator(override val source: UnaryOperator, val value: Value, val operator: String): Value(source) {

	init {
		units.add(value)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		value.type?.let { valueType ->
			val operator = valueType.scope.resolveOperator(operator)
			if(operator == null) {
				linter.addMessage(source, "Operator '${this.operator}()' hasn't been declared yet.",
					Message.Type.ERROR)
			} else {
				type = operator.returnType
			}
		}
	}
}