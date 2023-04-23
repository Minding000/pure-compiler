package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.DisallowedDeclarationType

abstract class MutableScope: Scope() {

	abstract fun declareType(linter: Linter, typeDefinition: TypeDefinition)

	abstract fun declareValue(linter: Linter, valueDeclaration: ValueDeclaration)

	open fun declareInitializer(linter: Linter, initializer: InitializerDefinition) {
		linter.addIssue(DisallowedDeclarationType(initializer.source, "Initializer", javaClass.simpleName))
	}
}
