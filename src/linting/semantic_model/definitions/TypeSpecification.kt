package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.types.ObjectType
import linting.semantic_model.types.Type
import linting.semantic_model.values.Value
import messages.Message
import linting.semantic_model.scopes.Scope
import parsing.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, val baseValue: Value,
						val genericParameters: List<Type>): Value(source) {

	init {
		units.add(baseValue)
		units.addAll(genericParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		baseValue.type?.let { baseType ->
			val placeholders = baseType.scope.getGenericTypes()
			if(genericParameters.size != placeholders.size) {
				linter.addMessage(source, "Number of provided type parameters " +
							"(${genericParameters.size}) doesn't match number of declared " +
							"generic types (${placeholders.size}).", Message.Type.ERROR)
				return
			}
			val typeSubstitutions = HashMap<ObjectType, Type>()
			var areParametersCompatible = true
			for(parameterIndex in placeholders.indices) {
				val placeholder = placeholders[parameterIndex]
				val parameter = genericParameters[parameterIndex]
				if(!placeholder.acceptsSubstituteType(parameter)) {
					areParametersCompatible = false
					linter.addMessage(source, "The type parameter " +
							"'$parameter' is not assignable to '$placeholder'.", Message.Type.ERROR)
				}
				typeSubstitutions[placeholder] = parameter
			}
			if(areParametersCompatible)
				type = baseType.withTypeSubstitutions(typeSubstitutions)
		}
	}
}
