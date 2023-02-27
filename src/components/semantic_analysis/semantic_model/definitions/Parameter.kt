package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.VariableUsage
import components.semantic_analysis.semantic_model.scopes.Scope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import messages.Message
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, scope: Scope, name: String, type: Type?, isMutable: Boolean,
				val hasDynamicSize: Boolean): ValueDeclaration(source, scope, name, type, null, true, isMutable) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, scope, name, type?.withTypeSubstitutions(typeSubstitutions), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter) {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				propertyDeclaration = parent.parentDefinition.scope.resolveValue(name)
				if(propertyDeclaration == null) {
					linter.addMessage(source, "Property parameter doesn't match any property.", Message.Type.ERROR)
				} else {
					propertyDeclaration?.linkValues(linter)
					type = propertyDeclaration?.type
				}
			} else {
				linter.addMessage(source, "Property parameters are only allowed in initializers.", Message.Type.ERROR)
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		if(isPropertySetter) {
			propertyDeclaration?.let { propertyDeclaration ->
				tracker.add(VariableUsage.Type.WRITE, propertyDeclaration, propertyDeclaration)
			}
		} else {
			tracker.declare(this, true)
		}
	}
}
