package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.OperatorDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.VariableValueDeclaration
import messages.Message

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun declareValue(linter: Linter, value: VariableValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.addMessage(initializer.source,
			"Initializer declarations aren't allowed in '${javaClass.simpleName}'.",
			Message.Type.ERROR)
	}

	open fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		linter.addMessage(newImplementation.source,
			"Function declarations aren't allowed in '${javaClass.simpleName}'.",
			Message.Type.ERROR)
	}

	open fun declareOperator(linter: Linter, operator: OperatorDefinition) {
		linter.addMessage(operator.source,
			"Operator declarations aren't allowed in '${javaClass.simpleName}'.",
			Message.Type.ERROR)
	}
}
