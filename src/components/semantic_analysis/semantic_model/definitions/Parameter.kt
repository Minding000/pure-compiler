package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.VariableTracker
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import messages.Message
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, name: String, type: Type?, isMutable: Boolean, val hasDynamicSize: Boolean):
	ValueDeclaration(source, name, type, null, true, isMutable) {
	val isPropertySetter = type == null
	var propertyDeclaration: ValueDeclaration? = null

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, name, type?.withTypeSubstitutions(typeSubstitutions), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		if(isPropertySetter) {
			val parent = parent
			if(parent is InitializerDefinition) {
				propertyDeclaration = parent.parentDefinition.scope.resolveValue(name)
				if(propertyDeclaration == null) //TODO test
					linter.addMessage(source, "Property parameter doesn't match any property.", Message.Type.ERROR)
				type = propertyDeclaration?.type
			} else { //TODO test
				linter.addMessage(source, "Property parameters are only allowed in initializers.", Message.Type.ERROR)
			}
		}
	}

	override fun analyseDataFlow(linter: Linter, tracker: VariableTracker) {
		tracker.declare(this, true)
	}
}
