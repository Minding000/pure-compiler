package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.scopes.MutableScope
import linting.semantic_model.types.Type
import linting.semantic_model.values.VariableValueDeclaration
import components.parsing.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, name: String, type: Type?, isMutable: Boolean,
				val hasDynamicSize: Boolean):
	VariableValueDeclaration(source, name, type, null, true, isMutable) {

	override fun withTypeSubstitutions(typeSubstitution: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, name, type?.withTypeSubstitutions(typeSubstitution), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		if(type == null)
			type = scope.resolveValue(name)?.type
	}
}
