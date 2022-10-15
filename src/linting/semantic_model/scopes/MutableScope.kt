package linting.semantic_model.scopes

import linting.Linter
import linting.semantic_model.definitions.FunctionImplementation
import linting.semantic_model.definitions.InitializerDefinition
import linting.semantic_model.definitions.OperatorDefinition
import linting.semantic_model.definitions.TypeDefinition
import linting.semantic_model.values.VariableValueDeclaration
import messages.Message

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.addMessage(initializer.source,
			"Initializer declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR)
	}

	open fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		linter.addMessage(newImplementation.source,
			"Function declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR)
	}

	open fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		linter.addMessage(operator.source,
			"Operator declarations aren't allowed in ${javaClass.simpleName}.",
			Message.Type.ERROR)
	}
}
