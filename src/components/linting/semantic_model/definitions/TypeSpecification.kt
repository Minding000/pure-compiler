package components.linting.semantic_model.definitions

import components.linting.Linter
import components.linting.semantic_model.scopes.Scope
import components.linting.semantic_model.types.StaticType
import components.linting.semantic_model.types.Type
import components.linting.semantic_model.values.Value
import messages.Message
import components.parsing.syntax_tree.definitions.TypeSpecification as TypeSpecificationSyntaxTree

class TypeSpecification(override val source: TypeSpecificationSyntaxTree, val baseValue: Value,
						val typeParameters: List<Type>): Value(source) {

	init {
		units.add(baseValue)
		units.addAll(typeParameters)
	}

	override fun linkValues(linter: Linter, scope: Scope) {
		super.linkValues(linter, scope)
		val baseType = baseValue.type
		if(baseType !is StaticType) {
			linter.addMessage(source, "Type specifications can only be used on initializers.",
				Message.Type.WARNING) //TODO write test for this
			return
		}
		type = baseType.withTypeParameters(typeParameters)
	}
}
