package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.values.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import linting.messages.Message

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.messages.add(Message(
			"${initializer.source.getStartString()}: Initializer declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR))
	}

	open fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		linter.messages.add(Message(
			"${newImplementation.source.getStartString()}: Function declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR))
	}

	open fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		linter.messages.add(Message(
			"${operator.source.getStartString()}: Operator declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR))
	}
}