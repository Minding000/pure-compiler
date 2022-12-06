package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.Linter
import components.semantic_analysis.semantic_model.scopes.MutableScope
import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.ValueDeclaration
import components.syntax_parser.syntax_tree.definitions.Parameter as ParameterSyntaxTree

class Parameter(override val source: ParameterSyntaxTree, name: String, type: Type?, isMutable: Boolean,
				val hasDynamicSize: Boolean):
	ValueDeclaration(source, name, type, null, true, isMutable) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): Parameter {
		return Parameter(source, name, type?.withTypeSubstitutions(typeSubstitutions), isMutable, hasDynamicSize)
	}

	override fun linkPropertyParameters(linter: Linter, scope: MutableScope) {
		if(type == null)
			type = scope.resolveValue(name)?.type
	}
}
