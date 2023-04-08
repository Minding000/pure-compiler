package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.PropertyParameterMismatch
import logger.issues.definition.PropertyParameterOutsideOfInitializer
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, scope: Scope, name: String, type: Type?, isMutable: Boolean,
				val hasDynamicSize: Boolean): ValueDeclaration(source, scope, name, type, null, true, isMutable) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null

	override fun withTypeSubstitutions(linter: Linter, typeSubstitutions: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, scope, name, type?.withTypeSubstitutions(linter, typeSubstitutions), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter) {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				propertyDeclaration = parent.parentDefinition.scope.resolveValue(name)
				if(propertyDeclaration == null) {
					linter.addIssue(PropertyParameterMismatch(source))
				} else {
					propertyDeclaration?.preLinkValues(linter)
					type = propertyDeclaration?.type
				}
			} else {
				linter.addIssue(PropertyParameterOutsideOfInitializer(source))
			}
		}
	}

	override fun analyseDataFlow(tracker: VariableTracker) {
		if(isPropertySetter) {
			propertyDeclaration?.let { propertyDeclaration ->
				tracker.add(VariableUsage.Kind.WRITE, propertyDeclaration, propertyDeclaration)
			}
		} else {
			tracker.declare(this, true)
		}
	}
}
