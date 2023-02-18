package components.semantic_analysis.semantic_model.definitions

import components.semantic_analysis.semantic_model.types.Type
import components.semantic_analysis.semantic_model.values.InterfaceMember
import components.semantic_analysis.semantic_model.values.Value
import components.syntax_parser.syntax_tree.general.Element

class PropertyDeclaration(source: Element, name: String, type: Type? = null, value: Value? = null, isStatic: Boolean = false,
						  isAbstract: Boolean = false, isConstant: Boolean = true, isMutable: Boolean = false,
						  isOverriding: Boolean = false):
	InterfaceMember(source, name, type, value, isStatic, isAbstract, isConstant, isMutable, isOverriding) {

	override fun withTypeSubstitutions(typeSubstitutions: Map<TypeDefinition, Type>): PropertyDeclaration {
		return PropertyDeclaration(source, name, type?.withTypeSubstitutions(typeSubstitutions), value, isStatic, isAbstract,
			isConstant, isMutable, isOverriding)
	}
}
