package components.semantic_analysis.semantic_model.values

import components.semantic_analysis.semantic_model.definitions.TypeDefinition
import components.semantic_analysis.semantic_model.types.Type
import components.syntax_parser.syntax_tree.general.Element

class PropertyDeclaration(source: Element, name: String, type: Type? = null, value: Value? = null,
						  isAbstract: Boolean = false, isConstant: Boolean = true, isMutable: Boolean = false):
	MemberDeclaration(source, name, type, value, isAbstract, isConstant, isMutable) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitutions), value, isAbstract,
			isConstant, isMutable)
	}
}
