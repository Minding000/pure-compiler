package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message

abstract class Scope {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun resolveType(name: String): TypeDefinition?

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	abstract fun resolveReference(name: String): VariableValueDeclaration?

	open fun declareFunction(linter: Linter, function: FunctionDefinition) {
		linter.messages.add(Message(
			"${function.source.getStartString()}: Function definitions aren't allowed here.",
			Message.Type.ERROR))
	}

	open fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		linter.messages.add(Message(
			"${operator.source.getStartString()}: Operator definitions aren't allowed here.",
			Message.Type.ERROR))
	}
}