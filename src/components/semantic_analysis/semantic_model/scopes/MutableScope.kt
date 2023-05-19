package components.semantic_analysis.semantic_model.scopes

import components.semantic_analysis.semantic_model.definitions.InitializerDefinition
import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.DisallowedDeclarationType

abstract class MutableScope: Scope() {

	abstract fun declareType(typeDefinition: TypeDefinition)

	abstract fun declareValue(valueDeclaration: ValueDeclaration)

	open fun declareInitializer(initializer: InitializerDefinition) {
		initializer.context.addIssue(DisallowedDeclarationType(initializer.source, "Initializer", javaClass.simpleName))
	}
}
