package linter.scopes

import linter.Linter
import linter.elements.definitions.FunctionDefinition
import linter.elements.definitions.InitializerDefinition
import linter.elements.definitions.OperatorDefinition
import linter.elements.values.TypeDefinition
import linter.elements.values.VariableValueDeclaration
import linter.messages.Message

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.messages.add(Message(
			"${initializer.source.getStartString()}: Initializer declarations aren't allowed in ${this.javaClass.simpleName}.",
			Message.Type.ERROR))
	}

	open fun declareFunction(linter: Linter, function: FunctionDefinition) {
		linter.messages.add(Message(
			"${function.source.getStartString()}: Function declarations aren't allowed in ${this.javaClass.simpleName}.",
			Message.Type.ERROR))
	}

	open fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		linter.messages.add(Message(
			"${operator.source.getStartString()}: Operator declarations aren't allowed in ${this.javaClass.simpleName}.",
			Message.Type.ERROR))
	}
}