package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.FunctionImplementation
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.Operator
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.DisallowedDeclarationType

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, type: TypeDefinition)

	abstract fun declareValue(linter: Linter, value: ValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.addIssue(DisallowedDeclarationType(initializer.source, "Initializer", javaClass.simpleName))
	}

	open fun declareFunction(linter: Linter, name: String, newImplementation: FunctionImplementation) {
		linter.addIssue(DisallowedDeclarationType(newImplementation.source, "Function", javaClass.simpleName))
	}

	open fun declareOperator(linter: Linter, kind: Operator.Kind, newImplementation: FunctionImplementation) {
		linter.addIssue(DisallowedDeclarationType(newImplementation.source, "Operator", javaClass.simpleName))
	}
}
