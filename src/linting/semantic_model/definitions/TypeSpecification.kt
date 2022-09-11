package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.literals.ObjectType
import linting.semantic_model.literals.Type
import linting.semantic_model.values.Value
import linting.messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, val baseValue: Value,
						private val genericParameters: List<Type>): Value(source) {

	init {
		units.add(baseValue)
		units.addAll(genericParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		baseValue.type?.let { baseType ->
			val placeholders = baseType.scope.getGenericTypes()
			if(genericParameters.size != placeholders.size) {
				linter.messages.add(Message(
					"${source.getStartString()}: Number of provided type parameters " +
							"(${genericParameters.size}) doesn't match number of declared " +
							"generic types (${placeholders.size}).", Message.Type.ERROR))
				return
			}
			val substitution = HashMap<ObjectType, Type>()
			var areParametersCompatible = true
			for(parameterIndex in placeholders.indices) {
				val placeholder = placeholders[parameterIndex]
				val parameter = genericParameters[parameterIndex]
				if(!placeholder.acceptsSubstituteType(parameter)) {
					areParametersCompatible = false
					linter.messages.add(Message("${source.getStartString()}: The type parameter " +
							"'$parameter' is not assignable to '$placeholder'.", Message.Type.ERROR))
				}
				substitution[placeholder] = parameter
			}
			if(areParametersCompatible)
				type = baseType.withTypeSubstitutions(substitution)
		}
	}
}
