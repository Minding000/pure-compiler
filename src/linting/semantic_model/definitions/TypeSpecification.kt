package linting.semantic_model.definitions

import linting.Linter
import linting.semantic_model.scopes.Scope
import linting.semantic_model.types.StaticType
import linting.semantic_model.types.Type
import linting.semantic_model.values.Value
import parsing.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, val baseValue: Value,
						val typeParameters: List<Type>): Value(source) {

	init {
		units.add(baseValue)
		units.addAll(typeParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		type = (baseValue.type as? StaticType)?.withTypeParameters(typeParameters)
	}
}
