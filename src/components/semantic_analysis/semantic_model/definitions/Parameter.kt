package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.context.VariableTracker
import components.semantic_analysis.semantic_model.context.VariableUsage
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import logger.issues.definition.PropertyParameterMismatch
import logger.issues.definition.PropertyParameterOutsideOfInitializer
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, scope: MutableScope, name: String, type: Type?, isMutable: Boolean,
				val hasDynamicSize: Boolean, isSpecificCopy: Boolean = false):
	ValueDeclaration(source, scope, name, type, null, true, isMutable, isSpecificCopy) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), isMutable, hasDynamicSize)
	}

	override fun declare() {
		if(type != null)
			scope.declareValue(this)
	}

	override fun determineType() {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				propertyDeclaration = parent.parentDefinition.scope.resolveValue(name)
				if(propertyDeclaration == null) {
					context.addIssue(PropertyParameterMismatch(source))
				} else {
					type = propertyDeclaration?.getComputedType()
				}
			} else {
				context.addIssue(PropertyParameterOutsideOfInitializer(source))
			}
		}
		super.determineType()
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
